package com.pro.aifitnesstracker.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pro.aifitnesstracker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.healthDao()

    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject()

            // 1. Export SharedPreferences
            val prefs = context.getSharedPreferences("AIFitnessPrefs", Context.MODE_PRIVATE)
            val prefsJson = JSONObject()
            for ((key, value) in prefs.all) {
                prefsJson.put(key, value)
            }
            jsonObject.put("preferences", prefsJson)

            // 2. Export Heart Rate Records
            val hrArray = JSONArray()
            dao.getAllHeartRateRecordsBackup().forEach { record ->
                val obj = JSONObject()
                obj.put("bpm", record.bpm)
                obj.put("hrv", record.hrv)
                obj.put("stress", record.stress)
                obj.put("energy", record.energy)
                obj.put("timestamp", record.timestamp)
                hrArray.put(obj)
            }
            jsonObject.put("heart_rates", hrArray)

            // 3. Export Blood Pressure Records
            val bpArray = JSONArray()
            dao.getAllBPRecordsBackup().forEach { record ->
                val obj = JSONObject()
                obj.put("systolic", record.systolic)
                obj.put("diastolic", record.diastolic)
                obj.put("pulse", record.pulse)
                obj.put("timestamp", record.timestamp)
                bpArray.put(obj)
            }
            jsonObject.put("blood_pressures", bpArray)

            // 4. Export Step Records
            val stepArray = JSONArray()
            dao.getAllStepRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("steps", record.steps)
                stepArray.put(obj)
            }
            jsonObject.put("steps", stepArray)

            // 5. Export Water Records
            val waterArray = JSONArray()
            dao.getAllWaterRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("glasses", record.glasses)
                waterArray.put(obj)
            }
            jsonObject.put("water", waterArray)

            // 6. Export Meal Records
            val mealArray = JSONArray()
            dao.getAllMealRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("mealType", record.mealType)
                obj.put("foodName", record.foodName)
                obj.put("calories", record.calories)
                obj.put("protein", record.protein)
                obj.put("carbs", record.carbs)
                obj.put("fats", record.fats)
                obj.put("timestamp", record.timestamp)
                mealArray.put(obj)
            }
            jsonObject.put("meals", mealArray)

            // 7. Export Sleep Records
            val sleepArray = JSONArray()
            dao.getAllSleepRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("bedTime", record.bedTime)
                obj.put("wakeTime", record.wakeTime)
                obj.put("durationHours", record.durationHours.toDouble())
                obj.put("quality", record.quality)
                sleepArray.put(obj)
            }
            jsonObject.put("sleep", sleepArray)

            // 8. Export Yoga Records
            val yogaArray = JSONArray()
            dao.getAllYogaRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("poseName", record.poseName)
                obj.put("durationSeconds", record.durationSeconds)
                obj.put("accuracy", record.accuracy)
                obj.put("timestamp", record.timestamp)
                yogaArray.put(obj)
            }
            jsonObject.put("yoga", yogaArray)

            // 9. Export Workout Records
            val workoutArray = JSONArray()
            dao.getAllWorkoutRecords().forEach { record ->
                val obj = JSONObject()
                obj.put("date", record.date)
                obj.put("workoutType", record.workoutType)
                obj.put("repsOrMinutes", record.repsOrMinutes)
                obj.put("caloriesBurned", record.caloriesBurned)
                obj.put("timestamp", record.timestamp)
                workoutArray.put(obj)
            }
            jsonObject.put("workouts", workoutArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonObject.toString(4))
                }
            }
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Export failed", e)
            false
        }
    }

    suspend fun importBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: return@withContext false

            val jsonObject = JSONObject(jsonString)

            // 1. Import SharedPreferences
            val prefsJson = jsonObject.optJSONObject("preferences")
            if (prefsJson != null) {
                val prefs = context.getSharedPreferences("AIFitnessPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.clear() // Clear existing settings before restore

                val keys = prefsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = prefsJson.get(key)) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                    }
                }
                editor.apply()
            }

            // Clear database tables before inserting to prevent duplicate records
            dao.deleteAllHeartRates()
            dao.deleteAllBloodPressures()
            dao.deleteAllSteps()
            dao.deleteAllWater()
            dao.deleteAllMeals()
            dao.deleteAllSleep()
            dao.deleteAllYoga()
            dao.deleteAllWorkouts()

            // 2. Import Heart Rate Records
            val hrArray = jsonObject.optJSONArray("heart_rates")
            if (hrArray != null) {
                for (i in 0 until hrArray.length()) {
                    val obj = hrArray.getJSONObject(i)
                    dao.insertHeartRate(
                        HeartRateRecord(
                            bpm = obj.getInt("bpm"),
                            hrv = obj.getInt("hrv"),
                            stress = obj.getString("stress"),
                            energy = obj.getInt("energy"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }

            // 3. Import Blood Pressure Records
            val bpArray = jsonObject.optJSONArray("blood_pressures")
            if (bpArray != null) {
                for (i in 0 until bpArray.length()) {
                    val obj = bpArray.getJSONObject(i)
                    dao.insertBloodPressure(
                        BloodPressureRecord(
                            systolic = obj.getInt("systolic"),
                            diastolic = obj.getInt("diastolic"),
                            pulse = obj.getInt("pulse"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }

            // 4. Import Step Records
            val stepArray = jsonObject.optJSONArray("steps")
            if (stepArray != null) {
                for (i in 0 until stepArray.length()) {
                    val obj = stepArray.getJSONObject(i)
                    dao.insertStepRecord(
                        StepRecord(
                            date = obj.getString("date"),
                            steps = obj.getInt("steps")
                        )
                    )
                }
            }

            // 5. Import Water Records
            val waterArray = jsonObject.optJSONArray("water")
            if (waterArray != null) {
                for (i in 0 until waterArray.length()) {
                    val obj = waterArray.getJSONObject(i)
                    dao.insertWaterRecord(
                        WaterRecord(
                            date = obj.getString("date"),
                            glasses = obj.getInt("glasses")
                        )
                    )
                }
            }

            // 6. Import Meal Records
            val mealArray = jsonObject.optJSONArray("meals")
            if (mealArray != null) {
                for (i in 0 until mealArray.length()) {
                    val obj = mealArray.getJSONObject(i)
                    dao.insertMeal(
                        MealRecord(
                            date = obj.getString("date"),
                            mealType = obj.getString("mealType"),
                            foodName = obj.getString("foodName"),
                            calories = obj.getInt("calories"),
                            protein = obj.getInt("protein"),
                            carbs = obj.getInt("carbs"),
                            fats = obj.getInt("fats"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }

            // 7. Import Sleep Records
            val sleepArray = jsonObject.optJSONArray("sleep")
            if (sleepArray != null) {
                for (i in 0 until sleepArray.length()) {
                    val obj = sleepArray.getJSONObject(i)
                    dao.insertSleepRecord(
                        SleepRecord(
                            date = obj.getString("date"),
                            bedTime = obj.getLong("bedTime"),
                            wakeTime = obj.getLong("wakeTime"),
                            durationHours = obj.getDouble("durationHours").toFloat(),
                            quality = obj.getString("quality")
                        )
                    )
                }
            }

            // 8. Import Yoga Records
            val yogaArray = jsonObject.optJSONArray("yoga")
            if (yogaArray != null) {
                for (i in 0 until yogaArray.length()) {
                    val obj = yogaArray.getJSONObject(i)
                    dao.insertYogaRecord(
                        YogaRecord(
                            date = obj.getString("date"),
                            poseName = obj.getString("poseName"),
                            durationSeconds = obj.getInt("durationSeconds"),
                            accuracy = obj.getInt("accuracy"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }

            // 9. Import Workout Records
            val workoutArray = jsonObject.optJSONArray("workouts")
            if (workoutArray != null) {
                for (i in 0 until workoutArray.length()) {
                    val obj = workoutArray.getJSONObject(i)
                    dao.insertWorkoutRecord(
                        WorkoutRecord(
                            date = obj.getString("date"),
                            workoutType = obj.getString("workoutType"),
                            repsOrMinutes = obj.getInt("repsOrMinutes"),
                            caloriesBurned = obj.getInt("caloriesBurned"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Import failed", e)
            false
        }
    }
}
