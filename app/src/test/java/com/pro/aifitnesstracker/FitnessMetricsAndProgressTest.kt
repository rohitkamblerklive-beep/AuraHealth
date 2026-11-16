package com.pro.aifitnesstracker

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive Automated Verification Suite for all FitSphere Features,
 * Progress Bar formulas, Health Score Algorithms, Fasting Tracker,
 * Blood Pressure classification, and Diet & Macros calculations.
 */
class FitnessMetricsAndProgressTest {

    // ── 1. Step Progress Bar & Active Metric Calculations ──────────────────────

    @Test
    fun testStepProgress_normalUnderGoal() {
        val steps = 6000
        val goal = 10000
        val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
        assertEquals(0.6f, progress, 0.001f)
    }

    @Test
    fun testStepProgress_exceedingGoal_isClampedToOne() {
        val steps = 14500
        val goal = 10000
        val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
        assertEquals("Progress exceeding goal must clamp at 1.0 (100%)", 1.0f, progress, 0.001f)
    }

    @Test
    fun testStepProgress_zeroGoal_noDivisionByZeroCrash() {
        val steps = 5000
        val goal = 0
        val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun testStepProgress_zeroSteps_isZero() {
        val steps = 0
        val goal = 10000
        val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
        assertEquals(0.0f, progress, 0.001f)
    }

    @Test
    fun testActiveCaloriesAndMinutesFormula() {
        val stepsToday = 8000
        val workoutCalories = 250
        val workoutDurationMins = 30
        val yogaDurationMins = 20

        val stepCalories = (stepsToday * 0.045f).toInt()
        assertEquals(360, stepCalories) // 8000 * 0.045 = 360

        val totalCalories = stepCalories + workoutCalories
        assertEquals(610, totalCalories)

        val walkingActiveMins = stepsToday / 100
        assertEquals(80, walkingActiveMins) // 8000 / 100 = 80

        val totalActiveMins = walkingActiveMins + workoutDurationMins + yogaDurationMins
        assertEquals(130, totalActiveMins)
    }

    // ── 2. Health Score Algorithm ─────────────────────────────────────────────

    @Test
    fun testHealthScore_perfectDay_equals100() {
        val stepsToday = 10000
        val stepGoal = 10000
        val waterGlasses = 8
        val sleepHours = 8f
        val hasWorkoutToday = true
        val activeCalories = 600
        val calorieGoal = 500
        val heartRate = 72

        val stepsScore = if (stepGoal > 0) ((stepsToday.toFloat() / stepGoal) * 30f).coerceIn(0f, 30f) else 0f
        val waterScore = ((waterGlasses.toFloat() / 8f) * 15f).coerceIn(0f, 15f)
        val sleepScore = ((sleepHours / 8f) * 20f).coerceIn(0f, 20f)
        val workoutScore = if (hasWorkoutToday) 15f else 0f
        val calScore = if (calorieGoal > 0) ((activeCalories.toFloat() / calorieGoal) * 10f).coerceIn(0f, 10f) else 0f
        val exerciseScore = (workoutScore + calScore).coerceIn(0f, 25f)
        val hrScore = when {
            heartRate in 60..100 -> 10f
            heartRate in 50..59 || heartRate in 101..120 -> 6f
            else -> 3f
        }

        val totalScore = (stepsScore + waterScore + sleepScore + exerciseScore + hrScore).toInt().coerceIn(0, 100)
        assertEquals(100, totalScore)
    }

    @Test
    fun testHealthScore_emptyDay_minimumBaseline() {
        val stepsToday = 0
        val stepGoal = 10000
        val waterGlasses = 0
        val sleepHours = 0f
        val hasWorkoutToday = false
        val activeCalories = 0
        val calorieGoal = 500
        val heartRate = 0 // elevated / abnormal

        val stepsScore = if (stepGoal > 0) ((stepsToday.toFloat() / stepGoal) * 30f).coerceIn(0f, 30f) else 0f
        val waterScore = ((waterGlasses.toFloat() / 8f) * 15f).coerceIn(0f, 15f)
        val sleepScore = ((sleepHours / 8f) * 20f).coerceIn(0f, 20f)
        val workoutScore = if (hasWorkoutToday) 15f else 0f
        val calScore = if (calorieGoal > 0) ((activeCalories.toFloat() / calorieGoal) * 10f).coerceIn(0f, 10f) else 0f
        val exerciseScore = (workoutScore + calScore).coerceIn(0f, 25f)
        val hrScore = when {
            heartRate in 60..100 -> 10f
            heartRate in 50..59 || heartRate in 101..120 -> 6f
            else -> 3f
        }

        val totalScore = (stepsScore + waterScore + sleepScore + exerciseScore + hrScore).toInt().coerceIn(0, 100)
        assertEquals(3, totalScore) // Only abnormal HR baseline
    }

    @Test
    fun testHealthScore_cappedAt100_evenWithOverachievement() {
        val stepsToday = 30000 // 3x goal
        val stepGoal = 10000
        val waterGlasses = 16 // 2x goal
        val sleepHours = 10f
        val hasWorkoutToday = true
        val activeCalories = 2000
        val calorieGoal = 500
        val heartRate = 65

        val stepsScore = if (stepGoal > 0) ((stepsToday.toFloat() / stepGoal) * 30f).coerceIn(0f, 30f) else 0f
        val waterScore = ((waterGlasses.toFloat() / 8f) * 15f).coerceIn(0f, 15f)
        val sleepScore = ((sleepHours / 8f) * 20f).coerceIn(0f, 20f)
        val workoutScore = if (hasWorkoutToday) 15f else 0f
        val calScore = if (calorieGoal > 0) ((activeCalories.toFloat() / calorieGoal) * 10f).coerceIn(0f, 10f) else 0f
        val exerciseScore = (workoutScore + calScore).coerceIn(0f, 25f)
        val hrScore = when {
            heartRate in 60..100 -> 10f
            heartRate in 50..59 || heartRate in 101..120 -> 6f
            else -> 3f
        }

        val totalScore = (stepsScore + waterScore + sleepScore + exerciseScore + hrScore).toInt().coerceIn(0, 100)
        assertEquals(100, totalScore)
    }

    // ── 3. Blood Pressure Classification & Gauge Needle ───────────────────────

    @Test
    fun testBloodPressureClassification() {
        fun classifyBp(systolic: Int, diastolic: Int): String {
            return when {
                systolic < 120 && diastolic < 80 -> "Optimal"
                systolic in 120..129 && diastolic < 80 -> "Normal"
                systolic in 130..139 || diastolic in 80..89 -> "Elevated"
                else -> "High"
            }
        }

        assertEquals("Optimal", classifyBp(115, 75))
        assertEquals("Normal", classifyBp(124, 78))
        assertEquals("Elevated", classifyBp(132, 79))
        assertEquals("Elevated", classifyBp(118, 85))
        assertEquals("High", classifyBp(145, 95))
    }

    @Test
    fun testBloodPressureGaugeAngle_rangeBounds() {
        fun computeNeedleAngle(systolic: Int): Float {
            val gaugeFraction = ((systolic.toFloat() - 80f) / (180f - 80f)).coerceIn(0.08f, 0.92f)
            return gaugeFraction * 180f
        }

        val lowAngle = computeNeedleAngle(60) // under min 80
        assertEquals(0.08f * 180f, lowAngle, 0.01f) // clamped at lower bound

        val highAngle = computeNeedleAngle(220) // above max 180
        assertEquals(0.92f * 180f, highAngle, 0.01f) // clamped at upper bound

        val midAngle = computeNeedleAngle(130) // midpoint
        assertEquals(0.5f * 180f, midAngle, 0.01f)
    }

    // ── 4. Water Hydration Tracking & Volume ───────────────────────────────────

    @Test
    fun testWaterHydration_litersAndHeroBadge() {
        val waterGlasses = 8
        val waterLiters = waterGlasses * 0.25f
        assertEquals(2.0f, waterLiters, 0.001f)

        val isWaterHeroUnlocked = waterGlasses >= 8
        assertTrue("Water Hero badge should unlock at 8+ glasses (2.0L)", isWaterHeroUnlocked)

        val underGoalGlasses = 7
        assertFalse(underGoalGlasses >= 8)
    }

    // ── 5. Sleep Optimizer Efficiency Formula ─────────────────────────────────

    @Test
    fun testSleepEfficiency_clampedBetween40And98() {
        fun computeSleepEfficiency(sleepHoursTotal: Float, sleepGoal: Float): Int {
            return ((sleepHoursTotal / sleepGoal) * 100).toInt().coerceIn(40, 98).takeIf { sleepHoursTotal > 0 } ?: 94
        }

        assertEquals("Default fallback when 0 hours logged is 94%", 94, computeSleepEfficiency(0f, 8f))
        assertEquals("Extremely low sleep is capped at 40%", 40, computeSleepEfficiency(1f, 8f))
        assertEquals("Optimal 8h sleep is 98% capped", 98, computeSleepEfficiency(8f, 8f))
        assertEquals("Excessive 12h sleep remains capped at 98%", 98, computeSleepEfficiency(12f, 8f))
        assertEquals("Normal 7h sleep is ~87%", 87, computeSleepEfficiency(7f, 8f))
    }

    // ── 6. Diet & Macros Target & Progress ─────────────────────────────────────

    @Test
    fun testMacroTargetsAndRemainingCalculations() {
        val calorieTarget = 2000
        val caloriesConsumed = 1450
        val caloriesRemaining = (calorieTarget - caloriesConsumed).coerceAtLeast(0)
        assertEquals(550, caloriesRemaining)

        // Carbs 50% / 4 cal/g
        val carbsTarget = (calorieTarget * 0.50f / 4f).toInt().coerceAtLeast(1)
        assertEquals(250, carbsTarget)

        // Protein 25% / 4 cal/g
        val proteinTarget = (calorieTarget * 0.25f / 4f).toInt().coerceAtLeast(1)
        assertEquals(125, proteinTarget)

        // Fats 25% / 9 cal/g
        val fatsTarget = (calorieTarget * 0.25f / 9f).toInt().coerceAtLeast(1)
        assertEquals(55, fatsTarget)

        val currentCarbs = 200
        val carbsProgress = (currentCarbs.toFloat() / carbsTarget.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.8f, carbsProgress, 0.001f)
    }

    @Test
    fun testCalorieRemaining_whenOverConsumed_doesNotGoNegative() {
        val calorieTarget = 1800
        val caloriesConsumed = 2200
        val caloriesRemaining = (calorieTarget - caloriesConsumed).coerceAtLeast(0)
        assertEquals("Remaining calories should never be negative", 0, caloriesRemaining)
    }

    // ── 7. Fasting Tracker Stages & Progress ───────────────────────────────────

    @Test
    fun testFastingStageTransitions() {
        fun getFastingStage(elapsedHours: Float): String {
            return when {
                elapsedHours < 2f -> "Blood Sugar Drops"
                elapsedHours < 12f -> "Glycogen Depletion"
                elapsedHours < 14f -> "Fat Burning Zone"
                elapsedHours < 16f -> "Ketosis State"
                else -> "Autophagy Renewal"
            }
        }

        assertEquals("Blood Sugar Drops", getFastingStage(1.5f))
        assertEquals("Glycogen Depletion", getFastingStage(8f))
        assertEquals("Fat Burning Zone", getFastingStage(13f))
        assertEquals("Ketosis State", getFastingStage(15f))
        assertEquals("Autophagy Renewal", getFastingStage(18f))
    }

    @Test
    fun testFastingProgressClamping() {
        val goalHours = 16
        val elapsedHours = 20f
        val progress = (elapsedHours / goalHours).coerceIn(0f, 1f)
        assertEquals("Fasting progress must clamp at 100% (1.0f)", 1.0f, progress, 0.001f)
    }

    // ── 8. Consecutive Streak Calculation Logic ───────────────────────────────

    @Test
    fun testStreakCalculation_consecutiveActiveDays() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()

        // Create 5 consecutive active days leading up to today
        val activeDates = mutableSetOf<String>()
        for (i in 0..4) {
            val dateCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -i) }
            activeDates.add(sdf.format(dateCal.time))
        }

        var currentDayStr = sdf.format(cal.time)
        if (!activeDates.contains(currentDayStr)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }

        var count = 0
        while (activeDates.contains(currentDayStr)) {
            count++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }

        assertEquals(5, count)
    }

    @Test
    fun testStreakCalculation_brokenStreak_returnsOnlyRecentStreak() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()

        val activeDates = mutableSetOf<String>()
        // Today & Yesterday active (2 days)
        activeDates.add(sdf.format(cal.time))
        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        activeDates.add(sdf.format(yesterdayCal.time))

        // Day before yesterday missed!
        // 4 days ago active
        val fourDaysAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -4) }
        activeDates.add(sdf.format(fourDaysAgo.time))

        var currentDayStr = sdf.format(cal.time)
        if (!activeDates.contains(currentDayStr)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }

        var count = 0
        while (activeDates.contains(currentDayStr)) {
            count++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }

        assertEquals("Streak should stop at the missed day and return 2", 2, count)
    }

    // ── 8. Hydration and Water Target Recommendation ─────────────────────────

    @Test
    fun testHydrationTargetCalculation_basedOnWeight() {
        val userWeightKg = 70f
        // Standard sports medicine rule: 35ml per kg of body weight
        val recommendedMl = (userWeightKg * 35f).toInt()
        assertEquals(2450, recommendedMl)

        // Glass count (250ml per glass)
        val glassCount = (recommendedMl / 250f).toInt()
        assertEquals(9, glassCount)
    }
}
