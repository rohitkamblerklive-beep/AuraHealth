package com.pro.aifitnesstracker.data.water

import android.content.Context
import com.pro.aifitnesstracker.AppDatabase
import com.pro.aifitnesstracker.FirebaseSyncManager
import com.pro.aifitnesstracker.HealthDao
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.WaterRecord
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class WaterTrackerRepository(
    private val waterDao: WaterTrackerDao,
    private val healthDao: HealthDao,
    private val prefsManager: SharedPrefsManager,
    private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun todayDateString(): String = dateFormat.format(Date())

    // ── Water Intake Entries CRUD ──

    fun getEntriesForDate(date: String): Flow<List<WaterIntakeEntry>> =
        waterDao.getEntriesForDate(date)

    suspend fun getEntriesForDateSync(date: String): List<WaterIntakeEntry> =
        withContext(Dispatchers.IO) { waterDao.getEntriesForDateSync(date) }

    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<WaterIntakeEntry>> =
        waterDao.getEntriesBetweenDates(startDate, endDate)

    suspend fun getEntriesBetweenDatesSync(startDate: String, endDate: String): List<WaterIntakeEntry> =
        withContext(Dispatchers.IO) { waterDao.getEntriesBetweenDatesSync(startDate, endDate) }

    suspend fun addWaterEntry(
        amountMl: Int,
        drinkType: String = DrinkType.WATER.code,
        containerName: String = "Regular Glass",
        note: String = "",
        customDate: String? = null,
        customTime: String? = null
    ): WaterIntakeEntry = withContext(Dispatchers.IO) {
        require(amountMl > 0) { "Water amount must be greater than 0 ml" }

        val date = customDate ?: todayDateString()
        val time = customTime ?: timeFormat.format(Date())
        val typeEnum = DrinkType.fromCode(drinkType)

        val entry = WaterIntakeEntry(
            amountMl = amountMl,
            drinkType = typeEnum.code,
            date = date,
            time = time,
            timestamp = System.currentTimeMillis(),
            containerName = containerName,
            note = note.trim(),
            hydrationFactor = typeEnum.hydrationFactor
        )

        val id = waterDao.insertEntry(entry)
        val created = entry.copy(id = id)

        // Recalculate daily statistics and sync with legacy systems
        recalculateAndSync(date)
        created
    }

    suspend fun updateWaterEntry(entry: WaterIntakeEntry) = withContext(Dispatchers.IO) {
        require(entry.amountMl > 0) { "Water amount must be greater than 0 ml" }
        waterDao.updateEntry(entry)
        recalculateAndSync(entry.date)
    }

    suspend fun deleteWaterEntry(entry: WaterIntakeEntry) = withContext(Dispatchers.IO) {
        waterDao.deleteEntry(entry)
        recalculateAndSync(entry.date)
    }

    suspend fun deleteWaterEntryById(id: Long, date: String) = withContext(Dispatchers.IO) {
        waterDao.deleteEntryById(id)
        recalculateAndSync(date)
    }

    /**
     * Recalculates total consumption for the given date, synchronizes with SharedPreferences,
     * Room HealthDao, Firebase, and updates streak & achievements.
     */
    private suspend fun recalculateAndSync(date: String) {
        val entries = waterDao.getEntriesForDateSync(date)
        val totalMl = entries.sumOf { it.amountMl }
        val glasses = (totalMl / 250)

        val isToday = (date == todayDateString())
        if (isToday) {
            prefsManager.saveWaterMlToday(totalMl)
            prefsManager.saveWaterGlassesToday(glasses)
        }

        // Keep Room WaterRecord synchronized
        try {
            healthDao.insertWaterRecord(WaterRecord(date = date, glasses = glasses))
        } catch (_: Exception) {}

        // Keep Firebase synchronized
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                FirebaseSyncManager.syncSingleWater(currentUser.uid, date, glasses)
            } catch (_: Exception) {}
        }

        // Recalculate streaks & achievements
        updateStreakAndAchievements(date, totalMl)
    }

    // ── Streak & Gamification ──

    private suspend fun updateStreakAndAchievements(date: String, totalMl: Int) {
        val goalMl = getGoalMlForDate(date)

        // Achievement 1: First Drop (Any entry ever)
        if (totalMl > 0) {
            prefsManager.unlockWaterAchievement("FIRST_DROP")
        }

        // Achievement 2: Hydration Starter (>= 50% of daily goal)
        if (totalMl >= (goalMl * 0.5f).toInt()) {
            prefsManager.unlockWaterAchievement("STARTER_50")
        }

        // Achievement 3: Hydration Hero (>= 100% of daily goal)
        if (totalMl >= goalMl) {
            prefsManager.unlockWaterAchievement("HERO_100")
            prefsManager.unlockBadge("WATER_HERO")
        }

        // Calculate continuous streak
        val currentStreak = calculateConsecutiveStreak()
        prefsManager.saveWaterStreak(currentStreak)
        val bestStreak = maxOf(currentStreak, prefsManager.getWaterBestStreak())
        prefsManager.saveWaterBestStreak(bestStreak)

        // Achievement 4: 3-Day Streak
        if (currentStreak >= 3) {
            prefsManager.unlockWaterAchievement("STREAK_3")
        }
        // Achievement 5: 7-Day Streak
        if (currentStreak >= 7) {
            prefsManager.unlockWaterAchievement("STREAK_7")
        }
        // Achievement 6: 30-Day Master Streak
        if (currentStreak >= 30) {
            prefsManager.unlockWaterAchievement("STREAK_30")
        }
    }

    suspend fun calculateConsecutiveStreak(): Int = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        var streak = 0
        // Check if today is already completed
        val todayTotal = waterDao.getTotalMlForDate(todayStr) ?: 0
        val todayGoal = getGoalMlForDate(todayStr)
        val todayMet = todayTotal >= todayGoal

        if (todayMet) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)
            val yesterdayTotal = waterDao.getTotalMlForDate(yesterdayStr) ?: 0
            val yesterdayGoal = getGoalMlForDate(yesterdayStr)
            if (yesterdayTotal < yesterdayGoal) {
                return@withContext 0
            }
        }

        // Count backward
        for (i in 0 until 180) { // Limit to 180 days check
            val dateStr = dateFormat.format(cal.time)
            val total = waterDao.getTotalMlForDate(dateStr) ?: 0
            val goal = getGoalMlForDate(dateStr)
            if (total >= goal && total > 0) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        streak
    }

    // ── Daily Hydration Goal System ──

    fun getGoalMlForDate(date: String): Int {
        val currentGoal = prefsManager.getWaterGoalMl()
        return currentGoal.coerceAtLeast(500)
    }

    fun saveDailyGoalMl(ml: Int) {
        val safeMl = ml.coerceIn(500, 6000)
        prefsManager.saveWaterGoalMl(safeMl)
    }

    /**
     * Estimates suggested hydration goal using weight, activity, and workout minutes.
     * Documented calculation method:
     * - Base: 35 ml per kg of body weight
     * - Activity Level: Sedentary (+0%), Light (+5%), Moderate (+10%), High (+15%), Athlete (+20%)
     * - Exercise: +12 ml per minute of exercise
     * Clamped comfortably between 1500 ml and 4500 ml, rounded to nearest 50 ml.
     */
    fun calculateSuggestedGoal(
        weightKg: Float,
        activityLevel: String,
        workoutMins: Int,
        gender: String = "Male"
    ): Int {
        val safeWeight = weightKg.coerceIn(30f, 200f)
        val baseMl = safeWeight * 35f

        val activityMultiplier = when (activityLevel.lowercase(Locale.ROOT)) {
            "sedentary" -> 1.0f
            "light" -> 1.05f
            "moderate" -> 1.10f
            "high", "very active" -> 1.15f
            "athlete" -> 1.20f
            else -> 1.10f
        }

        val workoutBonusMl = (workoutMins.coerceIn(0, 180) * 12f)
        val rawTotal = (baseMl * activityMultiplier) + workoutBonusMl

        val rounded = ((rawTotal / 50f).roundToInt() * 50).coerceIn(1500, 4500)
        return rounded
    }

    // ── Custom Containers CRUD ──

    fun getAllContainers(): Flow<List<CustomWaterContainer>> =
        waterDao.getAllContainers()

    suspend fun getAllContainersSync(): List<CustomWaterContainer> =
        withContext(Dispatchers.IO) {
            ensureDefaultContainers()
            waterDao.getAllContainersSync()
        }

    suspend fun ensureDefaultContainers() = withContext(Dispatchers.IO) {
        val count = waterDao.getContainerCount()
        if (count == 0) {
            val defaults = listOf(
                CustomWaterContainer(name = "Small Glass", amountMl = 150, icon = "🥛", isDefault = true),
                CustomWaterContainer(name = "Regular Glass", amountMl = 250, icon = "🥛", isDefault = true),
                CustomWaterContainer(name = "Large Glass", amountMl = 350, icon = "🥛", isDefault = true),
                CustomWaterContainer(name = "Water Bottle", amountMl = 500, icon = "🧴", isDefault = true),
                CustomWaterContainer(name = "Large Bottle", amountMl = 750, icon = "🧴", isDefault = true)
            )
            defaults.forEach { waterDao.insertContainer(it) }
        }
    }

    suspend fun addCustomContainer(name: String, amountMl: Int, icon: String): CustomWaterContainer =
        withContext(Dispatchers.IO) {
            require(name.isNotBlank()) { "Container name cannot be blank" }
            require(amountMl in 50..3000) { "Amount must be between 50 ml and 3000 ml" }
            val container = CustomWaterContainer(
                name = name.trim(),
                amountMl = amountMl,
                icon = icon.ifBlank { "🥛" }
            )
            val id = waterDao.insertContainer(container)
            container.copy(id = id)
        }

    suspend fun updateCustomContainer(container: CustomWaterContainer) = withContext(Dispatchers.IO) {
        require(container.name.isNotBlank()) { "Container name cannot be blank" }
        require(container.amountMl in 50..3000) { "Amount must be between 50 ml and 3000 ml" }
        waterDao.updateContainer(container)
    }

    suspend fun deleteCustomContainer(container: CustomWaterContainer) = withContext(Dispatchers.IO) {
        waterDao.deleteContainer(container)
    }

    suspend fun deleteCustomContainerById(id: Long) = withContext(Dispatchers.IO) {
        waterDao.deleteContainerById(id)
    }

    // ── Measurement Unit Helpers ──

    fun mlToOz(ml: Int): Float = ml * 0.033814f
    fun ozToMl(oz: Float): Int = (oz / 0.033814f).roundToInt()

    fun formatVolume(ml: Int, unit: String = prefsManager.getWaterUnit()): String {
        return if (unit.equals("oz", ignoreCase = true)) {
            val oz = mlToOz(ml)
            String.format(Locale.US, "%.1f fl oz", oz)
        } else {
            "$ml ml"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WaterTrackerRepository? = null

        fun getInstance(context: Context): WaterTrackerRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val prefs = SharedPrefsManager(context)
                val instance = WaterTrackerRepository(
                    waterDao = db.waterTrackerDao(),
                    healthDao = db.healthDao(),
                    prefsManager = prefs,
                    context = context.applicationContext
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
