package com.pro.aifitnesstracker.data.step

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted daily summary of user step activity.
 * One row per calendar date (yyyy-MM-dd).
 */
@Entity(tableName = "daily_step_records")
data class DailyStepEntity(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val steps: Int,
    val goal: Int,
    val distanceMeters: Float,
    val calories: Int,
    val activeSeconds: Int,
    val sessionsCount: Int,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Individual continuous walking session detected by the cadence analyzer
 * or manually triggered by the user.
 */
@Entity(
    tableName = "walking_sessions",
    indices = [Index(value = ["date"]), Index(value = ["startTimeMs"])]
)
data class WalkingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: yyyy-MM-dd
    val startTimeMs: Long,
    val endTimeMs: Long,
    val stepsCount: Int,
    val distanceMeters: Float,
    val calories: Int,
    val durationSeconds: Int,
    val averageCadenceSpm: Int = if (durationSeconds > 0) (stepsCount * 60 / durationSeconds) else 0
)

/**
 * Step milestones and streak achievements.
 */
@Entity(tableName = "step_achievements")
data class StepAchievementEntity(
    @PrimaryKey
    val achievementId: String,
    val title: String,
    val description: String,
    val icon: String,
    val targetValue: Int,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null,
    val unlockedTimestamp: Long? = null
)
