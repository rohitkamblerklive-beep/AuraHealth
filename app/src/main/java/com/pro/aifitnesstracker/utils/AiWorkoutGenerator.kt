package com.pro.aifitnesstracker.utils

import com.pro.aifitnesstracker.WorkoutPlanRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AiWorkoutGenerator {

    fun generate7DayPlan(
        goal: String,
        equipment: String,
        level: String,
        targetArea: String,
        duration: String,
        frequency: String,
        injuries: String,
        startDate: LocalDate = LocalDate.now()
    ): List<WorkoutPlanRecord> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val plans = mutableListOf<WorkoutPlanRecord>()

        // Parse duration (e.g. "30 min" -> 30)
        val durationMins = duration.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 30
        
        // Parse frequency (e.g. "4 days" -> 4)
        val freqDays = frequency.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 4

        // Build a list of 7 days: "Workout" or "Rest" based on frequency
        val daysStructure = buildDaysStructure(freqDays)

        daysStructure.forEachIndexed { index, dayType ->
            val currentDate = startDate.plusDays(index.toLong()).format(formatter)

            if (dayType != "Rest") {
                val plan = generateDailyWorkout(
                    goal = goal,
                    equipment = equipment,
                    level = level,
                    targetArea = targetArea,
                    durationMins = durationMins,
                    injuries = injuries,
                    dayType = dayType,
                    date = currentDate
                )
                plans.add(plan)
            }
        }

        return plans
    }

    private fun buildDaysStructure(freqDays: Int): List<String> {
        // Simple logic to distribute workout days across 7 days
        return when (freqDays) {
            3 -> listOf("Workout", "Rest", "Workout", "Rest", "Workout", "Rest", "Rest")
            4 -> listOf("Workout", "Workout", "Rest", "Workout", "Workout", "Rest", "Rest")
            5 -> listOf("Workout", "Workout", "Workout", "Rest", "Workout", "Workout", "Rest")
            6 -> listOf("Workout", "Workout", "Workout", "Workout", "Workout", "Workout", "Rest")
            else -> listOf("Workout", "Workout", "Rest", "Workout", "Cardio", "Workout", "Rest")
        }
    }

    private fun generateDailyWorkout(
        goal: String,
        equipment: String,
        level: String,
        targetArea: String,
        durationMins: Int,
        injuries: String,
        dayType: String,
        date: String
    ): WorkoutPlanRecord {
        val isCardio = dayType == "Cardio" || (goal.lowercase() == "weight loss" && Math.random() > 0.7)
        val category = if (isCardio) "Cardio" else "Strength"
        
        val intensity = when (level.lowercase()) {
            "advanced" -> "High"
            "intermediate" -> "Medium"
            else -> "Low"
        }

        val (title, subtitle, exercises) = getExercisesForProfile(
            goal = goal,
            equipment = equipment,
            targetArea = targetArea,
            isCardio = isCardio,
            durationMins = durationMins,
            injuries = injuries
        )

        return WorkoutPlanRecord(
            date = date,
            title = title,
            subtitle = subtitle,
            duration = "$durationMins min",
            intensity = intensity,
            category = category,
            exercises = exercises
        )
    }

    private fun getExercisesForProfile(
        goal: String, 
        equipment: String, 
        targetArea: String, 
        isCardio: Boolean,
        durationMins: Int,
        injuries: String
    ): Triple<String, String, String> {
        
        // Adjust sets based on duration
        val sets = if (durationMins <= 15) "2" else if (durationMins <= 30) "3" else "4"
        
        // Filter out bad exercises if injuries are present
        val hasBadKnees = injuries.lowercase().contains("bad knees")
        val hasBadBack = injuries.lowercase().contains("lower back pain")

        if (isCardio) {
            val cardioMoves = if (hasBadKnees) {
                "Shadow Boxing - 60s, Seated Punches - 60s, Low Impact Step Jacks - 60s, Core Plank - 60s, Rest - 30s (Repeat ${sets}x)"
            } else {
                "Jumping Jacks - 60s, High Knees - 60s, Burpees - 15 reps, Mountain Climbers - 60s, Rest - 30s (Repeat ${sets}x)"
            }
            return Triple("Fat Burner HIIT", "High Intensity Interval Training", cardioMoves)
        }

        val safeSquat = if (hasBadKnees) "Glute Bridges - ${sets}x15" else "Squats - ${sets}x15"
        val safeLunge = if (hasBadKnees) "Clamshells - ${sets}x15" else "Lunges - ${sets}x15"
        val safeDeadlift = if (hasBadBack) "Seated Leg Curls - ${sets}x15" else "Deadlifts - ${sets}x10"
        val safeCore = if (hasBadBack) "Dead Bugs - ${sets}x20" else "Crunches - ${sets}x20"

        return when (targetArea.lowercase()) {
            "upper body" -> {
                Triple(
                    "Upper Body Pump",
                    if (equipment.lowercase() == "no equipment") "Bodyweight Upper" else "Weighted Upper Focus",
                    if (equipment.lowercase() == "no equipment") {
                        "Pushups - ${sets}x15, Pike Pushups - ${sets}x10, Inverted Rows/Superman - ${sets}x12, Tricep Dips - ${sets}x12, $safeCore"
                    } else {
                        "Bench/Floor Press - ${sets}x10, Dumbbell Rows - ${sets}x12, Shoulder Press - ${sets}x10, Bicep Curls - ${sets}x12, $safeCore"
                    }
                )
            }
            "lower body" -> {
                Triple(
                    "Lower Body Power",
                    "Legs & Glutes Builder",
                    if (equipment.lowercase() == "no equipment") {
                        "$safeSquat, $safeLunge, Calf Raises - ${sets}x20, Wall Sit - 45s, $safeCore"
                    } else {
                        "Goblet $safeSquat, Weighted $safeLunge, $safeDeadlift, Weighted Calf Raises - ${sets}x15, $safeCore"
                    }
                )
            }
            "core" -> {
                Triple(
                    "Core Crusher",
                    "Abs & Obliques",
                    "Plank - 60s, $safeCore, Russian Twists - ${sets}x20, Leg Raises - ${sets}x15, Mountain Climbers - ${sets}x30"
                )
            }
            else -> { // Full Body
                Triple(
                    "Full Body Mastery",
                    "Complete Conditioning",
                    if (equipment.lowercase() == "no equipment") {
                        "Pushups - ${sets}x15, $safeSquat, Superman - ${sets}x12, $safeLunge, $safeCore"
                    } else {
                        "Dumbbell Press - ${sets}x10, Goblet $safeSquat, Dumbbell Rows - ${sets}x10, $safeDeadlift, $safeCore"
                    }
                )
            }
        }
    }
}
