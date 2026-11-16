package com.pro.aifitnesstracker.data.sleep

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted sleep session records (Manual, Live Tracking, or Imported from Health Connect).
 */
@Entity(
    tableName = "sleep_session_records",
    indices = [
        Index(value = ["date"]),
        Index(value = ["bedTimeMs"]),
        Index(value = ["externalId"], unique = true)
    ]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // yyyy-MM-dd representing the primary waking date
    val bedTimeMs: Long,
    val wakeTimeMs: Long,
    val durationMinutes: Int,
    val durationHours: Float,
    val sleepGoalHours: Float,
    val goalCompletionPercent: Float,
    val qualityRating: String, // "Very Poor", "Poor", "Average", "Good", "Excellent"
    val timeToFallAsleepMinutes: Int = 15,
    val awakeningsCount: Int = 0,
    val notes: String = "",
    val source: String = "MANUAL", // "MANUAL", "LIVE_TRACKER", "HEALTH_CONNECT"
    val externalId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sleep Schedule and Reminder Configuration.
 * Single row table with id = 1.
 */
@Entity(tableName = "sleep_schedule_config")
data class SleepScheduleEntity(
    @PrimaryKey val id: Int = 1,
    val sleepGoalHours: Float = 8.0f,
    // Weekday schedule (Mon-Fri)
    val weekdayBedtimeHour: Int = 22,
    val weekdayBedtimeMinute: Int = 30,
    val weekdayWakeupHour: Int = 6,
    val weekdayWakeupMinute: Int = 30,
    // Weekend schedule (Sat-Sun)
    val weekendBedtimeHour: Int = 23,
    val weekendBedtimeMinute: Int = 0,
    val weekendWakeupHour: Int = 7,
    val weekendWakeupMinute: Int = 30,
    val isWeekendDifferent: Boolean = true,
    // Reminders
    val bedtimeReminderEnabled: Boolean = true,
    val wakeupReminderEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 30
)

/**
 * Persisted sleep achievement milestones.
 */
@Entity(tableName = "sleep_achievements")
data class SleepAchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long? = null,
    val isUnlocked: Boolean = false
)

/**
 * Transient state of an active sleep tracking session.
 */
data class ActiveSleepState(
    val isTracking: Boolean = false,
    val startTimeMs: Long = 0L,
    val initialNotes: String = ""
)

/**
 * Aggregated analytics for a single day.
 */
data class DaySleepStat(
    val date: String, // yyyy-MM-dd
    val dayLabel: String, // Mon, Tue, etc.
    val durationHours: Float,
    val goalHours: Float,
    val isGoalMet: Boolean,
    val qualityRating: String
)

/**
 * Comprehensive Sleep Analytics data bundle.
 */
data class SleepAnalyticsSummary(
    val todayHours: Float,
    val todayGoalHours: Float,
    val todayCompletionPercent: Float,
    val lastNightBedtime: String,
    val lastNightWakeTime: String,
    val lastNightQuality: String,
    val weeklyAvgHours: Float,
    val weeklyGoalAchievementRate: Float,
    val weeklyScheduleConsistencyPercent: Int,
    val bestDayLabel: String,
    val weeklyDays: List<DaySleepStat>,
    val monthlyTotalHours: Float,
    val monthlyAvgHours: Float,
    val monthlyDaysCount: Int,
    val monthlyGoalMetDays: Int,
    val qualityDistribution: Map<String, Int>
)

/**
 * Sleep streak metrics.
 */
data class SleepStreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val completedDays: Int,
    val missedDays: Int
)

/**
 * Rule-based Sleep Optimization Recommendation.
 */
data class SleepRecommendation(
    val id: String,
    val title: String,
    val message: String,
    val iconEmoji: String,
    val category: String, // "CONSISTENCY", "DURATION", "SCHEDULE", "WELLNESS"
    val severity: String // "INFO", "SUGGESTION", "CELEBRATION"
)
