package com.pro.aifitnesstracker.data.sleep

import android.content.Context
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.pro.aifitnesstracker.HealthConnectManager
import com.pro.aifitnesstracker.HealthDao
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.SleepRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class SleepRepository(
    private val sleepDao: SleepTrackerDao,
    private val healthDao: HealthDao,
    private val prefsManager: SharedPrefsManager,
    private val healthConnectManager: HealthConnectManager? = null,
    private val context: Context? = null
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Active tracking state persisted via SharedPreferences
    private val prefs = context?.getSharedPreferences("SLEEP_ACTIVE_TRACKER_PREFS", Context.MODE_PRIVATE)

    private val _activeSleepState = MutableStateFlow(loadPersistedActiveState())
    val activeSleepState: StateFlow<ActiveSleepState> = _activeSleepState.asStateFlow()

    // Reactive streams
    val allSessions: Flow<List<SleepSessionEntity>> = sleepDao.getAllSessions()
    val latestSession: Flow<SleepSessionEntity?> = sleepDao.getLatestSession()
    val sleepSchedule: Flow<SleepScheduleEntity> = sleepDao.getSchedule().map { it ?: SleepScheduleEntity() }
    val achievements: Flow<List<SleepAchievementEntity>> = sleepDao.getAllAchievements()

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        // Initialize default achievements if empty
        ensureDefaultAchievements()
    }

    private fun loadPersistedActiveState(): ActiveSleepState {
        val isTracking = prefs?.getBoolean("IS_TRACKING", false) ?: false
        val startTime = prefs?.getLong("START_TIME_MS", 0L) ?: 0L
        val notes = prefs?.getString("INITIAL_NOTES", "") ?: ""
        return if (isTracking && startTime > 0L) {
            ActiveSleepState(isTracking = true, startTimeMs = startTime, initialNotes = notes)
        } else {
            ActiveSleepState()
        }
    }

    private fun persistActiveState(state: ActiveSleepState) {
        prefs?.edit()?.apply {
            putBoolean("IS_TRACKING", state.isTracking)
            putLong("START_TIME_MS", state.startTimeMs)
            putString("INITIAL_NOTES", state.initialNotes)
            apply()
        }
    }

    private fun ensureDefaultAchievements() {
        val defaultList = listOf(
            SleepAchievementEntity("first_night", "First Night", "Completed your first tracked sleep session", "🌙"),
            SleepAchievementEntity("beginner_3", "Sleep Beginner", "Recorded 3 complete nights of sleep", "⭐"),
            SleepAchievementEntity("champion_7", "Sleep Champion", "Achieved your sleep goal for 7 nights", "🏆"),
            SleepAchievementEntity("consistent_7", "Consistent Sleeper", "Maintained schedule consistency for 7 days", "🎯"),
            SleepAchievementEntity("master_30", "Sleep Master", "Logged 30 nights meeting your restorative sleep target", "👑")
        )
        repositoryScope.launch {
            sleepDao.insertAchievements(defaultList)
        }
    }

    // --- MANUAL & LIVE SLEEP TRACKING ---

    fun startSleep(startTimeMs: Long = System.currentTimeMillis(), notes: String = "") {
        val state = ActiveSleepState(isTracking = true, startTimeMs = startTimeMs, initialNotes = notes)
        _activeSleepState.value = state
        persistActiveState(state)
    }

    fun cancelActiveSleep() {
        val state = ActiveSleepState()
        _activeSleepState.value = state
        persistActiveState(state)
    }

    suspend fun wakeUp(
        wakeTimeMs: Long = System.currentTimeMillis(),
        qualityRating: String = "Good",
        awakenings: Int = 0,
        latencyMinutes: Int = 15,
        notes: String = ""
    ): SleepSessionEntity = withContext(Dispatchers.IO) {
        val currentState = _activeSleepState.value
        val bedTime = if (currentState.isTracking && currentState.startTimeMs > 0L) {
            currentState.startTimeMs
        } else {
            wakeTimeMs - (8 * 3600 * 1000L) // Default 8 hours prior if not actively tracked
        }

        val durationMs = (wakeTimeMs - bedTime).coerceAtLeast(60_000L)
        val durationMinutes = (durationMs / 60_000L).toInt()
        val durationHours = durationMinutes / 60f

        val schedule = sleepDao.getScheduleSync() ?: SleepScheduleEntity()
        val goalHours = schedule.sleepGoalHours
        val completionPercent = ((durationHours / goalHours) * 100f).coerceIn(0f, 200f)

        val morningDateStr = dateFormat.format(Date(wakeTimeMs))

        val combinedNotes = (currentState.initialNotes + if (notes.isNotBlank()) " $notes" else "").trim()

        val session = SleepSessionEntity(
            date = morningDateStr,
            bedTimeMs = bedTime,
            wakeTimeMs = wakeTimeMs,
            durationMinutes = durationMinutes,
            durationHours = (durationHours * 10f).roundToInt() / 10f,
            sleepGoalHours = goalHours,
            goalCompletionPercent = (completionPercent * 10f).roundToInt() / 10f,
            qualityRating = qualityRating,
            timeToFallAsleepMinutes = latencyMinutes,
            awakeningsCount = awakenings,
            notes = combinedNotes,
            source = "LIVE_TRACKER"
        )

        val id = sleepDao.insertSession(session)
        val savedSession = session.copy(id = id)

        // Clear active tracking state
        cancelActiveSleep()

        // Sync with legacy database & preferences for backward compatibility
        syncLegacySleep(savedSession)

        // Check milestones
        checkAndUnlockAchievements()

        savedSession
    }

    suspend fun addManualSleepSession(
        dateStr: String,
        bedTimeMs: Long,
        wakeTimeMs: Long,
        qualityRating: String,
        latencyMinutes: Int = 15,
        awakenings: Int = 0,
        notes: String = ""
    ): SleepSessionEntity = withContext(Dispatchers.IO) {
        val durationMs = (wakeTimeMs - bedTimeMs).coerceAtLeast(60_000L)
        val durationMinutes = (durationMs / 60_000L).toInt()
        val durationHours = durationMinutes / 60f

        val schedule = sleepDao.getScheduleSync() ?: SleepScheduleEntity()
        val goalHours = schedule.sleepGoalHours
        val completionPercent = ((durationHours / goalHours) * 100f).coerceIn(0f, 200f)

        val session = SleepSessionEntity(
            date = dateStr,
            bedTimeMs = bedTimeMs,
            wakeTimeMs = wakeTimeMs,
            durationMinutes = durationMinutes,
            durationHours = (durationHours * 10f).roundToInt() / 10f,
            sleepGoalHours = goalHours,
            goalCompletionPercent = (completionPercent * 10f).roundToInt() / 10f,
            qualityRating = qualityRating,
            timeToFallAsleepMinutes = latencyMinutes,
            awakeningsCount = awakenings,
            notes = notes.trim(),
            source = "MANUAL"
        )

        val id = sleepDao.insertSession(session)
        val savedSession = session.copy(id = id)

        syncLegacySleep(savedSession)
        checkAndUnlockAchievements()
        savedSession
    }

    suspend fun updateSleepSession(session: SleepSessionEntity) = withContext(Dispatchers.IO) {
        val durationMs = (session.wakeTimeMs - session.bedTimeMs).coerceAtLeast(60_000L)
        val durationMinutes = (durationMs / 60_000L).toInt()
        val durationHours = durationMinutes / 60f
        val completionPercent = ((durationHours / session.sleepGoalHours) * 100f).coerceIn(0f, 200f)

        val updated = session.copy(
            durationMinutes = durationMinutes,
            durationHours = (durationHours * 10f).roundToInt() / 10f,
            goalCompletionPercent = (completionPercent * 10f).roundToInt() / 10f
        )
        sleepDao.updateSession(updated)
        syncLegacySleep(updated)
        checkAndUnlockAchievements()
    }

    suspend fun deleteSleepSession(session: SleepSessionEntity) = withContext(Dispatchers.IO) {
        sleepDao.deleteSession(session)
        try {
            healthDao.deleteSleepRecord(
                SleepRecord(
                    id = session.id.toInt(),
                    date = session.date,
                    bedTime = session.bedTimeMs,
                    wakeTime = session.wakeTimeMs,
                    durationHours = session.durationHours,
                    quality = session.qualityRating
                )
            )
        } catch (_: Exception) {}
    }

    private suspend fun syncLegacySleep(session: SleepSessionEntity) {
        try {
            healthDao.insertSleepRecord(
                SleepRecord(
                    id = if (session.id <= Int.MAX_VALUE) session.id.toInt() else 0,
                    date = session.date,
                    bedTime = session.bedTimeMs,
                    wakeTime = session.wakeTimeMs,
                    durationHours = session.durationHours,
                    quality = session.qualityRating
                )
            )
            prefsManager.saveSleepHours(session.durationHours)
        } catch (_: Exception) {}
    }

    // --- SCHEDULE & GOAL MANAGEMENT ---

    suspend fun getSchedule(): SleepScheduleEntity = withContext(Dispatchers.IO) {
        sleepDao.getScheduleSync() ?: SleepScheduleEntity()
    }

    suspend fun saveSchedule(schedule: SleepScheduleEntity) = withContext(Dispatchers.IO) {
        sleepDao.saveSchedule(schedule)
        prefsManager.saveSleepGoal(schedule.sleepGoalHours)
        context?.let { SleepReminderScheduler.reschedule(it) }
    }

    // --- ANALYTICS CALCULATIONS ---

    fun calculateAnalytics(
        sessions: List<SleepSessionEntity>,
        schedule: SleepScheduleEntity
    ): SleepAnalyticsSummary {
        val todayStr = dateFormat.format(Date())
        val latest = sessions.firstOrNull()

        val todayHours = latest?.takeIf { it.date == todayStr }?.durationHours
            ?: latest?.durationHours ?: 0f
        val todayGoal = schedule.sleepGoalHours
        val todayPercent = if (todayGoal > 0f) ((todayHours / todayGoal) * 100f).coerceIn(0f, 150f) else 0f

        val lastNightBed = latest?.let { timeFormat.format(Date(it.bedTimeMs)) } ?: "--:--"
        val lastNightWake = latest?.let { timeFormat.format(Date(it.wakeTimeMs)) } ?: "--:--"
        val lastNightQual = latest?.qualityRating ?: "None"

        // Last 7 days aggregation
        val cal = Calendar.getInstance()
        val last7Days = mutableListOf<DaySleepStat>()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        var weeklyTotalHours = 0f
        var weeklyGoalMetCount = 0
        var consistentDaysCount = 0

        for (i in 6 downTo 0) {
            val dCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = dateFormat.format(dCal.time)
            val dayName = dayLabels[dCal.get(Calendar.DAY_OF_WEEK) - 1]

            val daySession = sessions.firstOrNull { it.date == dateStr }
            val hours = daySession?.durationHours ?: 0f
            val isMet = hours >= schedule.sleepGoalHours
            if (isMet) weeklyGoalMetCount++
            weeklyTotalHours += hours

            // Check schedule consistency (bedtime within 45 mins of scheduled)
            if (daySession != null) {
                val isWeekend = dCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val targetHour = if (isWeekend && schedule.isWeekendDifferent) schedule.weekendBedtimeHour else schedule.weekdayBedtimeHour
                val targetMinute = if (isWeekend && schedule.isWeekendDifferent) schedule.weekendBedtimeMinute else schedule.weekdayBedtimeMinute

                val actualBedCal = Calendar.getInstance().apply { timeInMillis = daySession.bedTimeMs }
                val actualHour = actualBedCal.get(Calendar.HOUR_OF_DAY)
                val actualMinute = actualBedCal.get(Calendar.MINUTE)

                val diffMinutes = abs((actualHour * 60 + actualMinute) - (targetHour * 60 + targetMinute))
                if (diffMinutes <= 45 || diffMinutes >= (24 * 60 - 45)) {
                    consistentDaysCount++
                }
            }

            last7Days.add(
                DaySleepStat(
                    date = dateStr,
                    dayLabel = dayName,
                    durationHours = hours,
                    goalHours = schedule.sleepGoalHours,
                    isGoalMet = isMet,
                    qualityRating = daySession?.qualityRating ?: "None"
                )
            )
        }

        val weeklyAvg = (weeklyTotalHours / 7f * 10f).roundToInt() / 10f
        val weeklyAchievementRate = ((weeklyGoalMetCount.toFloat() / 7f) * 100f).coerceIn(0f, 100f)
        val weeklyConsistency = ((consistentDaysCount.toFloat() / 7f) * 100f).roundToInt().coerceIn(0, 100)

        val bestDay = last7Days.maxByOrNull { it.durationHours }
        val bestDayLabel = if (bestDay != null && bestDay.durationHours > 0f) {
            "${bestDay.dayLabel} (${bestDay.durationHours}h)"
        } else "None"

        // Last 30 days
        val last30Days = mutableListOf<SleepSessionEntity>()
        val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        sessions.forEach {
            if (it.wakeTimeMs >= thirtyDaysAgo) last30Days.add(it)
        }

        val monthlyTotal = last30Days.sumOf { it.durationHours.toDouble() }.toFloat()
        val monthlyAvg = if (last30Days.isNotEmpty()) (monthlyTotal / last30Days.size * 10f).roundToInt() / 10f else 0f
        val monthlyGoalMet = last30Days.count { it.durationHours >= schedule.sleepGoalHours }

        val qualityMap = mutableMapOf("Excellent" to 0, "Good" to 0, "Average" to 0, "Poor" to 0, "Very Poor" to 0)
        sessions.forEach {
            val q = it.qualityRating
            if (qualityMap.containsKey(q)) {
                qualityMap[q] = qualityMap[q]!! + 1
            }
        }

        return SleepAnalyticsSummary(
            todayHours = todayHours,
            todayGoalHours = todayGoal,
            todayCompletionPercent = todayPercent,
            lastNightBedtime = lastNightBed,
            lastNightWakeTime = lastNightWake,
            lastNightQuality = lastNightQual,
            weeklyAvgHours = weeklyAvg,
            weeklyGoalAchievementRate = weeklyAchievementRate,
            weeklyScheduleConsistencyPercent = weeklyConsistency,
            bestDayLabel = bestDayLabel,
            weeklyDays = last7Days,
            monthlyTotalHours = (monthlyTotal * 10f).roundToInt() / 10f,
            monthlyAvgHours = monthlyAvg,
            monthlyDaysCount = last30Days.size,
            monthlyGoalMetDays = monthlyGoalMet,
            qualityDistribution = qualityMap
        )
    }

    // --- STREAK CALCULATION ---

    fun calculateStreak(sessions: List<SleepSessionEntity>, goalHours: Float): SleepStreakSummary {
        if (sessions.isEmpty()) return SleepStreakSummary(0, 0, 0, 0)

        val sessionsByDate = sessions.associateBy { it.date }
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        // Check if today has met goal
        var currentStreak = 0
        var checkCal = Calendar.getInstance()

        // If today has met goal, include today; else check yesterday
        val todaySession = sessionsByDate[todayStr]
        if (todaySession != null && todaySession.durationHours >= goalHours) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dStr = dateFormat.format(checkCal.time)
            val s = sessionsByDate[dStr]
            if (s != null && s.durationHours >= goalHours) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 0
        val sortedDates = sessionsByDate.keys.sorted()
        for (i in sortedDates.indices) {
            val s = sessionsByDate[sortedDates[i]]
            if (s != null && s.durationHours >= goalHours) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }
        if (currentStreak > longestStreak) longestStreak = currentStreak

        val completedCount = sessions.count { it.durationHours >= goalHours }
        val missedCount = sessions.size - completedCount

        return SleepStreakSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completedDays = completedCount,
            missedDays = missedCount
        )
    }

    // --- RECOMMENDATIONS (Rule-Based) ---

    fun generateRecommendations(
        analytics: SleepAnalyticsSummary,
        schedule: SleepScheduleEntity,
        sessions: List<SleepSessionEntity>
    ): List<SleepRecommendation> {
        val list = mutableListOf<SleepRecommendation>()

        // 1. Sleep Duration Deficit
        if (analytics.weeklyAvgHours > 0f && analytics.weeklyAvgHours < schedule.sleepGoalHours - 0.5f) {
            val deficit = ((schedule.sleepGoalHours - analytics.weeklyAvgHours) * 10f).roundToInt() / 10f
            list.add(
                SleepRecommendation(
                    id = "rec_duration_deficit",
                    title = "Sleep Debt Notice",
                    message = "Your 7-day average of ${analytics.weeklyAvgHours}h is ${deficit}h below your goal. Shifting your bedtime 30 minutes earlier helps recover cellular vitality.",
                    iconEmoji = "⏳",
                    category = "DURATION",
                    severity = "SUGGESTION"
                )
            )
        }

        // 2. Schedule Inconsistency
        if (analytics.weeklyScheduleConsistencyPercent < 70 && sessions.size >= 3) {
            list.add(
                SleepRecommendation(
                    id = "rec_consistency",
                    title = "Circadian Alignment",
                    message = "Your bedtime variance is fluctuating. Keeping a regular bedtime within 30 minutes reinforces your body's natural melatonin production.",
                    iconEmoji = "🎯",
                    category = "CONSISTENCY",
                    severity = "SUGGESTION"
                )
            )
        }

        // 3. Social Jetlag (Weekday vs Weekend Bedtime Gap)
        val weekdayBed = schedule.weekdayBedtimeHour * 60 + schedule.weekdayBedtimeMinute
        val weekendBed = schedule.weekendBedtimeHour * 60 + schedule.weekendBedtimeMinute
        val shiftMinutes = abs(weekendBed - weekdayBed)
        if (shiftMinutes >= 90) {
            list.add(
                SleepRecommendation(
                    id = "rec_weekend_shift",
                    title = "Social Jetlag Warning",
                    message = "Weekend bedtime differs by ${shiftMinutes / 60}h ${shiftMinutes % 60}m. Keeping weekend bedtimes within 1 hour of weekdays prevents Monday morning fatigue.",
                    iconEmoji = "🌙",
                    category = "SCHEDULE",
                    severity = "INFO"
                )
            )
        }

        // 4. Repeated Poor Sleep Quality
        val poorCount = sessions.take(7).count { it.qualityRating == "Poor" || it.qualityRating == "Very Poor" }
        if (poorCount >= 3) {
            list.add(
                SleepRecommendation(
                    id = "rec_quality_dip",
                    title = "Sleep Environment Optimization",
                    message = "Multiple sessions were rated low in restorative quality. Consider keeping bedroom temperature cool (65-68°F), dimming screens 1 hour prior to sleep, and testing our relaxation soundscapes.",
                    iconEmoji = "🛋️",
                    category = "WELLNESS",
                    severity = "SUGGESTION"
                )
            )
        }

        // 5. Celebration
        if (analytics.weeklyGoalAchievementRate >= 85f && sessions.size >= 4) {
            list.add(
                SleepRecommendation(
                    id = "rec_champion_praise",
                    title = "Outstanding Sleep Consistency! 🌟",
                    message = "You have met your sleep target for over 85% of recent nights. Consistent sleep boosts mental focus, immunity, and athletic recovery.",
                    iconEmoji = "🏆",
                    category = "WELLNESS",
                    severity = "CELEBRATION"
                )
            )
        }

        // Default baseline recommendation if list is small
        if (list.size < 2) {
            list.add(
                SleepRecommendation(
                    id = "rec_evening_winddown",
                    title = "Evening Wind-Down Habit",
                    message = "Try listening to Rain or Ocean soundscapes 15 minutes before lights-out to gently transition your brain into slow-wave relaxation.",
                    iconEmoji = "🎧",
                    category = "WELLNESS",
                    severity = "INFO"
                )
            )
        }

        return list
    }

    // --- ACHIEVEMENTS UNLOCKING ---

    suspend fun checkAndUnlockAchievements() = withContext(Dispatchers.IO) {
        val totalSessions = sleepDao.getTotalSessionsCount()
        val all = sleepDao.getAllSessionsList()
        val schedule = sleepDao.getScheduleSync() ?: SleepScheduleEntity()
        val now = System.currentTimeMillis()

        // 1. First Night
        if (totalSessions >= 1) {
            sleepDao.unlockAchievement("first_night", now)
        }
        // 2. Sleep Beginner
        if (totalSessions >= 3) {
            sleepDao.unlockAchievement("beginner_3", now)
        }
        // 3. Sleep Champion (7 days goal met)
        val goalMetCount = all.count { it.durationHours >= schedule.sleepGoalHours }
        if (goalMetCount >= 7) {
            sleepDao.unlockAchievement("champion_7", now)
        }
        // 4. Consistent Sleeper (streak >= 7)
        val streak = calculateStreak(all, schedule.sleepGoalHours)
        if (streak.currentStreak >= 7 || streak.longestStreak >= 7) {
            sleepDao.unlockAchievement("consistent_7", now)
        }
        // 5. Sleep Master (30 days goal met)
        if (goalMetCount >= 30) {
            sleepDao.unlockAchievement("master_30", now)
        }
    }

    // --- HEALTH CONNECT IMPORT INTEGRATION ---

    suspend fun syncHealthConnectSleep(): Int = withContext(Dispatchers.IO) {
        val client = healthConnectManager?.healthConnectClient ?: return@withContext 0
        var importedCount = 0

        try {
            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30L * 24 * 3600) // Last 30 days
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)
            val schedule = sleepDao.getScheduleSync() ?: SleepScheduleEntity()

            for (record in response.records) {
                val extId = record.metadata.id
                val existing = sleepDao.findByExternalId(extId)
                if (existing != null) continue

                val bedMs = record.startTime.toEpochMilli()
                val wakeMs = record.endTime.toEpochMilli()
                val durationMs = (wakeMs - bedMs).coerceAtLeast(60_000L)
                val durationMinutes = (durationMs / 60_000L).toInt()
                val durationHours = durationMinutes / 60f

                val morningDate = dateFormat.format(Date(wakeMs))
                val goal = schedule.sleepGoalHours
                val completionPercent = ((durationHours / goal) * 100f).coerceIn(0f, 200f)

                val newSession = SleepSessionEntity(
                    date = morningDate,
                    bedTimeMs = bedMs,
                    wakeTimeMs = wakeMs,
                    durationMinutes = durationMinutes,
                    durationHours = (durationHours * 10f).roundToInt() / 10f,
                    sleepGoalHours = goal,
                    goalCompletionPercent = (completionPercent * 10f).roundToInt() / 10f,
                    qualityRating = "Good",
                    timeToFallAsleepMinutes = 15,
                    awakeningsCount = 0,
                    notes = record.notes ?: "Imported from Health Connect",
                    source = "HEALTH_CONNECT",
                    externalId = extId
                )

                sleepDao.insertSession(newSession)
                syncLegacySleep(newSession)
                importedCount++
            }

            if (importedCount > 0) {
                checkAndUnlockAchievements()
            }
        } catch (e: Exception) {
            android.util.Log.e("SleepRepository", "Health Connect sync failed: ${e.message}")
        }

        importedCount
    }
}
