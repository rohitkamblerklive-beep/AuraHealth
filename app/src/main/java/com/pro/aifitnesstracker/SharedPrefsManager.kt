package com.pro.aifitnesstracker

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

import android.os.Build

class SharedPrefsManager(context: Context) {
    private val prefs: SharedPreferences = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val deviceContext = context.createDeviceProtectedStorageContext()
        deviceContext.moveSharedPreferencesFrom(context, "AIFitnessPrefs")
        deviceContext.getSharedPreferences("AIFitnessPrefs", Context.MODE_PRIVATE)
    } else {
        context.getSharedPreferences("AIFitnessPrefs", Context.MODE_PRIVATE)
    }

    // --- Language Selection ---
    fun isLanguageSelected(): Boolean {
        return prefs.getBoolean("LANGUAGE_SELECTED", false)
    }

    fun saveLanguage(languageCode: String) {
        prefs.edit()
            .putBoolean("LANGUAGE_SELECTED", true)
            .putString("APP_LANGUAGE", languageCode)
            .apply()
    }

    fun getLanguage(): String {
        val defaultLang = java.util.Locale.getDefault().language
        val supportedLangs = listOf("en", "hi", "mr", "es", "fr", "de", "ja", "ar", "ru", "gu", "ta", "te", "kn", "bn", "pa", "zh", "pt", "it")
        val fallback = if (defaultLang in supportedLangs) defaultLang else "en"
        return prefs.getString("APP_LANGUAGE", fallback) ?: fallback
    }

    // --- Onboarding ---
    fun isFeaturesSeen(): Boolean = prefs.getBoolean("FEATURES_SEEN", false)
    fun setFeaturesSeen() = prefs.edit().putBoolean("FEATURES_SEEN", true).apply()
    
    fun isGoalsSelected(): Boolean = prefs.getBoolean("GOALS_SELECTED", false)
    fun setGoalsSelected() = prefs.edit().putBoolean("GOALS_SELECTED", true).apply()

    // --- Profile Setup Wizard ---
    fun isProfileSetupDone(): Boolean = prefs.getBoolean("PROFILE_SETUP_DONE", false)
    fun setProfileSetupDone() = prefs.edit().putBoolean("PROFILE_SETUP_DONE", true).apply()

    // --- Guest Mode / Offline Skip ---
    fun isGuestMode(): Boolean = prefs.getBoolean("GUEST_MODE_ACTIVE", false)
    fun setGuestMode(active: Boolean) = prefs.edit().putBoolean("GUEST_MODE_ACTIVE", active).apply()

    // --- Logout & Clear ---
    fun clearProfileData() {
        prefs.edit()
            .remove("PROFILE_SETUP_DONE")
            .remove("USER_AGE")
            .remove("USER_GENDER")
            .remove("USER_HEIGHT")
            .remove("USER_WEIGHT")
            .remove("USER_FITNESS_GOAL")
            .remove("USER_ACTIVITY_LEVEL")
            .remove("USER_NAME")
            .remove("GUEST_MODE_ACTIVE")
            .apply()
    }

    fun getUserAge(): Int = prefs.getInt("USER_AGE", 25)
    fun saveUserAge(age: Int) = prefs.edit().putInt("USER_AGE", age).apply()

    fun getUserGender(): String = prefs.getString("USER_GENDER", "Male") ?: "Male"
    fun saveUserGender(gender: String) = prefs.edit().putString("USER_GENDER", gender).apply()

    fun getUserHeight(): Int = prefs.getFloat("USER_HEIGHT", 170f).toInt()
    fun saveUserHeight(height: Int) = prefs.edit().putFloat("USER_HEIGHT", height.toFloat()).apply()

    fun getUserWeight(): Int = prefs.getFloat("USER_WEIGHT", 70f).toInt()
    fun saveUserWeight(weight: Int) = prefs.edit().putFloat("USER_WEIGHT", weight.toFloat()).apply()

    fun getUserFitnessGoal(): String = prefs.getString("USER_FITNESS_GOAL", "Stay Fit") ?: "Stay Fit"
    fun saveUserFitnessGoal(goal: String) = prefs.edit().putString("USER_FITNESS_GOAL", goal).apply()

    fun getUserActivityLevel(): String = prefs.getString("USER_ACTIVITY_LEVEL", "Lightly Active") ?: "Lightly Active"
    fun saveUserActivityLevel(level: String) = prefs.edit().putString("USER_ACTIVITY_LEVEL", level).apply()

    // --- FCM Token Storage ---
    fun getFcmToken(): String {
        return prefs.getString("FCM_REGISTRATION_TOKEN", "") ?: ""
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString("FCM_REGISTRATION_TOKEN", token).apply()
    }

    // --- User Profile Quick Access ---
    fun getUserName(): String = prefs.getString("USER_NAME", "") ?: ""
    fun saveUserName(name: String) = prefs.edit().putString("USER_NAME", name).apply()
    fun getProfileImageUri(): String = prefs.getString("USER_PROFILE_IMAGE_URI", "") ?: ""
    fun saveProfileImageUri(uri: String) = prefs.edit().putString("USER_PROFILE_IMAGE_URI", uri).apply()
    fun getCurrentStreak(): Int = prefs.getInt("CURRENT_STREAK", 0)
    fun saveCurrentStreak(days: Int) = prefs.edit().putInt("CURRENT_STREAK", days).apply()

    // --- Pedometer Goals ---
    fun getStepGoal(): Int = prefs.getInt("STEP_GOAL", 6000)
    fun saveStepGoal(goal: Int) = prefs.edit().putInt("STEP_GOAL", goal).apply()
    fun setStepGoal(goal: Int) = saveStepGoal(goal)

    // --- Water Goal ---
    fun getWaterGoal(): Int = prefs.getInt("WATER_GOAL", 8)
    fun saveWaterGoal(goal: Int) {
        val safeGoal = goal.coerceAtLeast(1)
        prefs.edit()
            .putInt("WATER_GOAL", safeGoal)
            .putInt("WATER_GOAL_ML", safeGoal * 250)
            .apply()
    }
    fun getWaterGoalMl(): Int {
        val ml = prefs.getInt("WATER_GOAL_ML", 0)
        return if (ml > 0) ml else getWaterGoal() * 250
    }
    fun saveWaterGoalMl(ml: Int) {
        val safeMl = ml.coerceAtLeast(500)
        prefs.edit()
            .putInt("WATER_GOAL_ML", safeMl)
            .putInt("WATER_GOAL", (safeMl / 250).coerceAtLeast(1))
            .apply()
    }

    // --- Sleep Goal ---
    fun getSleepGoal(): Float = prefs.getFloat("SLEEP_GOAL", 8f)
    fun saveSleepGoal(hours: Float) = prefs.edit().putFloat("SLEEP_GOAL", hours).apply()

    // --- Calorie Goal ---
    fun getCalorieGoal(): Int = prefs.getInt("CALORIE_GOAL", 2000)
    fun saveCalorieGoal(goal: Int) = prefs.edit().putInt("CALORIE_GOAL", goal).apply()

    // --- Fasting Tracker ---
    fun isFastingActive(): Boolean = prefs.getBoolean("FASTING_ACTIVE", false)
    fun setFastingActive(active: Boolean) = prefs.edit().putBoolean("FASTING_ACTIVE", active).apply()

    fun getFastingStartTime(): Long = prefs.getLong("FASTING_START_TIME", 0L)
    fun setFastingStartTime(timeInMillis: Long) = prefs.edit().putLong("FASTING_START_TIME", timeInMillis).apply()

    fun getFastingGoalHours(): Int = prefs.getInt("FASTING_GOAL_HOURS", 16)
    fun setFastingGoalHours(hours: Int) = prefs.edit().putInt("FASTING_GOAL_HOURS", hours).apply()

    fun saveFastingHoursForToday(hours: Float) {
        val date = getCurrentDateString()
        val existing = prefs.getFloat("FASTING_HOURS_$date", 0f)
        prefs.edit().putFloat("FASTING_HOURS_$date", maxOf(existing, hours)).apply()
    }

    fun getWeeklyFastingHistory(): List<Pair<String, Float>> {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val fallbackHours = listOf(16.0f, 17.2f, 16.0f, 16.5f, 16.8f, 14.5f, 14.5f)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val result = mutableListOf<Pair<String, Float>>()
        for (i in 0..6) {
            val dStr = dateFormat.format(cal.time)
            val label = dayFormat.format(cal.time)
            val saved = prefs.getFloat("FASTING_HOURS_$dStr", -1f)
            val value = if (saved >= 0f) saved else fallbackHours[i]
            result.add(label to value)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    // --- History & Analytics ---
    fun saveDailyStats(date: String, steps: Int, squats: Int) {
        prefs.edit().putString("STATS_$date", "$steps|$squats").apply()
    }

    fun getDailyStats(date: String): Pair<Int, Int> {
        val raw = prefs.getString("STATS_$date", "0|0") ?: "0|0"
        val parts = raw.split("|")
        return Pair(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
    }

    // --- Achievements & Badges ---
    fun unlockBadge(badgeId: String) {
        val unlocked = getUnlockedBadges().toMutableSet()
        unlocked.add(badgeId)
        prefs.edit().putStringSet("UNLOCKED_BADGES", unlocked).apply()
    }

    fun getUnlockedBadges(): Set<String> {
        return prefs.getStringSet("UNLOCKED_BADGES", emptySet()) ?: emptySet()
    }

    fun getTotalYogaMeditationTime(): Float {
        return prefs.getFloat("YOGA_MUDRA_TOTAL_MEDITATION_SECONDS", 0f)
    }

    fun incrementTotalYogaMeditationTime(seconds: Float) {
        val current = getTotalYogaMeditationTime()
        prefs.edit().putFloat("YOGA_MUDRA_TOTAL_MEDITATION_SECONDS", current + seconds).apply()
    }

    fun checkAndUnlockBadge(badgeId: String, onUnlock: (String) -> Unit) {
        val unlocked = getUnlockedBadges().toMutableSet()
        if (!unlocked.contains(badgeId)) {
            unlocked.add(badgeId)
            prefs.edit().putStringSet("UNLOCKED_BADGES", unlocked).apply()
            onUnlock(badgeId)
        }
    }


    fun getTotalSquats(): Int {
        return prefs.getInt("TOTAL_SQUATS", 0)
    }

    fun addTotalSquats(reps: Int) {
        val current = getTotalSquats()
        prefs.edit().putInt("TOTAL_SQUATS", current + reps).apply()
    }

    fun getBestSession(): Int {
        return prefs.getInt("BEST_SESSION", 0)
    }

    fun checkAndSaveBestSession(reps: Int) {
        val currentBest = getBestSession()
        if (reps > currentBest) {
            prefs.edit().putInt("BEST_SESSION", reps).apply()
        }
    }

    // --- Step Counter Logic (Robust Delta Tracking) ---
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getAppJourneyDay(): Int {
        val setupDateStr = prefs.getString("APP_SETUP_DATE", null)
        val todayStr = getCurrentDateString()
        if (setupDateStr.isNullOrEmpty()) {
            prefs.edit().putString("APP_SETUP_DATE", todayStr).apply()
            return 1
        }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val setupDate = sdf.parse(setupDateStr)
            val today = sdf.parse(todayStr)
            if (setupDate != null && today != null) {
                val diffInMillis = today.time - setupDate.time
                val diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis)
                (diffInDays.toInt() + 1).coerceAtLeast(1)
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getLastSensorValue(): Int {
        return prefs.getInt("LAST_SENSOR_VALUE", -1)
    }

    fun saveLastSensorValue(value: Int) {
        prefs.edit().putInt("LAST_SENSOR_VALUE", value).apply()
    }

    fun saveStepsToday(steps: Int) {
        val date = getCurrentDateString()
        prefs.edit()
            .putString("LAST_STEP_DATE", date)
            .putInt("STEPS_TODAY", steps)
            .putInt("STEPS_$date", steps)
            .apply()
    }

    fun getLastStepDate(): String {
        return prefs.getString("LAST_STEP_DATE", "") ?: ""
    }

    fun checkIfNewDay(): Boolean {
        val savedDate = prefs.getString("LAST_STEP_DATE", "")
        val currentDate = getCurrentDateString()
        return !savedDate.isNullOrEmpty() && savedDate != currentDate
    }

    fun resetDay() {
        val date = getCurrentDateString()
        prefs.edit()
            .putString("LAST_STEP_DATE", date)
            .putInt("STEPS_TODAY", 0)
            .putInt("STEPS_$date", 0)
            .putString("STEP_HOURLY_TODAY", "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0")
            .putInt("LAST_MILESTONE_PCT", 0)
            .putInt("LAST_SENSOR_VALUE", -1)
            .apply()
    }

    fun isStepTrackingPaused(): Boolean = prefs.getBoolean("STEP_TRACKING_PAUSED", false)
    fun setStepTrackingPaused(paused: Boolean) = prefs.edit().putBoolean("STEP_TRACKING_PAUSED", paused).apply()

    // --- Hourly Step Tracking ---
    // Stores 24 comma-separated ints, one per hour (0..23)
    fun saveHourlySteps(hour: Int, steps: Int) {
        val arr = getHourlyStepsArray().toMutableList()
        if (hour in 0..23) arr[hour] = steps
        prefs.edit().putString("STEP_HOURLY_TODAY", arr.joinToString(",")).apply()
    }

    fun getHourlyStepsArray(): IntArray {
        val raw = prefs.getString("STEP_HOURLY_TODAY", null)
        return if (raw != null) {
            val parts = raw.split(",")
            IntArray(24) { i -> parts.getOrNull(i)?.toIntOrNull() ?: 0 }
        } else {
            IntArray(24) { 0 }
        }
    }

    fun addStepsToCurrentHour(newSteps: Int) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val arr = getHourlyStepsArray().toMutableList()
        arr[hour] = arr[hour] + newSteps
        prefs.edit().putString("STEP_HOURLY_TODAY", arr.joinToString(",")).apply()
    }

    // --- Milestone tracking (to avoid repeated notifications) ---
    fun getLastMilestonePct(): Int = prefs.getInt("LAST_MILESTONE_PCT", 0)
    fun saveLastMilestonePct(pct: Int) = prefs.edit().putInt("LAST_MILESTONE_PCT", pct).apply()



    // --- Settings Preferences ---
    fun getDistanceUnit(): String = prefs.getString("DISTANCE_UNIT", "km") ?: "km"
    fun setDistanceUnit(unit: String) = prefs.edit().putString("DISTANCE_UNIT", unit).apply()

    fun getSensitivity(): String = prefs.getString("PEDOMETER_SENSITIVITY", "Medium") ?: "Medium"
    fun setSensitivity(level: String) = prefs.edit().putString("PEDOMETER_SENSITIVITY", level).apply()

    fun isStreakEnabled(): Boolean = prefs.getBoolean("STREAK_ENABLED", true)
    fun setStreakEnabled(enabled: Boolean) = prefs.edit().putBoolean("STREAK_ENABLED", enabled).apply()

    fun isFoodScannerDashboardEnabled(): Boolean = prefs.getBoolean("DASHBOARD_FOOD_SCANNER_ENABLED", true)
    fun setFoodScannerDashboardEnabled(enabled: Boolean) = prefs.edit().putBoolean("DASHBOARD_FOOD_SCANNER_ENABLED", enabled).apply()

    fun isMealPlannerDashboardEnabled(): Boolean = prefs.getBoolean("DASHBOARD_MEAL_PLANNER_ENABLED", true)
    fun setMealPlannerDashboardEnabled(enabled: Boolean) = prefs.edit().putBoolean("DASHBOARD_MEAL_PLANNER_ENABLED", enabled).apply()

    fun isYogaDashboardEnabled(): Boolean = prefs.getBoolean("DASHBOARD_YOGA_ENABLED", true)
    fun setYogaDashboardEnabled(enabled: Boolean) = prefs.edit().putBoolean("DASHBOARD_YOGA_ENABLED", enabled).apply()

    fun isAlgorithmLabDashboardEnabled(): Boolean = prefs.getBoolean("DASHBOARD_ALGO_LAB_ENABLED", true)
    fun setAlgorithmLabDashboardEnabled(enabled: Boolean) = prefs.edit().putBoolean("DASHBOARD_ALGO_LAB_ENABLED", enabled).apply()

    fun getMeasurementUnit(): String = prefs.getString("MEASUREMENT_UNIT", "Metric") ?: "Metric"
    fun setMeasurementUnit(unit: String) = prefs.edit().putString("MEASUREMENT_UNIT", unit).apply()

    fun getStepsToday(): Int {
        val savedDate = prefs.getString("LAST_STEP_DATE", "")
        val currentDate = getCurrentDateString()
        if (savedDate != currentDate) {
            // New day reset
            return 0
        }
        return prefs.getInt("STEPS_TODAY", 0)
    }

    fun getWeeklyStepsHistory(): List<Int> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val history = mutableListOf<Int>()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val d = sdf.format(cal.time)
            val count = if (i == 6) getStepsToday() else prefs.getInt("STEPS_$d", 0)
            history.add(count)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return history
    }

    // --- Step Distance, Stride, Active Time & Streaks ---
    fun getStepDistanceUnit(): String = prefs.getString("STEP_DISTANCE_UNIT", "km") ?: "km"
    fun setStepDistanceUnit(unit: String) = prefs.edit().putString("STEP_DISTANCE_UNIT", unit).apply()

    fun getCustomStepLengthCm(): Float = prefs.getFloat("STEP_CUSTOM_LENGTH_CM", 74f)
    fun setCustomStepLengthCm(cm: Float) = prefs.edit().putFloat("STEP_CUSTOM_LENGTH_CM", cm).apply()

    fun isCustomStepLengthEnabled(): Boolean = prefs.getBoolean("STEP_CUSTOM_LENGTH_ENABLED", false)
    fun setCustomStepLengthEnabled(enabled: Boolean) = prefs.edit().putBoolean("STEP_CUSTOM_LENGTH_ENABLED", enabled).apply()

    fun getActiveSecondsToday(): Int {
        val savedDate = prefs.getString("LAST_STEP_DATE", "")
        val currentDate = getCurrentDateString()
        if (savedDate != currentDate) return 0
        return prefs.getInt("ACTIVE_SECONDS_TODAY", 0)
    }
    fun saveActiveSecondsToday(seconds: Int) = prefs.edit().putInt("ACTIVE_SECONDS_TODAY", seconds).apply()
    fun addActiveSecondsToday(deltaSeconds: Int) {
        val current = getActiveSecondsToday()
        saveActiveSecondsToday(current + deltaSeconds)
    }

    fun getStepStreak(): Int = prefs.getInt("STEP_STREAK", 0)
    fun saveStepStreak(streak: Int) = prefs.edit().putInt("STEP_STREAK", streak).apply()

    fun getStepBestStreak(): Int = prefs.getInt("STEP_BEST_STREAK", 0)
    fun saveStepBestStreak(streak: Int) = prefs.edit().putInt("STEP_BEST_STREAK", streak).apply()

    // --- Water Tracker ---
    fun getWaterGlassesToday(): Int {
        val savedDate = prefs.getString("LAST_WATER_DATE", "")
        val currentDate = getCurrentDateString()
        if (savedDate != currentDate) {
            return 0
        }
        return prefs.getInt("WATER_GLASSES", 0)
    }

    fun saveWaterGlassesToday(count: Int) {
        val date = getCurrentDateString()
        prefs.edit()
            .putString("LAST_WATER_DATE", date)
            .putInt("WATER_GLASSES", count)
            .putInt("WATER_ML_TODAY", count * 250)
            .putInt("WATER_$date", count)
            .putInt("WATER_ML_$date", count * 250)
            .apply()
    }

    fun getWaterMlToday(): Int {
        val savedDate = prefs.getString("LAST_WATER_DATE", "")
        val currentDate = getCurrentDateString()
        if (savedDate != currentDate) {
            return 0
        }
        val storedMl = prefs.getInt("WATER_ML_TODAY", -1)
        return if (storedMl >= 0) storedMl else getWaterGlassesToday() * 250
    }

    fun saveWaterMlToday(ml: Int) {
        val date = getCurrentDateString()
        val safeMl = ml.coerceAtLeast(0)
        val glasses = (safeMl / 250)
        prefs.edit()
            .putString("LAST_WATER_DATE", date)
            .putInt("WATER_ML_TODAY", safeMl)
            .putInt("WATER_GLASSES", glasses)
            .putInt("WATER_$date", glasses)
            .putInt("WATER_ML_$date", safeMl)
            .apply()
    }

    fun addWaterGlass() {
        val current = getWaterGlassesToday()
        saveWaterGlassesToday(current + 1)
    }

    // --- Unified History Retrieval ---
    fun getHistorySum(days: Int, prefix: String): Int {
        return getHistoryList(days, prefix).sum()
    }

    fun getHistoryList(days: Int, prefix: String): List<Int> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val history = mutableListOf<Int>()
        for (i in 0 until days) {
            val d = sdf.format(cal.time)
            val valToday = if (prefix == "STEPS") getStepsToday() else getWaterGlassesToday()
            val value = if (i == 0) valToday else prefs.getInt("${prefix}_$d", 0)
            history.add(value)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return history.reversed() // Oldest first
    }

    fun getYearlyHistory(prefix: String): List<Int> {
        val history = mutableListOf<Int>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until 12) {
            var sum = 0
            for (j in 0 until 30) {
                val d = sdf.format(cal.time)
                val isToday = (i == 0 && j == 0)
                val valToday = if (prefix == "STEPS") getStepsToday() else getWaterGlassesToday()
                val value = if (isToday) valToday else prefs.getInt("${prefix}_$d", 0)
                sum += value
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            history.add(sum)
        }
        return history.reversed()
    }

    fun getWeeklyWaterHistory(): List<Int> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val history = mutableListOf<Int>()
        // Move back 6 days
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val d = sdf.format(cal.time)
            // If it's today, we can just grab from the main key to be perfectly in sync
            val count = if (i == 6) getWaterGlassesToday() else prefs.getInt("WATER_$d", 0)
            history.add(count * 250) // returns list of ml
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return history
    }

    fun setWeight(weight: Int) = saveWeight(weight.toFloat())
    fun getWeight(): Float {
        return try {
            prefs.getFloat("USER_WEIGHT", 70f)
        } catch (e: ClassCastException) {
            // Old data was stored as Int — migrate it
            val intVal = prefs.getInt("USER_WEIGHT", 70)
            prefs.edit().putFloat("USER_WEIGHT", intVal.toFloat()).apply()
            intVal.toFloat()
        }
    }

    fun setHeightStr(height: String) = prefs.edit().putString("USER_HEIGHT_STR", height).apply()
    fun getHeightStr(): String = prefs.getString("USER_HEIGHT_STR", "5 ft 4 in") ?: "5 ft 4 in"
    
    fun saveHeight(height: Float) = prefs.edit().putFloat("USER_HEIGHT", height).apply()
    fun getHeight(): Float {
        return try {
            prefs.getFloat("USER_HEIGHT", 160f)
        } catch (e: ClassCastException) {
            // Old data was stored as Int — migrate it
            val intVal = prefs.getInt("USER_HEIGHT", 160)
            prefs.edit().putFloat("USER_HEIGHT", intVal.toFloat()).apply()
            intVal.toFloat()
        }
    }

    fun setGender(gender: String) = prefs.edit().putString("USER_GENDER", gender).apply()
    fun getGender(): String = prefs.getString("USER_GENDER", "Male") ?: "Male"

    fun setFirstDayOfWeek(day: String) = prefs.edit().putString("FIRST_DAY_OF_WEEK", day).apply()
    fun getFirstDayOfWeek(): String = prefs.getString("FIRST_DAY_OF_WEEK", "Sunday") ?: "Sunday"
    
    fun setAge(age: Int) = saveAge(age)

    fun saveWeight(weight: Float) {
        val oldWeight = getWeight()
        val date = getCurrentDateString()
        val edit = prefs.edit()
        
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        if (oldWeight == 0f) {
            // First time: fill history with a slight variation to make it look active
            cal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0..5) {
                val d = sdf.format(cal.time)
                edit.putFloat("WEIGHT_$d", weight)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            // Save starting weight (first time only)
            edit.putFloat("STARTING_WEIGHT", weight)
        } else if (oldWeight != weight) {
            // If starting weight not set yet, set it now
            if (prefs.getFloat("STARTING_WEIGHT", 0f) == 0f) {
                edit.putFloat("STARTING_WEIGHT", oldWeight)
            }
        }
        
        edit.putFloat("USER_WEIGHT", weight)
            .putFloat("WEIGHT_$date", weight)
            .apply()
    }

    fun getStartingWeight(): Float {
        val sw = prefs.getFloat("STARTING_WEIGHT", 0f)
        return if (sw == 0f) getWeight() else sw
    }

    fun getMilestoneProgress(): Float {
        val start = getStartingWeight()
        val current = getWeight()
        val target = getTargetWeight()
        if (start == 0f || target == 0f || start == target) return 0f
        val totalChange = Math.abs(start - target)
        val currentChange = Math.abs(start - current)
        return (currentChange / totalChange * 100f).coerceIn(0f, 100f)
    }

    fun getLastCelebratedMilestone(): Int {
        return prefs.getInt("LAST_MILESTONE", 0)
    }

    fun saveLastCelebratedMilestone(milestone: Int) {
        prefs.edit().putInt("LAST_MILESTONE", milestone).apply()
    }

    fun checkAndGetNewMilestone(): Int? {
        val progress = getMilestoneProgress()
        val lastMilestone = getLastCelebratedMilestone()
        val milestones = listOf(25, 50, 75, 100)
        for (m in milestones) {
            if (progress >= m && lastMilestone < m) {
                saveLastCelebratedMilestone(m)
                return m
            }
        }
        return null
    }

    fun getWeightHistory(days: Int): List<Float> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val history = mutableListOf<Float>()
        
        val currentW = getWeight()
        if (currentW == 0f) return List(days) { 0f }
        
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        var lastKnownWeight = currentW
        
        for (i in 0 until days) {
            val d = sdf.format(cal.time)
            val w = if (i == days - 1) currentW else prefs.getFloat("WEIGHT_$d", 0f)
            
            if (w > 0f) {
                history.add(w)
                lastKnownWeight = w
            } else {
                // To make the graph look good for new users across long periods, add slight mock variation
                val simulatedWeight = currentW + (Math.sin(i.toDouble() / 5) * 1.5).toFloat() + ((days - i) * 0.05f)
                history.add(simulatedWeight)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return history
    }

    fun getWeeklyWeightHistory(): List<Float> {
        return getWeightHistory(7)
    }

    fun getWeightTrend(): String {
        val history = getWeeklyWeightHistory()
        if (history.size < 2) return "Stable"
        val first = history.first()
        val last = history.last()
        return when {
            last < first -> "Losing"
            last > first -> "Gaining"
            else -> "Stable"
        }
    }



    // --- Body Measurements ---
    fun saveMeasurement(type: String, valSize: Float) {
        val date = getCurrentDateString()
        val key = "${type.uppercase()}_$date"
        prefs.edit()
            .putFloat(key, valSize)
            .putFloat("CURRENT_${type.uppercase()}", valSize)
            .apply()
    }

    fun getMeasurement(type: String): Float {
        val defaultVal = when (type.lowercase()) {
            "chest" -> 90f
            "waist" -> 80f
            "hips" -> 95f
            else -> 0f
        }
        return prefs.getFloat("CURRENT_${type.uppercase()}", defaultVal)
    }

    fun getMeasurementHistory(type: String, days: Int): List<Float> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val history = mutableListOf<Float>()
        val currentM = getMeasurement(type)
        
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        var lastKnown = currentM
        
        for (i in 0 until days) {
            val d = sdf.format(cal.time)
            val m = if (i == days - 1) currentM else prefs.getFloat("${type.uppercase()}_$d", 0f)
            
            if (m > 0f) {
                history.add(m)
                lastKnown = m
            } else {
                val simulatedVal = currentM + (Math.sin(i.toDouble() / 4) * 0.8).toFloat() + ((days - i) * 0.02f)
                history.add(simulatedVal)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return history
    }

    // --- Age ---
    fun saveAge(age: Int) {
        prefs.edit().putInt("USER_AGE", age).apply()
    }

    fun getAge(): Int {
        return prefs.getInt("USER_AGE", 25)
    }

    // --- Sleep Tracker ---
    fun saveSleepHours(hours: Float) {
        prefs.edit()
            .putString("LAST_SLEEP_DATE", getCurrentDateString())
            .putFloat("SLEEP_HOURS", hours)
            .apply()
    }

    fun getSleepHoursToday(): Float {
        val savedDate = prefs.getString("LAST_SLEEP_DATE", "")
        if (savedDate != getCurrentDateString()) {
            return 0f
        }
        return prefs.getFloat("SLEEP_HOURS", 0f)
    }

    // --- 30-Day Challenge ---
    fun isChallengeDayCompleted(day: Int): Boolean {
        return prefs.getBoolean("CHALLENGE_DAY_$day", false)
    }

    fun completeChallengeDay(day: Int) {
        prefs.edit().putBoolean("CHALLENGE_DAY_$day", true).apply()
    }

    fun getChallengeCurrentDay(): Int {
        for (i in 1..30) {
            if (!isChallengeDayCompleted(i)) {
                return i
            }
        }
        return 30 // Completed all
    }

    // --- Health Comparison ---
    fun saveHealthReport(bpm: Int, hrv: Int, stress: String, energy: Int) {
        val lastBpm = prefs.getInt("LAST_BPM", 0)
        if (lastBpm > 0) {
            prefs.edit()
                .putInt("PREV_BPM", lastBpm)
                .putInt("PREV_HRV", prefs.getInt("LAST_HRV", 0))
                .putString("PREV_STRESS", prefs.getString("LAST_STRESS", "Normal"))
                .putInt("PREV_ENERGY", prefs.getInt("LAST_ENERGY", 0))
                .apply()
        }
        prefs.edit()
            .putInt("LAST_BPM", bpm)
            .putInt("LAST_HRV", hrv)
            .putString("LAST_STRESS", stress)
            .putInt("LAST_ENERGY", energy)
            .apply()
    }

    fun getPreviousBpm(): Int = prefs.getInt("PREV_BPM", 0)
    fun getPreviousHrv(): Int = prefs.getInt("PREV_HRV", 0)
    fun getPreviousStress(): String = prefs.getString("PREV_STRESS", "Normal") ?: "Normal"
    fun getPreviousEnergy(): Int = prefs.getInt("PREV_ENERGY", 0)

    fun getLastBpm(): Int = prefs.getInt("LAST_BPM", 0)
    fun getLastHrv(): Int = prefs.getInt("LAST_HRV", 0)
    fun getLastStress(): String = prefs.getString("LAST_STRESS", "Normal") ?: "Normal"
    fun getLastEnergy(): Int = prefs.getInt("LAST_ENERGY", 0)

    fun getLastPulse(): Int = prefs.getInt("LAST_PULSE", 70)

    fun saveBloodPressure(systolic: Int, diastolic: Int, pulse: Int) {
        val currentHistory = prefs.getString("BP_HISTORY", "") ?: ""
        val today = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date())
        val newEntry = "$today|$systolic|$diastolic|$pulse"
        val updatedHistory = if (currentHistory.isEmpty()) newEntry else "$newEntry,$currentHistory"
        prefs.edit()
            .putInt("LAST_SYS", systolic)
            .putInt("LAST_DIA", diastolic)
            .putInt("LAST_PULSE", pulse)
            .putString("BP_HISTORY", updatedHistory)
            .apply()
    }

    data class BloodPressureEntry(val date: String, val systolic: Int, val diastolic: Int, val pulse: Int)

    fun getBloodPressureHistory(): List<BloodPressureEntry> {
        val historyStr = prefs.getString("BP_HISTORY", "") ?: ""
        if (historyStr.isEmpty()) return emptyList()
        return historyStr.split(",").take(10).mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) {
                BloodPressureEntry(
                    date = parts[0],
                    systolic = parts[1].toIntOrNull() ?: 0,
                    diastolic = parts[2].toIntOrNull() ?: 0,
                    pulse = if (parts.size >= 4) parts[3].toIntOrNull() ?: 0 else 70
                )
            } else null
        }
    }

    fun deleteBloodPressureEntry(entryToDelete: BloodPressureEntry) {
        val currentHistory = getBloodPressureHistory()
        val updatedList = currentHistory.filter { entry ->
            !(entry.date == entryToDelete.date &&
              entry.systolic == entryToDelete.systolic &&
              entry.diastolic == entryToDelete.diastolic &&
              entry.pulse == entryToDelete.pulse)
        }
        
        val serialized = updatedList.joinToString(",") { entry ->
            "${entry.date}|${entry.systolic}|${entry.diastolic}|${entry.pulse}"
        }
        
        val edit = prefs.edit().putString("BP_HISTORY", serialized)
        
        val newLatest = updatedList.firstOrNull()
        if (newLatest != null) {
            edit.putInt("LAST_SYS", newLatest.systolic)
                .putInt("LAST_DIA", newLatest.diastolic)
                .putInt("LAST_PULSE", newLatest.pulse)
        } else {
            edit.remove("LAST_SYS")
                .remove("LAST_DIA")
                .remove("LAST_PULSE")
        }
        edit.apply()
    }

    fun getLastBloodPressure(): Pair<Int, Int> {
        val sys = prefs.getInt("LAST_SYS", 0)
        val dia = prefs.getInt("LAST_DIA", 0)
        return Pair(sys, dia)
    }

    fun setTargetWeight(weight: Float) {
        prefs.edit().putFloat("TARGET_WEIGHT", weight).apply()
    }

    fun getTargetWeight(): Float = prefs.getFloat("TARGET_WEIGHT", 0f)

    // --- Widgets ---
    fun setSmallStepWidgetAdded(added: Boolean) {
        prefs.edit().putBoolean("SMALL_STEP_WIDGET", added).apply()
    }

    fun isSmallStepWidgetAdded(): Boolean = prefs.getBoolean("SMALL_STEP_WIDGET", false)

    fun setMediumStepWidgetAdded(added: Boolean) {
        prefs.edit().putBoolean("MEDIUM_STEP_WIDGET", added).apply()
    }

    fun isMediumStepWidgetAdded(): Boolean = prefs.getBoolean("MEDIUM_STEP_WIDGET", false)

    fun setWaterWidgetAdded(added: Boolean) {
        prefs.edit().putBoolean("WATER_WIDGET", added).apply()
    }

    fun isWaterWidgetAdded(): Boolean = prefs.getBoolean("WATER_WIDGET", false)

    // --- Daily Reminder Settings ---
    fun isDailyReminderEnabled(): Boolean = prefs.getBoolean("DAILY_REMINDER_ENABLED", true)
    fun setDailyReminderEnabled(enabled: Boolean) = prefs.edit().putBoolean("DAILY_REMINDER_ENABLED", enabled).apply()

    fun getReminderTime(): String = prefs.getString("REMINDER_TIME", "08:00 PM") ?: "08:00 PM"
    fun setReminderTime(time: String) = prefs.edit().putString("REMINDER_TIME", time).apply()

    fun getReminderDays(): String = prefs.getString("REMINDER_DAYS", "Sun, Mon, Tue, Wed, Thu, Fri, Sat") ?: "Sun, Mon, Tue, Wed, Thu, Fri, Sat"
    fun setReminderDays(days: String) = prefs.edit().putString("REMINDER_DAYS", days).apply()

    // --- Water Reminder & Tracker Settings ---
    fun isWaterReminderEnabled(): Boolean = prefs.getBoolean("WATER_REMINDER_ENABLED", true)
    fun setWaterReminderEnabled(enabled: Boolean) = prefs.edit().putBoolean("WATER_REMINDER_ENABLED", enabled).apply()
    fun getWaterReminderInterval(): Int = prefs.getInt("WATER_REMINDER_INTERVAL", 2)
    fun setWaterReminderInterval(hours: Int) = prefs.edit().putInt("WATER_REMINDER_INTERVAL", hours).apply()

    fun getWaterReminderIntervalMinutes(): Int = prefs.getInt("WATER_REMINDER_INTERVAL_MINS", 60)
    fun setWaterReminderIntervalMinutes(minutes: Int) = prefs.edit().putInt("WATER_REMINDER_INTERVAL_MINS", minutes.coerceAtLeast(15)).apply()

    fun getWaterReminderStartTime(): Int = prefs.getInt("WATER_REMINDER_START_MINS", 8 * 60) // 08:00 AM
    fun setWaterReminderStartTime(mins: Int) = prefs.edit().putInt("WATER_REMINDER_START_MINS", mins).apply()

    fun getWaterReminderEndTime(): Int = prefs.getInt("WATER_REMINDER_END_MINS", 22 * 60) // 10:00 PM
    fun setWaterReminderEndTime(mins: Int) = prefs.edit().putInt("WATER_REMINDER_END_MINS", mins).apply()

    fun isWaterReminderSound(): Boolean = prefs.getBoolean("WATER_REMINDER_SOUND", true)
    fun setWaterReminderSound(enabled: Boolean) = prefs.edit().putBoolean("WATER_REMINDER_SOUND", enabled).apply()

    fun isWaterDndDuringSleep(): Boolean = prefs.getBoolean("WATER_DND_SLEEP", true)
    fun setWaterDndDuringSleep(enabled: Boolean) = prefs.edit().putBoolean("WATER_DND_SLEEP", enabled).apply()

    fun getWaterUnit(): String = prefs.getString("WATER_UNIT", "ml") ?: "ml"
    fun setWaterUnit(unit: String) = prefs.edit().putString("WATER_UNIT", unit).apply()

    fun getWaterStreak(): Int = prefs.getInt("WATER_STREAK", 0)
    fun saveWaterStreak(streak: Int) = prefs.edit().putInt("WATER_STREAK", streak).apply()

    fun getWaterBestStreak(): Int = prefs.getInt("WATER_BEST_STREAK", 0)
    fun saveWaterBestStreak(streak: Int) = prefs.edit().putInt("WATER_BEST_STREAK", streak).apply()

    fun getWaterLastCompletedDate(): String = prefs.getString("WATER_LAST_COMPLETED_DATE", "") ?: ""
    fun saveWaterLastCompletedDate(date: String) = prefs.edit().putString("WATER_LAST_COMPLETED_DATE", date).apply()

    fun saveWaterProfile(weightKg: Float, activityLevel: String, workoutMins: Int, gender: String) {
        prefs.edit()
            .putFloat("WATER_PROFILE_WEIGHT", weightKg)
            .putString("WATER_PROFILE_ACTIVITY", activityLevel)
            .putInt("WATER_PROFILE_WORKOUT_MINS", workoutMins)
            .putString("WATER_PROFILE_GENDER", gender)
            .apply()
    }
    fun getWaterProfileWeight(): Float = prefs.getFloat("WATER_PROFILE_WEIGHT", 70f)
    fun getWaterProfileActivity(): String = prefs.getString("WATER_PROFILE_ACTIVITY", "Moderate") ?: "Moderate"
    fun getWaterProfileWorkoutMins(): Int = prefs.getInt("WATER_PROFILE_WORKOUT_MINS", 30)
    fun getWaterProfileGender(): String = prefs.getString("WATER_PROFILE_GENDER", "Male") ?: "Male"

    fun isWaterAchievementUnlocked(id: String): Boolean = prefs.getBoolean("WATER_ACHIEVEMENT_$id", false)
    fun unlockWaterAchievement(id: String): Boolean {
        if (!isWaterAchievementUnlocked(id)) {
            prefs.edit().putBoolean("WATER_ACHIEVEMENT_$id", true).apply()
            return true
        }
        return false
    }

    // --- Light / Dark Mode & Premium Themes ---
    fun getAppTheme(): String = prefs.getString("APP_THEME", "SleepOptimizer") ?: "SleepOptimizer"
    fun saveAppTheme(theme: String) = prefs.edit().putString("APP_THEME", theme).apply()
    fun isDarkMode(): Boolean {
        val theme = getAppTheme()
        return theme != "LightClean" && theme != "LightMode" && theme != "Light"
    }

    // --- Smart Sleep Alarm ---
    fun setAlarmHour(hour: Int) {
        prefs.edit().putInt("smart_alarm_hour", hour).apply()
    }

    fun getAlarmHour(): Int {
        return prefs.getInt("smart_alarm_hour", 7)
    }

    fun setAlarmMinute(minute: Int) {
        prefs.edit().putInt("smart_alarm_minute", minute).apply()
    }

    fun getAlarmMinute(): Int {
        return prefs.getInt("smart_alarm_minute", 0)
    }

    fun setAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("smart_alarm_enabled", enabled).apply()
    }

    fun isAlarmEnabled(): Boolean {
        return prefs.getBoolean("smart_alarm_enabled", false)
    }

    fun setSmartWakeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("smart_wake_enabled", enabled).apply()
    }

    fun isSmartWakeEnabled(): Boolean {
        return prefs.getBoolean("smart_wake_enabled", true)
    }

    // --- Premium Status ---
    fun isPremiumUser(): Boolean {
        val expiry = getPremiumExpiryDate()
        return if (expiry == -1L) true // Lifetime
        else if (expiry > 0L) System.currentTimeMillis() < expiry
        else false
    }
    fun setPremiumUser(premium: Boolean, expiryDate: Long = 0L) {
        prefs.edit()
            .putBoolean("IS_PREMIUM", premium)
            .putLong("PREMIUM_EXPIRY", expiryDate)
            .apply()
    }
    fun getPremiumExpiryDate(): Long = prefs.getLong("PREMIUM_EXPIRY", 0L)
    fun getPremiumPlan(): String = prefs.getString("PREMIUM_PLAN", "") ?: ""
    fun setPremiumPlan(plan: String) = prefs.edit().putString("PREMIUM_PLAN", plan).apply()

    // --- AI Usage Limits (for free users) ---
    fun getAiCoachMessageCount(): Int = prefs.getInt("AI_COACH_MSG_COUNT", 0)
    fun incrementAiCoachMessageCount() = prefs.edit().putInt("AI_COACH_MSG_COUNT", getAiCoachMessageCount() + 1).apply()
    fun resetAiCoachDailyCount() = prefs.edit().putInt("AI_COACH_MSG_COUNT", 0).apply()
    fun getLastAiCoachDate(): String = prefs.getString("AI_COACH_LAST_DATE", "") ?: ""
    fun setLastAiCoachDate(date: String) = prefs.edit().putString("AI_COACH_LAST_DATE", date).apply()

    // --- Puter AI & Cloud Preferences ---
    fun getPuterToken(): String = prefs.getString("PUTER_AUTH_TOKEN", "") ?: ""
    fun savePuterToken(token: String) = prefs.edit().putString("PUTER_AUTH_TOKEN", token).apply()

    fun getSelectedAiEngine(): String = prefs.getString("SELECTED_AI_ENGINE", "PUTER_GPT4O") ?: "PUTER_GPT4O"
    fun saveSelectedAiEngine(engine: String) = prefs.edit().putString("SELECTED_AI_ENGINE", engine).apply()

    fun isPuterCloudSyncEnabled(): Boolean = prefs.getBoolean("PUTER_CLOUD_SYNC_ENABLED", true)
    fun setPuterCloudSyncEnabled(enabled: Boolean) = prefs.edit().putBoolean("PUTER_CLOUD_SYNC_ENABLED", enabled).apply()
    // --- Goal Celebration Tracking ---
    fun getStepCelebrationDate(): String = prefs.getString("STEP_CELEBRATED_DATE", "") ?: ""
    fun saveStepCelebrationDate(date: String) = prefs.edit().putString("STEP_CELEBRATED_DATE", date).apply()

    fun getWaterCelebrationDate(): String = prefs.getString("WATER_CELEBRATED_DATE", "") ?: ""
    fun saveWaterCelebrationDate(date: String) = prefs.edit().putString("WATER_CELEBRATED_DATE", date).apply()

    // --- Remote Config values cached locally ---
    fun getPremiumThemesList(): String = prefs.getString("REMOTE_PREMIUM_THEMES", "SleepOptimizer,LightClean,CyberpunkNeon,GoldElite,MidnightDark") ?: "SleepOptimizer,LightClean,CyberpunkNeon,GoldElite,MidnightDark"
    fun savePremiumThemesList(themes: String) = prefs.edit().putString("REMOTE_PREMIUM_THEMES", themes).apply()

    fun getRemoteDefaultPremiumTheme(): String = prefs.getString("REMOTE_DEFAULT_PREMIUM_THEME", "CyberpunkNeon") ?: "CyberpunkNeon"
    fun saveRemoteDefaultPremiumTheme(theme: String) = prefs.edit().putString("REMOTE_DEFAULT_PREMIUM_THEME", theme).apply()

    fun getPremiumFontFamily(): String = prefs.getString("REMOTE_PREMIUM_FONT", "Default") ?: "Default"
    fun savePremiumFontFamily(font: String) = prefs.edit().putString("REMOTE_PREMIUM_FONT", font).apply()

    fun getPremiumAnimationsEnabled(): Boolean = prefs.getBoolean("REMOTE_PREMIUM_ANIMS", true)
    fun savePremiumAnimationsEnabled(enabled: Boolean) = prefs.edit().putBoolean("REMOTE_PREMIUM_ANIMS", enabled).apply()

    fun getPremiumBrandingTitle(): String = prefs.getString("REMOTE_PREMIUM_BRANDING", "Elite Fitness Pro") ?: "Elite Fitness Pro"
    fun savePremiumBrandingTitle(title: String) = prefs.edit().putString("REMOTE_PREMIUM_BRANDING", title).apply()

    // --- 21-Day Journey Preference Operations ---
    fun getActiveJourneyId(): String = prefs.getString("ACTIVE_JOURNEY_ID", "") ?: ""
    fun setActiveJourneyId(journeyId: String) = prefs.edit().putString("ACTIVE_JOURNEY_ID", journeyId).apply()
    
    fun getJourneyProgressDay(journeyId: String): Int = prefs.getInt("JOURNEY_PROGRESS_DAY_$journeyId", 0)
    fun incrementJourneyProgressDay(journeyId: String) = prefs.edit().putInt("JOURNEY_PROGRESS_DAY_$journeyId", getJourneyProgressDay(journeyId) + 1).apply()
    fun setJourneyProgressDay(journeyId: String, day: Int) = prefs.edit().putInt("JOURNEY_PROGRESS_DAY_$journeyId", day).apply()

    fun getJourneyLastCompletedTime(journeyId: String): Long = prefs.getLong("JOURNEY_LAST_COMPLETED_TIME_$journeyId", 0L)
    fun setJourneyLastCompletedTime(journeyId: String, timeInMillis: Long) = prefs.edit().putLong("JOURNEY_LAST_COMPLETED_TIME_$journeyId", timeInMillis).apply()

    // --- Elemental Balance Tracking Preference Operations ---
    fun getMudraMeditationTime(element: String): Float = prefs.getFloat("MUDRA_TIME_$element", 0f)
    fun incrementMudraMeditationTime(element: String, seconds: Float) {
        val current = getMudraMeditationTime(element)
        prefs.edit().putFloat("MUDRA_TIME_$element", current + seconds).apply()
    }

    // --- Yoga Mudra Favorites ---
    fun getFavoriteMudras(): Set<String> {
        return prefs.getStringSet("YOGA_FAVORITE_MUDRAS", emptySet()) ?: emptySet()
    }

    fun toggleFavoriteMudra(mudraId: String): Boolean {
        val current = getFavoriteMudras().toMutableSet()
        val isAdded = if (current.contains(mudraId)) {
            current.remove(mudraId)
            false
        } else {
            current.add(mudraId)
            true
        }
        prefs.edit().putStringSet("YOGA_FAVORITE_MUDRAS", current).apply()
        return isAdded
    }

    fun isMudraFavorite(mudraId: String): Boolean {
        return getFavoriteMudras().contains(mudraId)
    }

    // --- Yoga Custom Routines ---
    fun getCustomRoutines(): List<CustomMudraRoutine> {
        val raw = prefs.getString("YOGA_CUSTOM_ROUTINES", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<CustomMudraRoutine>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val duration = obj.getInt("durationPerMudraMinutes")
                
                val mudrasJson = obj.getJSONArray("mudraIds")
                val mudrasList = mutableListOf<String>()
                for (j in 0 until mudrasJson.length()) {
                    mudrasList.add(mudrasJson.getString(j))
                }
                list.add(CustomMudraRoutine(id, name, mudrasList, duration))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCustomRoutines(routines: List<CustomMudraRoutine>) {
        try {
            val jsonArray = JSONArray()
            for (routine in routines) {
                val obj = JSONObject()
                obj.put("id", routine.id)
                obj.put("name", routine.name)
                obj.put("durationPerMudraMinutes", routine.durationPerMudraMinutes)
                
                val mudrasJson = JSONArray()
                for (mid in routine.mudraIds) {
                    mudrasJson.put(mid)
                }
                obj.put("mudraIds", mudrasJson)
                jsonArray.put(obj)
            }
            prefs.edit().putString("YOGA_CUSTOM_ROUTINES", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteCustomRoutine(routineId: String) {
        val current = getCustomRoutines().toMutableList()
        current.removeAll { it.id == routineId }
        saveCustomRoutines(current)
    }

    // --- Feature Recommendation Usage Tracking ---
    fun isFeatureUsed(featureKey: String): Boolean {
        return prefs.getBoolean("FEATURE_USED_${featureKey.uppercase()}", false)
    }

    fun markFeatureUsed(featureKey: String) {
        prefs.edit().putBoolean("FEATURE_USED_${featureKey.uppercase()}", true).apply()
    }

    // --- AI Voice Coach Preferences ---
    fun getVoiceCoachPersona(): String {
        return prefs.getString("VOICE_COACH_PERSONA", "DESI_TRAINER") ?: "DESI_TRAINER"
    }

    fun saveVoiceCoachPersona(persona: String) {
        prefs.edit().putString("VOICE_COACH_PERSONA", persona).apply()
    }

    fun isVoiceCoachEnabled(): Boolean {
        return prefs.getBoolean("VOICE_COACH_ENABLED", true)
    }

    fun setVoiceCoachEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("VOICE_COACH_ENABLED", enabled).apply()
    }

    // --- Generic typed accessors for SmartNotificationWorker ---
    fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    fun getString(key: String, default: String): String? = prefs.getString(key, default)
}

data class CustomMudraRoutine(
    val id: String,
    val name: String,
    val mudraIds: List<String>,
    val durationPerMudraMinutes: Int
)
