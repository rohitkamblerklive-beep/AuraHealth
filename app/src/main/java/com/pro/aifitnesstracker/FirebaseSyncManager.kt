package com.pro.aifitnesstracker

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FirebaseSyncManager {
    private val db: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference

    fun syncProfileToCloud(
        userId: String,
        age: Int,
        gender: String,
        height: Int,
        weight: Int,
        goal: String,
        activityLevel: String,
        name: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val profile = mapOf(
            "age" to age,
            "gender" to gender,
            "height" to height,
            "weight" to weight,
            "goal" to goal,
            "activityLevel" to activityLevel,
            "name" to name,
            "updatedAt" to System.currentTimeMillis()
        )

        db.child("users")
            .child(userId)
            .updateChildren(profile)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun fetchProfileFromCloud(
        userId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.child("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val data = snapshot.value as? Map<String, Any>
                    onSuccess(data)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun syncFcmTokenToCloud(userId: String, token: String, onComplete: (Boolean) -> Unit = {}) {
        if (userId.isBlank() || token.isBlank()) {
            onComplete(false)
            return
        }
        db.child("users")
            .child(userId)
            .child("fcmToken")
            .setValue(token)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // --- SINGLE SYNC METHODS ---

    fun syncSingleStep(userId: String, date: String, steps: Int) {
        db.child("users").child(userId).child("step_records")
            .child(date).setValue(mapOf("steps" to steps))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleStep succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleStep failed: ${e.message}") }
    }

    fun syncSingleWater(userId: String, date: String, glasses: Int) {
        db.child("users").child(userId).child("water_records")
            .child(date).setValue(mapOf("glasses" to glasses))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleWater succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleWater failed: ${e.message}") }
    }

    fun syncSingleChatRecord(userId: String, record: AIChatRecord) {
        db.child("users").child(userId).child("ai_chat_records")
            .child(record.id.toString()).setValue(mapOf(
                "text" to record.text,
                "isUser" to record.isUser,
                "timestamp" to record.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleChatRecord succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleChatRecord failed: ${e.message}") }
    }

    fun syncSingleMeal(userId: String, meal: MealRecord) {
        db.child("users").child(userId).child("meal_records")
            .child(meal.id.toString()).setValue(mapOf(
                "date" to meal.date,
                "mealType" to meal.mealType,
                "foodName" to meal.foodName,
                "calories" to meal.calories,
                "protein" to meal.protein,
                "carbs" to meal.carbs,
                "fats" to meal.fats,
                "timestamp" to meal.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleMeal succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleMeal failed: ${e.message}") }
    }

    fun deleteSingleMeal(userId: String, mealId: String) {
        db.child("users").child(userId).child("meal_records")
            .child(mealId).removeValue()
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "deleteSingleMeal succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "deleteSingleMeal failed: ${e.message}") }
    }

    fun syncSingleSleep(userId: String, record: SleepRecord) {
        db.child("users").child(userId).child("sleep_records")
            .child(record.id.toString()).setValue(mapOf(
                "date" to record.date,
                "bedTime" to record.bedTime,
                "wakeTime" to record.wakeTime,
                "durationHours" to record.durationHours,
                "quality" to record.quality
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleSleep succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleSleep failed: ${e.message}") }
    }

    fun deleteSingleSleep(userId: String, sleepId: String) {
        db.child("users").child(userId).child("sleep_records")
            .child(sleepId).removeValue()
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "deleteSingleSleep succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "deleteSingleSleep failed: ${e.message}") }
    }

    fun syncSingleWorkout(userId: String, record: WorkoutRecord) {
        db.child("users").child(userId).child("workout_records")
            .child(record.id.toString()).setValue(mapOf(
                "date" to record.date,
                "workoutType" to record.workoutType,
                "repsOrMinutes" to record.repsOrMinutes,
                "caloriesBurned" to record.caloriesBurned,
                "timestamp" to record.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleWorkout succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleWorkout failed: ${e.message}") }
    }

    fun syncSingleYoga(userId: String, record: YogaRecord) {
        db.child("users").child(userId).child("yoga_records")
            .child(record.id.toString()).setValue(mapOf(
                "date" to record.date,
                "poseName" to record.poseName,
                "durationSeconds" to record.durationSeconds,
                "accuracy" to record.accuracy,
                "timestamp" to record.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleYoga succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleYoga failed: ${e.message}") }
    }

    fun syncSingleHeartRate(userId: String, record: HeartRateRecord) {
        db.child("users").child(userId).child("heart_rate_records")
            .child(record.id.toString()).setValue(mapOf(
                "bpm" to record.bpm,
                "hrv" to record.hrv,
                "stress" to record.stress,
                "energy" to record.energy,
                "timestamp" to record.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleHeartRate succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleHeartRate failed: ${e.message}") }
    }

    fun syncSingleBP(userId: String, record: BloodPressureRecord) {
        db.child("users").child(userId).child("blood_pressure_records")
            .child(record.id.toString()).setValue(mapOf(
                "systolic" to record.systolic,
                "diastolic" to record.diastolic,
                "pulse" to record.pulse,
                "timestamp" to record.timestamp
            ))
            .addOnSuccessListener { Log.d("SYNC_MANAGER", "syncSingleBP succeeded") }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "syncSingleBP failed: ${e.message}") }
    }

    fun deleteSingleBP(userId: String, systolic: Int, diastolic: Int, pulse: Int) {
        db.child("users").child(userId).child("blood_pressure_records").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.children) {
                    val sysVal = doc.child("systolic").value as? Long
                    val diaVal = doc.child("diastolic").value as? Long
                    val pulVal = doc.child("pulse").value as? Long
                    if (sysVal?.toInt() == systolic && diaVal?.toInt() == diastolic && pulVal?.toInt() == pulse) {
                        doc.ref.removeValue()
                            .addOnSuccessListener { Log.d("SYNC_MANAGER", "deleteSingleBP succeeded") }
                            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "deleteSingleBP failed: ${e.message}") }
                    }
                }
            }
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "deleteSingleBP get failed: ${e.message}") }
    }

    // --- FULL BACKUP UPLOAD ---

    fun uploadFullBackup(userId: String, appDatabase: AppDatabase) {
        val dao = appDatabase.healthDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Steps
                dao.getAllStepRecords().forEach { s ->
                    db.child("users").child(userId).child("step_records")
                        .child(s.date).setValue(mapOf("steps" to s.steps))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Steps succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Steps failed: ${e.message}") }
                }

                // Water
                dao.getAllWaterRecords().forEach { w ->
                    db.child("users").child(userId).child("water_records")
                        .child(w.date).setValue(mapOf("glasses" to w.glasses))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Water succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Water failed: ${e.message}") }
                }

                // Meals
                dao.getAllMealRecords().forEach { m ->
                    db.child("users").child(userId).child("meal_records")
                        .child(m.id.toString()).setValue(mapOf(
                            "date" to m.date,
                            "mealType" to m.mealType,
                            "foodName" to m.foodName,
                            "calories" to m.calories,
                            "protein" to m.protein,
                            "carbs" to m.carbs,
                            "fats" to m.fats,
                            "timestamp" to m.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Meals succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Meals failed: ${e.message}") }
                }

                // Sleep
                dao.getAllSleepRecords().forEach { sl ->
                    db.child("users").child(userId).child("sleep_records")
                        .child(sl.id.toString()).setValue(mapOf(
                            "date" to sl.date,
                            "bedTime" to sl.bedTime,
                            "wakeTime" to sl.wakeTime,
                            "durationHours" to sl.durationHours,
                            "quality" to sl.quality
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Sleep succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Sleep failed: ${e.message}") }
                }

                // Workouts
                dao.getAllWorkoutRecords().forEach { w ->
                    db.child("users").child(userId).child("workout_records")
                        .child(w.id.toString()).setValue(mapOf(
                            "date" to w.date,
                            "workoutType" to w.workoutType,
                            "repsOrMinutes" to w.repsOrMinutes,
                            "caloriesBurned" to w.caloriesBurned,
                            "timestamp" to w.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Workouts succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Workouts failed: ${e.message}") }
                }

                // Yoga
                dao.getAllYogaRecords().forEach { y ->
                    db.child("users").child(userId).child("yoga_records")
                        .child(y.id.toString()).setValue(mapOf(
                            "date" to y.date,
                            "poseName" to y.poseName,
                            "durationSeconds" to y.durationSeconds,
                            "accuracy" to y.accuracy,
                            "timestamp" to y.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Yoga succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Yoga failed: ${e.message}") }
                }

                // Heart Rate
                dao.getAllHeartRateRecordsBackup().forEach { h ->
                    db.child("users").child(userId).child("heart_rate_records")
                        .child(h.id.toString()).setValue(mapOf(
                            "bpm" to h.bpm,
                            "hrv" to h.hrv,
                            "stress" to h.stress,
                            "energy" to h.energy,
                            "timestamp" to h.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Heart Rate succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Heart Rate failed: ${e.message}") }
                }

                // Blood Pressure
                dao.getAllBPRecordsBackup().forEach { b ->
                    db.child("users").child(userId).child("blood_pressure_records")
                        .child(b.id.toString()).setValue(mapOf(
                            "systolic" to b.systolic,
                            "diastolic" to b.diastolic,
                            "pulse" to b.pulse,
                            "timestamp" to b.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup BP succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup BP failed: ${e.message}") }
                }

                // Chat Records
                dao.getAllChatRecords().forEach { c ->
                    db.child("users").child(userId).child("ai_chat_records")
                        .child(c.id.toString()).setValue(mapOf(
                            "text" to c.text,
                            "isUser" to c.isUser,
                            "timestamp" to c.timestamp
                        ))
                        .addOnSuccessListener { Log.d("SYNC_MANAGER", "uploadFullBackup Chat succeeded") }
                        .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "uploadFullBackup Chat failed: ${e.message}") }
                }
                Log.d("SYNC_MANAGER", "Full backup upload completed successfully.")
            } catch (e: Exception) {
                Log.e("SYNC_MANAGER", "Error uploading full backup: ${e.message}")
            }
        }
    }

    // --- FULL BACKUP RESTORE ---

    fun restoreFullBackup(userId: String, appDatabase: AppDatabase, prefsManager: SharedPrefsManager? = null, onComplete: () -> Unit = {}) {
        val dao = appDatabase.healthDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear existing local databases to prevent old remnants
                dao.deleteAllSteps()
                dao.deleteAllWater()
                dao.deleteAllMeals()
                dao.deleteAllSleep()
                dao.deleteAllYoga()
                dao.deleteAllWorkouts()
                dao.deleteAllHeartRates()
                dao.deleteAllBloodPressures()
                dao.deleteAllChatRecords()

                // Restore Steps
                val stepSnapshot = Tasks.await(db.child("users").child(userId).child("step_records").get())
                stepSnapshot.children.forEach { doc ->
                    val date = doc.key ?: return@forEach
                    val steps = doc.child("steps").value as? Long ?: 0L
                    dao.insertStepRecord(StepRecord(date, steps.toInt()))
                }

                // Restore Water
                val waterSnapshot = Tasks.await(db.child("users").child(userId).child("water_records").get())
                waterSnapshot.children.forEach { doc ->
                    val date = doc.key ?: return@forEach
                    val glasses = doc.child("glasses").value as? Long ?: 0L
                    dao.insertWaterRecord(WaterRecord(date, glasses.toInt()))
                }

                // Restore Meals
                val mealSnapshot = Tasks.await(db.child("users").child(userId).child("meal_records").get())
                mealSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val date = doc.child("date").value as? String ?: ""
                    val mealType = doc.child("mealType").value as? String ?: ""
                    val foodName = doc.child("foodName").value as? String ?: ""
                    val calories = doc.child("calories").value as? Long ?: 0L
                    val protein = doc.child("protein").value as? Long ?: 0L
                    val carbs = doc.child("carbs").value as? Long ?: 0L
                    val fats = doc.child("fats").value as? Long ?: 0L
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertMeal(MealRecord(id, date, mealType, foodName, calories.toInt(), protein.toInt(), carbs.toInt(), fats.toInt(), timestamp))
                }

                // Restore Sleep
                val sleepSnapshot = Tasks.await(db.child("users").child(userId).child("sleep_records").get())
                sleepSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val date = doc.child("date").value as? String ?: ""
                    val bedTime = doc.child("bedTime").value as? Long ?: 0L
                    val wakeTime = doc.child("wakeTime").value as? Long ?: 0L
                    val durationHours = (doc.child("durationHours").value as? Number)?.toFloat() ?: 0f
                    val quality = doc.child("quality").value as? String ?: "Good"
                    dao.insertSleepRecord(SleepRecord(id, date, bedTime, wakeTime, durationHours, quality))
                }

                // Restore Workouts
                val workoutSnapshot = Tasks.await(db.child("users").child(userId).child("workout_records").get())
                workoutSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val date = doc.child("date").value as? String ?: ""
                    val workoutType = doc.child("workoutType").value as? String ?: ""
                    val repsOrMinutes = doc.child("repsOrMinutes").value as? Long ?: 0L
                    val caloriesBurned = doc.child("caloriesBurned").value as? Long ?: 0L
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertWorkoutRecord(WorkoutRecord(id, date, workoutType, repsOrMinutes.toInt(), caloriesBurned.toInt(), timestamp))
                }

                // Restore Yoga
                val yogaSnapshot = Tasks.await(db.child("users").child(userId).child("yoga_records").get())
                yogaSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val date = doc.child("date").value as? String ?: ""
                    val poseName = doc.child("poseName").value as? String ?: ""
                    val durationSeconds = doc.child("durationSeconds").value as? Long ?: 0L
                    val accuracy = doc.child("accuracy").value as? Long ?: 0L
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertYogaRecord(YogaRecord(id, date, poseName, durationSeconds.toInt(), accuracy.toInt(), timestamp))
                }

                // Restore Heart Rate
                val hrSnapshot = Tasks.await(db.child("users").child(userId).child("heart_rate_records").get())
                hrSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val bpm = doc.child("bpm").value as? Long ?: 0L
                    val hrv = doc.child("hrv").value as? Long ?: 0L
                    val stress = doc.child("stress").value as? String ?: "Low"
                    val energy = doc.child("energy").value as? Long ?: 0L
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertHeartRate(HeartRateRecord(id, bpm.toInt(), hrv.toInt(), stress, energy.toInt(), timestamp))
                }

                // Restore Blood Pressure
                val bpSnapshot = Tasks.await(db.child("users").child(userId).child("blood_pressure_records").get())
                bpSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val systolic = doc.child("systolic").value as? Long ?: 0L
                    val diastolic = doc.child("diastolic").value as? Long ?: 0L
                    val pulse = doc.child("pulse").value as? Long ?: 0L
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertBloodPressure(BloodPressureRecord(id, systolic.toInt(), diastolic.toInt(), pulse.toInt(), timestamp))
                }

                // Restore Chat Records
                val chatSnapshot = Tasks.await(db.child("users").child(userId).child("ai_chat_records").get())
                chatSnapshot.children.forEach { doc ->
                    val id = doc.key?.toIntOrNull() ?: return@forEach
                    val text = doc.child("text").value as? String ?: ""
                    val isUser = doc.child("isUser").value as? Boolean ?: false
                    val timestamp = doc.child("timestamp").value as? Long ?: System.currentTimeMillis()
                    dao.insertChatRecord(AIChatRecord(id, text, isUser, timestamp))
                }

                // Synchronize restored today stats into SharedPrefsManager
                if (prefsManager != null) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val todaySteps = dao.getStepsForDate(today)?.steps ?: 0
                    val todayWater = dao.getWaterForDate(today)?.glasses ?: 0
                    if (todaySteps > 0) {
                        prefsManager.saveStepsToday(todaySteps)
                    }
                    if (todayWater > 0) {
                        prefsManager.saveWaterGlassesToday(todayWater)
                    }
                }

                Log.d("SYNC_MANAGER", "Full restore completed successfully.")
            } catch (e: Exception) {
                Log.e("SYNC_MANAGER", "Error restoring full backup: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // --- ACHIEVEMENTS SYNC ---

    fun syncAchievementsToCloud(userId: String, badges: Set<String>) {
        if (userId.isBlank()) return
        db.child("users").child(userId)
            .updateChildren(mapOf("unlockedBadges" to badges.toList(), "badgesUpdatedAt" to System.currentTimeMillis()))
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "Badge sync failed: ${e.message}") }
    }

    fun fetchAchievementsFromCloud(
        userId: String,
        onSuccess: (Set<String>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        if (userId.isBlank()) { onSuccess(emptySet()); return }
        db.child("users").child(userId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val list = doc.child("unlockedBadges").value as? List<String> ?: emptyList()
                onSuccess(list.toSet())
            }
            .addOnFailureListener { onFailure(it) }
    }

    // --- FOOD SCAN SYNC ---

    fun syncFoodScanToCloud(userId: String, foodName: String, calories: Int, protein: Int, carbs: Int, fats: Int) {
        if (userId.isBlank()) return
        val data = mapOf(
            "foodName" to foodName,
            "calories" to calories,
            "protein" to protein,
            "carbs" to carbs,
            "fats" to fats,
            "scannedAt" to System.currentTimeMillis()
        )
        db.child("users").child(userId).child("food_scans")
            .push()
            .setValue(data)
            .addOnFailureListener { e -> Log.e("SYNC_MANAGER", "Food scan sync failed: ${e.message}") }
    }

    fun fetchFoodScansFromCloud(
        userId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit
    ) {
        if (userId.isBlank()) { onSuccess(emptyList()); return }
        db.child("users").child(userId).child("food_scans")
            .orderByChild("scannedAt")
            .limitToLast(50)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.children.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val map = doc.value as? Map<String, Any>
                    map
                }.reversed()
                onSuccess(list)
            }
            .addOnFailureListener { onSuccess(emptyList()) }
    }

    // --- PREMIUM STATUS SYNC ---

    fun syncPremiumStatusToCloud(userId: String, isPremium: Boolean, expiryDate: Long) {
        if (userId.isBlank()) return
        db.child("users").child(userId)
            .updateChildren(mapOf("isPremium" to isPremium, "premiumExpiry" to expiryDate))
    }

    fun fetchPremiumStatusFromCloud(
        userId: String,
        onResult: (isPremium: Boolean, expiry: Long) -> Unit
    ) {
        if (userId.isBlank()) { onResult(false, 0L); return }
        db.child("users").child(userId).get()
            .addOnSuccessListener { doc ->
                val isPremium = doc.child("isPremium").value as? Boolean ?: false
                val expiry = doc.child("premiumExpiry").value as? Long ?: 0L
                onResult(isPremium, expiry)
            }
            .addOnFailureListener { onResult(false, 0L) }
    }
}
