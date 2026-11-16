package com.pro.aifitnesstracker

import kotlinx.coroutines.flow.Flow

class HealthRepository(
    private val healthDao: HealthDao,
    private val prefsManager: SharedPrefsManager
) {
    fun getHealthDao(): HealthDao = healthDao

    // Heart Rate
    suspend fun saveHeartRate(bpm: Int, hrv: Int, stress: String, energy: Int) {
        val record = HeartRateRecord(bpm = bpm, hrv = hrv, stress = stress, energy = energy)
        val id = healthDao.insertHeartRate(record)
        prefsManager.saveHealthReport(bpm, hrv, stress, energy)
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleHeartRate(currentUser.uid, record.copy(id = id.toInt()))
        }
    }

    fun getHeartRateHistory(): Flow<List<HeartRateRecord>> = healthDao.getAllHeartRateRecords()

    // Blood Pressure
    suspend fun saveBloodPressure(systolic: Int, diastolic: Int, pulse: Int) {
        val record = BloodPressureRecord(systolic = systolic, diastolic = diastolic, pulse = pulse)
        val id = healthDao.insertBloodPressure(record)
        prefsManager.saveBloodPressure(systolic, diastolic, pulse)
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleBP(currentUser.uid, record.copy(id = id.toInt()))
        }
    }

    suspend fun deleteBloodPressure(systolic: Int, diastolic: Int, pulse: Int) {
        healthDao.deleteBloodPressureByValues(systolic, diastolic, pulse)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.deleteSingleBP(currentUser.uid, systolic, diastolic, pulse)
        }
    }

    fun getBPHistory(): Flow<List<BloodPressureRecord>> = healthDao.getAllBPRecords()

    // Steps
    suspend fun updateSteps(date: String, steps: Int) {
        healthDao.insertStepRecord(StepRecord(date, steps))
        prefsManager.saveStepsToday(steps)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleStep(currentUser.uid, date, steps)
        }
    }

    fun getStepHistory(): Flow<List<StepRecord>> = healthDao.getStepHistory()

    // Water
    suspend fun updateWater(date: String, glasses: Int) {
        healthDao.insertWaterRecord(WaterRecord(date, glasses))
        prefsManager.saveWaterGlassesToday(glasses)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleWater(currentUser.uid, date, glasses)
        }
    }

    // --- MEAL LOGGING ---
    suspend fun addMeal(meal: MealRecord) {
        val id = healthDao.insertMeal(meal)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleMeal(currentUser.uid, meal.copy(id = id.toInt()))
        }
    }

    fun getMealsByDate(date: String): Flow<List<MealRecord>> {
        return healthDao.getMealsByDate(date)
    }

    suspend fun deleteMeal(meal: MealRecord) {
        healthDao.deleteMeal(meal)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.deleteSingleMeal(currentUser.uid, meal.id.toString())
        }
    }

    fun getMealsForDateRange(startDate: String, endDate: String): Flow<List<MealRecord>> {
        return healthDao.getMealsForDateRange(startDate, endDate)
    }

    // --- SLEEP ---
    suspend fun addSleepRecord(record: SleepRecord) {
        val id = healthDao.insertSleepRecord(record)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleSleep(currentUser.uid, record.copy(id = id.toInt()))
        }
    }

    fun getSleepHistory(): Flow<List<SleepRecord>> {
        return healthDao.getSleepHistory()
    }

    suspend fun deleteSleepRecord(record: SleepRecord) {
        healthDao.deleteSleepRecord(record)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.deleteSingleSleep(currentUser.uid, record.id.toString())
        }
    }

    // --- YOGA ---
    suspend fun saveYogaSession(pose: String, duration: Int, accuracy: Int) {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val record = YogaRecord(date = date, poseName = pose, durationSeconds = duration, accuracy = accuracy)
        val id = healthDao.insertYogaRecord(record)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleYoga(currentUser.uid, record.copy(id = id.toInt()))
        }
    }

    fun getYogaHistory(): Flow<List<YogaRecord>> = healthDao.getYogaHistory()

    // --- WORKOUT ---
    suspend fun saveWorkout(type: String, value: Int, calories: Int) {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val record = WorkoutRecord(date = date, workoutType = type, repsOrMinutes = value, caloriesBurned = calories)
        val id = healthDao.insertWorkoutRecord(record)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleWorkout(currentUser.uid, record.copy(id = id.toInt()))
        }
    }

    fun getWorkoutHistory(): Flow<List<WorkoutRecord>> = healthDao.getWorkoutHistory()

    fun getWorkoutRecordsForDateRange(startDate: String, endDate: String): Flow<List<WorkoutRecord>> {
        return healthDao.getWorkoutRecordsForDateRange(startDate, endDate)
    }

    // --- WORKOUT PLANS ---
    suspend fun addWorkoutPlan(plan: WorkoutPlanRecord) {
        healthDao.insertWorkoutPlan(plan)
    }

    fun getWorkoutPlansByDate(date: String): Flow<List<WorkoutPlanRecord>> {
        return healthDao.getWorkoutPlansByDate(date)
    }

    suspend fun deleteWorkoutPlan(plan: WorkoutPlanRecord) {
        healthDao.deleteWorkoutPlan(plan)
    }

    // --- AI CHAT HISTORY ---
    fun getChatHistory(): Flow<List<AIChatRecord>> = healthDao.getAllChatRecordsFlow()

    suspend fun saveChatMessage(text: String, isUser: Boolean) {
        val record = AIChatRecord(text = text, isUser = isUser)
        val id = healthDao.insertChatRecord(record)
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseSyncManager.syncSingleChatRecord(currentUser.uid, record.copy(id = id.toInt()))
        }
    }
}

