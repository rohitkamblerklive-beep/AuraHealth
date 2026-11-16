package com.pro.aifitnesstracker.data.yoga

import android.content.Context
import com.pro.aifitnesstracker.HealthDao
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.YogaRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class YogaRepository(
    private val yogaDao: YogaDao,
    private val healthDao: HealthDao,
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("yoga_prefs", Context.MODE_PRIVATE)
    private val mainPrefs = SharedPrefsManager(context)

    companion object {
        const val KEY_DAILY_GOAL_MINS = "yoga_daily_goal_minutes"
        const val KEY_REMINDER_ENABLED = "yoga_reminder_enabled"
        const val KEY_REMINDER_HOUR = "yoga_reminder_hour"
        const val KEY_REMINDER_MINUTE = "yoga_reminder_minute"
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }

    // ── Daily Goal & Preferences ──────────────────────────────────────────────
    fun getDailyGoalMinutes(): Int {
        return prefs.getInt(KEY_DAILY_GOAL_MINS, 20)
    }

    fun setDailyGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL_MINS, minutes.coerceIn(5, 120)).apply()
    }

    fun isReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ENABLED, false)

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    fun getReminderTime(): Pair<Int, Int> {
        val h = prefs.getInt(KEY_REMINDER_HOUR, 7) // default 7:00 AM
        val m = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        return Pair(h, m)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).putInt(KEY_REMINDER_MINUTE, minute).apply()
    }

    // ── Daily Dashboard Flow Data ─────────────────────────────────────────────
    fun getTodaySessions(): Flow<List<YogaSessionRecord>> {
        return yogaDao.getSessionsForDate(getTodayDateString())
    }

    fun getTodayCompletedMinutes(): Flow<Int> {
        return getTodaySessions().map { sessions ->
            sessions.sumOf { it.completedDurationSeconds } / 60
        }
    }

    fun getTodayCaloriesBurned(): Flow<Int> {
        return getTodaySessions().map { sessions ->
            sessions.sumOf { it.caloriesBurned }
        }
    }

    fun getTodayRemainingMinutes(): Flow<Int> {
        val goal = getDailyGoalMinutes()
        return getTodayCompletedMinutes().map { completed ->
            (goal - completed).coerceAtLeast(0)
        }
    }

    // ── All Sessions & History ────────────────────────────────────────────────
    fun getAllSessions(): Flow<List<YogaSessionRecord>> = yogaDao.getAllSessions()

    fun getRecentSessions(limit: Int = 10): Flow<List<YogaSessionRecord>> = yogaDao.getRecentSessions(limit)

    // ── Save Session & Sync ───────────────────────────────────────────────────
    suspend fun recordYogaSession(
        sessionTitle: String,
        category: String,
        plannedSeconds: Int,
        completedSeconds: Int,
        posesCompleted: Int,
        totalPoses: Int,
        isCompleted: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val today = getTodayDateString()

        // ACSM formula calculation: ~2.5 to 4.5 kcal / min depending on category
        val burnRatePerMin = when (category) {
            YogaCategoryNames.WEIGHT_LOSS -> 4.5f
            YogaCategoryNames.ADVANCED -> 4.8f
            YogaCategoryNames.FULL_BODY -> 4.0f
            YogaCategoryNames.MORNING -> 3.2f
            YogaCategoryNames.FLEXIBILITY -> 3.0f
            YogaCategoryNames.BACK_FOCUSED -> 2.8f
            YogaCategoryNames.STRESS_RELIEF -> 2.0f
            YogaCategoryNames.EVENING -> 1.8f
            YogaCategoryNames.MEDITATION -> 1.5f
            else -> 2.5f
        }
        val calculatedCals = ((completedSeconds / 60f) * burnRatePerMin).toInt().coerceAtLeast(if (completedSeconds > 30) 3 else 0)

        val record = YogaSessionRecord(
            date = today,
            sessionTitle = sessionTitle,
            category = category,
            plannedDurationSeconds = plannedSeconds,
            completedDurationSeconds = completedSeconds,
            caloriesBurned = calculatedCals,
            posesCompleted = posesCompleted,
            totalPoses = totalPoses,
            isFullyCompleted = isCompleted
        )

        val insertedId = yogaDao.insertSession(record)

        // Backward sync to HealthDao so Main Dashboard and Workout History sync automatically!
        try {
            healthDao.insertYogaRecord(
                YogaRecord(
                    date = today,
                    poseName = sessionTitle,
                    durationSeconds = completedSeconds,
                    accuracy = 95
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check & auto-unlock achievements
        checkAndUnlockAchievements()

        insertedId
    }

    // ── Streak Calculation ────────────────────────────────────────────────────
    suspend fun calculateStreaks(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val dates = yogaDao.getDistinctPracticedDatesSync()
        if (dates.isEmpty()) return@withContext Pair(0, 0)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance()
        val todayStr = sdf.format(todayCal.time)
        todayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(todayCal.time)

        val dateSet = dates.toSortedSet()
        var currentStreak = 0
        var maxStreak = 0

        // Determine if streak is active (today or yesterday has a session)
        if (dateSet.contains(todayStr) || dateSet.contains(yesterdayStr)) {
            val checkCal = Calendar.getInstance()
            if (!dateSet.contains(todayStr)) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (true) {
                val dStr = sdf.format(checkCal.time)
                if (dateSet.contains(dStr)) {
                    currentStreak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Calculate longest historical streak
        var tempStreak = 0
        var prevCal: Calendar? = null
        for (dStr in dateSet) {
            try {
                val d = sdf.parse(dStr) ?: continue
                val cal = Calendar.getInstance().apply { time = d }
                if (prevCal == null) {
                    tempStreak = 1
                } else {
                    val diffDays = ((cal.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    if (diffDays == 1) {
                        tempStreak++
                    } else if (diffDays > 1) {
                        tempStreak = 1
                    }
                }
                prevCal = cal
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } catch (_: Exception) {}
        }
        if (currentStreak > maxStreak) maxStreak = currentStreak

        Pair(currentStreak, maxStreak)
    }

    fun getCurrentStreakFlow(): Flow<Int> {
        return yogaDao.getDistinctPracticedDates().map { dates ->
            calculateStreaks().first
        }.flowOn(Dispatchers.IO)
    }

    // ── Weekly Mon-Sun Analytics ──────────────────────────────────────────────
    data class WeeklyAnalyticsData(
        val dayMinutes: Map<String, Int>, // "Mon" -> mins, "Tue" -> mins...
        val totalMinutes: Int,
        val averageDailyMinutes: Int,
        val bestDayName: String,
        val bestDayMinutes: Int,
        val daysPracticedCount: Int,
        val completionRatePercent: Int
    )

    suspend fun getWeeklyAnalytics(): WeeklyAnalyticsData = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val mondayStr = sdf.format(cal.time)

        val weekDaysMap = mutableMapOf<String, Int>()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        dayLabels.forEach { weekDaysMap[it] = 0 }

        val startCal = cal.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, 6)
        val sundayStr = sdf.format(startCal.time)

        val sessions = yogaDao.getSessionsBetweenDatesSync(mondayStr, sundayStr)
        var totalSecs = 0
        var completedSessionsCount = 0

        for (s in sessions) {
            totalSecs += s.completedDurationSeconds
            if (s.isFullyCompleted) completedSessionsCount++
            try {
                val d = sdf.parse(s.date)
                if (d != null) {
                    val dayLabel = dayFormat.format(d)
                    val existing = weekDaysMap[dayLabel] ?: 0
                    weekDaysMap[dayLabel] = existing + (s.completedDurationSeconds / 60)
                }
            } catch (_: Exception) {}
        }

        val totalMins = totalSecs / 60
        val daysPracticed = weekDaysMap.values.count { it > 0 }
        val avgDailyMins = if (daysPracticed > 0) totalMins / 7 else 0

        var bestDay = "Mon"
        var bestDayMins = 0
        weekDaysMap.forEach { (d, mins) ->
            if (mins >= bestDayMins) {
                bestDayMins = mins
                bestDay = d
            }
        }

        val completionRate = if (sessions.isNotEmpty()) {
            ((completedSessionsCount.toFloat() / sessions.size) * 100).toInt()
        } else {
            0
        }

        WeeklyAnalyticsData(
            dayMinutes = weekDaysMap,
            totalMinutes = totalMins,
            averageDailyMinutes = avgDailyMins,
            bestDayName = if (bestDayMins > 0) bestDay else "None",
            bestDayMinutes = bestDayMins,
            daysPracticedCount = daysPracticed,
            completionRatePercent = completionRate
        )
    }

    // ── Monthly Analytics ─────────────────────────────────────────────────────
    data class MonthlyAnalyticsData(
        val totalMinutes: Int,
        val totalSessions: Int,
        val consistencyPercent: Int,
        val currentStreak: Int,
        val longestStreak: Int
    )

    suspend fun getMonthlyAnalytics(): MonthlyAnalyticsData = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayStr = sdf.format(cal.time)

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDayStr = sdf.format(cal.time)

        val sessions = yogaDao.getSessionsBetweenDatesSync(firstDayStr, lastDayStr)
        val totalMins = sessions.sumOf { it.completedDurationSeconds } / 60
        val distinctDays = sessions.map { it.date }.distinct().size
        val consistency = if (currentDayOfMonth > 0) {
            ((distinctDays.toFloat() / currentDayOfMonth) * 100).toInt().coerceIn(0, 100)
        } else 0

        val (curStreak, maxStreak) = calculateStreaks()

        MonthlyAnalyticsData(
            totalMinutes = totalMins,
            totalSessions = sessions.size,
            consistencyPercent = consistency,
            currentStreak = curStreak,
            longestStreak = maxStreak
        )
    }

    // ── Achievements ──────────────────────────────────────────────────────────
    private suspend fun initAchievementsIfEmpty() {
        val initialList = listOf(
            YogaAchievementEntity("first_yoga", "First Yoga", "Complete your first mindful yoga practice", "🌱"),
            YogaAchievementEntity("yoga_beginner", "Yoga Beginner", "Complete 3 dedicated yoga sessions", "🧘"),
            YogaAchievementEntity("yoga_explorer", "Yoga Explorer", "Complete 10 guided yoga sessions", "✨"),
            YogaAchievementEntity("yoga_master", "Yoga Master", "Complete 30 guided yoga sessions", "👑"),
            YogaAchievementEntity("streak_7", "7-Day Streak", "Practice yoga for 7 consecutive days", "🔥"),
            YogaAchievementEntity("streak_30", "Consistency Champion", "Achieve a 30-day consecutive yoga streak", "🏆")
        )
        yogaDao.insertAchievements(initialList)
    }

    suspend fun checkAndUnlockAchievements() = withContext(Dispatchers.IO) {
        initAchievementsIfEmpty()

        val totalCount = yogaDao.getTotalSessionsCountSync()
        val (streak, _) = calculateStreaks()
        val now = System.currentTimeMillis()

        if (totalCount >= 1) yogaDao.unlockAchievement("first_yoga", now)
        if (totalCount >= 3) yogaDao.unlockAchievement("yoga_beginner", now)
        if (totalCount >= 10) yogaDao.unlockAchievement("yoga_explorer", now)
        if (totalCount >= 30) yogaDao.unlockAchievement("yoga_master", now)
        if (streak >= 7) yogaDao.unlockAchievement("streak_7", now)
        if (streak >= 30) yogaDao.unlockAchievement("streak_30", now)
    }

    fun getAllAchievements(): Flow<List<YogaAchievementEntity>> {
        return yogaDao.getAllAchievements()
    }

    // ── Favorites ─────────────────────────────────────────────────────────────
    fun getAllFavoritePoseIds(): Flow<List<String>> {
        return yogaDao.getAllFavorites().map { list -> list.map { it.poseId } }
    }

    fun isFavorite(poseId: String): Flow<Boolean> = yogaDao.isFavorite(poseId)

    suspend fun toggleFavorite(poseId: String, isCurrentlyFav: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyFav) {
            yogaDao.deleteFavorite(poseId)
        } else {
            yogaDao.insertFavorite(YogaFavoriteEntity(poseId))
        }
    }

    // ── Custom Routines ───────────────────────────────────────────────────────
    fun getAllCustomRoutines(): Flow<List<CustomYogaRoutineEntity>> = yogaDao.getAllCustomRoutines()

    suspend fun saveCustomRoutine(
        id: String,
        name: String,
        category: String,
        poseIds: List<String>,
        durations: List<Int>,
        restSecs: Int
    ) = withContext(Dispatchers.IO) {
        val entity = CustomYogaRoutineEntity(
            id = id,
            name = name,
            category = category,
            poseIds = poseIds.joinToString(","),
            poseDurations = durations.joinToString(","),
            restIntervalSeconds = restSecs
        )
        yogaDao.insertCustomRoutine(entity)
    }

    suspend fun deleteCustomRoutine(id: String) = withContext(Dispatchers.IO) {
        yogaDao.deleteCustomRoutine(id)
    }

    // ── Personalized Plans ────────────────────────────────────────────────────
    fun getAllPlanProgress(): Flow<List<YogaPlanProgressEntity>> = yogaDao.getAllPlanProgress()

    fun getPlanProgress(planId: String): Flow<YogaPlanProgressEntity?> = yogaDao.getPlanProgress(planId)

    suspend fun startPlan(planId: String) = withContext(Dispatchers.IO) {
        val existing = yogaDao.getPlanProgressSync(planId)
        val updated = existing?.copy(isStarted = true, isPaused = false)
            ?: YogaPlanProgressEntity(planId = planId, currentDay = 1, isStarted = true, isPaused = false)
        yogaDao.insertOrUpdatePlanProgress(updated)
    }

    suspend fun togglePausePlan(planId: String) = withContext(Dispatchers.IO) {
        val existing = yogaDao.getPlanProgressSync(planId) ?: return@withContext
        val updated = existing.copy(isPaused = !existing.isPaused)
        yogaDao.insertOrUpdatePlanProgress(updated)
    }

    suspend fun completePlanDay(planId: String, dayNumber: Int) = withContext(Dispatchers.IO) {
        val existing = yogaDao.getPlanProgressSync(planId) ?: YogaPlanProgressEntity(planId = planId, currentDay = 1, isStarted = true)
        val completedList = existing.completedDays.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()
        completedList.add(dayNumber)
        val nextDay = (dayNumber + 1).coerceAtMost(30)
        val updated = existing.copy(
            currentDay = nextDay,
            completedDays = completedList.sorted().joinToString(","),
            lastCompletedDate = getTodayDateString()
        )
        yogaDao.insertOrUpdatePlanProgress(updated)
    }

    suspend fun resetPlan(planId: String) = withContext(Dispatchers.IO) {
        yogaDao.resetPlanProgress(planId)
    }
}
