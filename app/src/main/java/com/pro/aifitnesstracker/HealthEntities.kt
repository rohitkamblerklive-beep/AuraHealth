package com.pro.aifitnesstracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_records")
data class HeartRateRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: Int,
    val hrv: Int,
    val stress: String,
    val energy: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blood_pressure_records")
data class BloodPressureRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "step_records")
data class StepRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val steps: Int
)

@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val glasses: Int
)

@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val foodName: String,
    val calories: Int,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val bedTime: Long, // timestamp
    val wakeTime: Long, // timestamp
    val durationHours: Float,
    val quality: String // Excellent, Good, Fair, Poor
)

@Entity(tableName = "yoga_records")
data class YogaRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val poseName: String,
    val durationSeconds: Int,
    val accuracy: Int, // 0-100
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val workoutType: String,
    val repsOrMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_plan_records")
data class WorkoutPlanRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val title: String,
    val subtitle: String,
    val duration: String,
    val intensity: String,
    val exercises: String, // Comma-separated list of exercises
    val category: String = "Strength"
)

@Entity(tableName = "ai_chat_records")
data class AIChatRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

