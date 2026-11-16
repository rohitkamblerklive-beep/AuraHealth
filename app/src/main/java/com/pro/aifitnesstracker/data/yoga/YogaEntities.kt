package com.pro.aifitnesstracker.data.yoga

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "yoga_session_records")
data class YogaSessionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val sessionTitle: String,
    val category: String,
    val plannedDurationSeconds: Int,
    val completedDurationSeconds: Int,
    val caloriesBurned: Int,
    val posesCompleted: Int,
    val totalPoses: Int,
    val isFullyCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_yoga_routines")
data class CustomYogaRoutineEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val poseIds: String, // Comma-separated pose IDs
    val poseDurations: String, // Comma-separated seconds
    val restIntervalSeconds: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "yoga_favorites")
data class YogaFavoriteEntity(
    @PrimaryKey
    val poseId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "yoga_plan_progress")
data class YogaPlanProgressEntity(
    @PrimaryKey
    val planId: String,
    val currentDay: Int = 1,
    val isStarted: Boolean = false,
    val isPaused: Boolean = false,
    val completedDays: String = "", // Comma-separated day numbers: "1,2,3"
    val startedAt: Long = System.currentTimeMillis(),
    val lastCompletedDate: String? = null
)

@Entity(tableName = "yoga_achievements")
data class YogaAchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
