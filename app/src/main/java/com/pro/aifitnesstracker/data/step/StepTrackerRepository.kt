package com.pro.aifitnesstracker.data.step

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.pro.aifitnesstracker.AppDatabase
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.StepRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class SensorStatusInfo(
    val sensorType: String,
    val sensorName: String,
    val vendor: String,
    val powerMa: Float,
    val isHardwareSensor: Boolean,
    val description: String
)

class StepTrackerRepository private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.stepTrackerDao()
    private val prefs = SharedPrefsManager(context)
    private val sessionMutex = Mutex()

    // Active walking session state
    private var activeSessionStartTime: Long = 0L
    private var activeSessionLastStepTime: Long = 0L
    private var activeSessionSteps: Int = 0

    companion object {
        @Volatile
        private var INSTANCE: StepTrackerRepository? = null

        fun getInstance(context: Context): StepTrackerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepTrackerRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ── Metric & Calculations ──

    fun getStrideLengthMeters(): Float {
        if (prefs.isCustomStepLengthEnabled()) {
            val customCm = prefs.getCustomStepLengthCm()
            if (customCm > 20f && customCm < 200f) {
                return customCm / 100f
            }
        }
        val heightCm = prefs.getHeight()
        return if (heightCm > 50f) {
            (heightCm * 0.414f) / 100f
        } else {
            0.74f // Standard baseline stride (74 cm)
        }
    }

    fun calculateDistanceMeters(steps: Int): Float {
        return steps * getStrideLengthMeters()
    }

    fun calculateCalories(steps: Int, distanceMeters: Float? = null): Int {
        val distM = distanceMeters ?: calculateDistanceMeters(steps)
        val distanceKm = distM / 1000f
        val weight = prefs.getWeight().let { if (it > 20f) it else 70f }
        // ACSM walking expenditure: approx 0.75 kcal per kg per km
        return (weight * distanceKm * 0.75f).roundToInt().coerceAtLeast(0)
    }

    fun formatDistance(distanceMeters: Float, unit: String): String {
        return if (unit.equals("mi", ignoreCase = true) || unit.equals("miles", ignoreCase = true)) {
            val miles = distanceMeters / 1609.344f
            String.format(Locale.US, "%.2f mi", miles)
        } else {
            val km = distanceMeters / 1000f
            String.format(Locale.US, "%.2f km", km)
        }
    }

    fun formatDuration(seconds: Int): String {
        val totalMins = (seconds / 60).coerceAtLeast(0)
        return if (totalMins >= 60) {
            val hrs = totalMins / 60
            val mins = totalMins % 60
            "${hrs}h ${mins}m"
        } else {
            "$totalMins min"
        }
    }

    // ── Sensor Status ──

    fun getSensorStatus(): SensorStatusInfo {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        return when {
            stepCounter != null -> SensorStatusInfo(
                sensorType = "Hardware Step Counter",
                sensorName = stepCounter.name,
                vendor = stepCounter.vendor,
                powerMa = stepCounter.power,
                isHardwareSensor = true,
                description = "Ultra-low power hardware pedometer counting continuous steps."
            )
            stepDetector != null -> SensorStatusInfo(
                sensorType = "Hardware Step Detector",
                sensorName = stepDetector.name,
                vendor = stepDetector.vendor,
                powerMa = stepDetector.power,
                isHardwareSensor = true,
                description = "Hardware detector reporting instantaneous footfalls."
            )
            else -> SensorStatusInfo(
                sensorType = "Accelerometer Cadence Fallback",
                sensorName = "Software Pedometer",
                vendor = "Device Accelerometer",
                powerMa = 0.5f,
                isHardwareSensor = false,
                description = "Using motion sensor peak analysis because dedicated step sensor is unavailable."
            )
        }
    }

    // ── Step Ingestion & Walking Session Tracking ──

    suspend fun recordStepEvent(stepsToday: Int, deltaSteps: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        sessionMutex.withLock {
            if (activeSessionStartTime == 0L || (now - activeSessionLastStepTime) > 60_000L) {
                // If previous session had enough movement (at least 15 steps in 20s), finalize it
                if (activeSessionSteps >= 15 && (activeSessionLastStepTime - activeSessionStartTime) >= 20_000L) {
                    val durationSec = ((activeSessionLastStepTime - activeSessionStartTime) / 1000L).toInt()
                    val dist = calculateDistanceMeters(activeSessionSteps)
                    val cal = calculateCalories(activeSessionSteps, dist)
                    dao.insertWalkingSession(
                        WalkingSessionEntity(
                            date = todayStr,
                            startTimeMs = activeSessionStartTime,
                            endTimeMs = activeSessionLastStepTime,
                            stepsCount = activeSessionSteps,
                            distanceMeters = dist,
                            calories = cal,
                            durationSeconds = durationSec
                        )
                    )
                    prefs.addActiveSecondsToday(durationSec)
                }
                // Start a fresh session
                activeSessionStartTime = now
                activeSessionLastStepTime = now
                activeSessionSteps = deltaSteps
            } else {
                activeSessionSteps += deltaSteps
                activeSessionLastStepTime = now
            }
        }

        // Daily aggregate calculations
        val currentDist = calculateDistanceMeters(stepsToday)
        val currentCal = calculateCalories(stepsToday, currentDist)
        val goal = prefs.getStepGoal()
        val activeSec = prefs.getActiveSecondsToday()
        val sessionsToday = dao.getSessionsForDateSync(todayStr).size

        val dailyRecord = DailyStepEntity(
            date = todayStr,
            steps = stepsToday,
            goal = goal,
            distanceMeters = currentDist,
            calories = currentCal,
            activeSeconds = activeSec,
            sessionsCount = sessionsToday,
            lastUpdatedTimestamp = now
        )
        dao.insertOrUpdateDailyRecord(dailyRecord)

        // Sync with existing legacy HealthDao & SharedPrefsManager
        try {
            db.healthDao().insertStepRecord(StepRecord(todayStr, stepsToday))
            prefs.saveDailyStats(todayStr, stepsToday, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check and unlock milestones & streaks
        checkAndUnlockAchievements(stepsToday)
    }

    suspend fun flushActiveSession() {
        sessionMutex.withLock {
            val now = System.currentTimeMillis()
            if (activeSessionSteps >= 15 && (activeSessionLastStepTime - activeSessionStartTime) >= 20_000L) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val durationSec = ((activeSessionLastStepTime - activeSessionStartTime) / 1000L).toInt()
                val dist = calculateDistanceMeters(activeSessionSteps)
                val cal = calculateCalories(activeSessionSteps, dist)
                dao.insertWalkingSession(
                    WalkingSessionEntity(
                        date = todayStr,
                        startTimeMs = activeSessionStartTime,
                        endTimeMs = activeSessionLastStepTime,
                        stepsCount = activeSessionSteps,
                        distanceMeters = dist,
                        calories = cal,
                        durationSeconds = durationSec
                    )
                )
                prefs.addActiveSecondsToday(durationSec)
                activeSessionStartTime = 0L
                activeSessionLastStepTime = 0L
                activeSessionSteps = 0
            }
        }
    }

    suspend fun addManualWalkingSession(steps: Int, durationMinutes: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        val durationSec = (durationMinutes * 60).coerceAtLeast(60)
        val startTime = now - (durationSec * 1000L)
        val dist = calculateDistanceMeters(steps)
        val cal = calculateCalories(steps, dist)

        dao.insertWalkingSession(
            WalkingSessionEntity(
                date = todayStr,
                startTimeMs = startTime,
                endTimeMs = now,
                stepsCount = steps,
                distanceMeters = dist,
                calories = cal,
                durationSeconds = durationSec
            )
        )
        prefs.addActiveSecondsToday(durationSec)

        val updatedStepsToday = prefs.getStepsToday() + steps
        prefs.saveStepsToday(updatedStepsToday)
        prefs.addStepsToCurrentHour(steps)
        recordStepEvent(updatedStepsToday, steps)
    }

    // ── Database Queries ──

    fun getDailyRecord(date: String): Flow<DailyStepEntity?> = dao.getDailyRecord(date)

    suspend fun getDailyRecordSync(date: String): DailyStepEntity? = dao.getDailyRecordSync(date)

    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<DailyStepEntity>> =
        dao.getRecordsInRange(startDate, endDate)

    suspend fun getRecordsInRangeSync(startDate: String, endDate: String): List<DailyStepEntity> =
        dao.getRecordsInRangeSync(startDate, endDate)

    fun getAllRecords(): Flow<List<DailyStepEntity>> = dao.getAllRecords()

    fun getSessionsForDate(date: String): Flow<List<WalkingSessionEntity>> =
        dao.getSessionsForDate(date)

    fun getAllRecentSessions(): Flow<List<WalkingSessionEntity>> = dao.getAllRecentSessions()

    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    // ── Achievements & Streaks ──

    fun getAllAchievements(): Flow<List<StepAchievementEntity>> = dao.getAllAchievements()

    suspend fun ensureDefaultAchievements() {
        val defaults = listOf(
            StepAchievementEntity("STEP_1K", "First Steps", "Walk 1,000 steps in a single day", "👟", 1000),
            StepAchievementEntity("STEP_3K", "Beginner Walker", "Walk 3,000 steps in a single day", "🚶", 3000),
            StepAchievementEntity("STEP_5K", "Active Walker", "Reach 5,000 steps in a single day", "💪", 5000),
            StepAchievementEntity("STEP_8K", "Fitness Enthusiast", "Reach 8,000 steps in a single day", "🔥", 8000),
            StepAchievementEntity("STEP_10K", "10K Champion", "Achieve the 10,000 steps daily benchmark", "🏆", 10000),
            StepAchievementEntity("STREAK_7", "Walking Streak", "Hit your step goal 7 consecutive days", "⚡", 7),
            StepAchievementEntity("STREAK_30", "Monthly Champion", "Hit your step goal 30 consecutive days", "👑", 30)
        )
        dao.insertAllAchievements(defaults)
    }

    private suspend fun checkAndUnlockAchievements(stepsToday: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        val stepBadges = listOf(
            "STEP_1K" to 1000,
            "STEP_3K" to 3000,
            "STEP_5K" to 5000,
            "STEP_8K" to 8000,
            "STEP_10K" to 10000
        )

        for ((id, target) in stepBadges) {
            if (stepsToday >= target) {
                val existing = dao.getAchievement(id)
                if (existing != null && !existing.isUnlocked) {
                    dao.insertOrUpdateAchievement(
                        existing.copy(
                            isUnlocked = true,
                            unlockedDate = todayStr,
                            unlockedTimestamp = now
                        )
                    )
                    prefs.unlockBadge(id)
                }
            }
        }

        // Streak check
        val streak = calculateConsecutiveStreaks()
        prefs.saveStepStreak(streak)
        if (streak > prefs.getStepBestStreak()) {
            prefs.saveStepBestStreak(streak)
        }

        if (streak >= 7) {
            val badge = dao.getAchievement("STREAK_7")
            if (badge != null && !badge.isUnlocked) {
                dao.insertOrUpdateAchievement(
                    badge.copy(isUnlocked = true, unlockedDate = todayStr, unlockedTimestamp = now)
                )
                prefs.unlockBadge("STREAK_7")
            }
        }
        if (streak >= 30) {
            val badge = dao.getAchievement("STREAK_30")
            if (badge != null && !badge.isUnlocked) {
                dao.insertOrUpdateAchievement(
                    badge.copy(isUnlocked = true, unlockedDate = todayStr, unlockedTimestamp = now)
                )
                prefs.unlockBadge("STREAK_30")
            }
        }
    }

    suspend fun calculateConsecutiveStreaks(): Int {
        val recentRecords = dao.getRecentRecordsSync().associateBy { it.date }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        var streak = 0
        val todayStr = sdf.format(cal.time)
        val todayRec = recentRecords[todayStr]
        val currentGoal = prefs.getStepGoal()

        if (todayRec != null && todayRec.steps >= todayRec.goal) {
            streak++
        }

        // Check backwards day by day
        while (true) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d = sdf.format(cal.time)
            val rec = recentRecords[d]
            val dayGoal = rec?.goal ?: currentGoal
            if (rec != null && rec.steps >= dayGoal && dayGoal > 0) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
