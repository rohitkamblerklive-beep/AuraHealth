package com.pro.aifitnesstracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    // Heart Rate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(record: HeartRateRecord): Long

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC")
    fun getAllHeartRateRecords(): Flow<List<HeartRateRecord>>

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHeartRateRecord(): HeartRateRecord?

    // Blood Pressure
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressure(record: BloodPressureRecord): Long

    @Query("SELECT * FROM blood_pressure_records ORDER BY timestamp DESC")
    fun getAllBPRecords(): Flow<List<BloodPressureRecord>>

    @Query("SELECT * FROM blood_pressure_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBPRecord(): BloodPressureRecord?

    @Query("DELETE FROM blood_pressure_records WHERE systolic = :systolic AND diastolic = :diastolic AND pulse = :pulse")
    suspend fun deleteBloodPressureByValues(systolic: Int, diastolic: Int, pulse: Int)

    // Steps
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepRecord(record: StepRecord)

    @Query("SELECT * FROM step_records WHERE date = :date")
    suspend fun getStepsForDate(date: String): StepRecord?

    @Query("SELECT * FROM step_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getStepRecordsForDateRange(startDate: String, endDate: String): List<StepRecord>

    @Query("SELECT * FROM step_records ORDER BY date DESC LIMIT 30")
    fun getStepHistory(): Flow<List<StepRecord>>

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentHeartRates(): List<HeartRateRecord>

    // Water
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterRecord(record: WaterRecord)

    @Query("SELECT * FROM water_records WHERE date = :date")
    suspend fun getWaterForDate(date: String): WaterRecord?

    @Query("SELECT * FROM water_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getWaterRecordsForDateRange(startDate: String, endDate: String): List<WaterRecord>

    // --- MEALS ---
    @Insert
    suspend fun insertMeal(meal: MealRecord): Long

    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY timestamp ASC")
    fun getMealsByDate(date: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
    fun getMealsForDateRange(startDate: String, endDate: String): Flow<List<MealRecord>>

    @Query("SELECT * FROM meal_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
    suspend fun getMealRecordsListForDateRange(startDate: String, endDate: String): List<MealRecord>

    @Delete
    suspend fun deleteMeal(meal: MealRecord)

    // --- SLEEP ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecord): Long

    @Query("SELECT * FROM sleep_records ORDER BY date DESC LIMIT 30")
    fun getSleepHistory(): Flow<List<SleepRecord>>

    @Delete
    suspend fun deleteSleepRecord(record: SleepRecord)

    // --- YOGA ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYogaRecord(record: YogaRecord): Long

    @Query("SELECT * FROM yoga_records ORDER BY timestamp DESC LIMIT 50")
    fun getYogaHistory(): Flow<List<YogaRecord>>

    // --- WORKOUT ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutRecord(record: WorkoutRecord): Long

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC LIMIT 50")
    fun getWorkoutHistory(): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getWorkoutRecordsForDateRange(startDate: String, endDate: String): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getWorkoutRecordsListForDateRange(startDate: String, endDate: String): List<WorkoutRecord>

    // --- WORKOUT PLANS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlanRecord)

    @Query("SELECT * FROM workout_plan_records WHERE date = :date ORDER BY id ASC")
    fun getWorkoutPlansByDate(date: String): Flow<List<WorkoutPlanRecord>>

    @Delete
    suspend fun deleteWorkoutPlan(plan: WorkoutPlanRecord)

    @Query("DELETE FROM workout_plan_records")
    suspend fun deleteAllWorkoutPlans()

    @Query("SELECT * FROM workout_plan_records")
    suspend fun getAllWorkoutPlans(): List<WorkoutPlanRecord>


    // Backup & Restore helper queries
    @Query("SELECT * FROM heart_rate_records")
    suspend fun getAllHeartRateRecordsBackup(): List<HeartRateRecord>

    @Query("SELECT * FROM blood_pressure_records")
    suspend fun getAllBPRecordsBackup(): List<BloodPressureRecord>

    @Query("SELECT * FROM step_records")
    suspend fun getAllStepRecords(): List<StepRecord>

    @Query("SELECT * FROM water_records")
    suspend fun getAllWaterRecords(): List<WaterRecord>

    @Query("SELECT * FROM meal_records")
    suspend fun getAllMealRecords(): List<MealRecord>

    @Query("SELECT * FROM sleep_records")
    suspend fun getAllSleepRecords(): List<SleepRecord>

    @Query("SELECT * FROM yoga_records")
    suspend fun getAllYogaRecords(): List<YogaRecord>

    @Query("SELECT * FROM workout_records")
    suspend fun getAllWorkoutRecords(): List<WorkoutRecord>

    @Query("DELETE FROM heart_rate_records")
    suspend fun deleteAllHeartRates()

    @Query("DELETE FROM blood_pressure_records")
    suspend fun deleteAllBloodPressures()

    @Query("DELETE FROM step_records")
    suspend fun deleteAllSteps()

    @Query("DELETE FROM water_records")
    suspend fun deleteAllWater()

    @Query("DELETE FROM meal_records")
    suspend fun deleteAllMeals()

    @Query("DELETE FROM sleep_records")
    suspend fun deleteAllSleep()

    @Query("DELETE FROM yoga_records")
    suspend fun deleteAllYoga()

    @Query("DELETE FROM workout_records")
    suspend fun deleteAllWorkouts()

    // --- AI CHAT HISTORY ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRecord(record: AIChatRecord): Long

    @Query("SELECT * FROM ai_chat_records ORDER BY timestamp ASC")
    fun getAllChatRecordsFlow(): Flow<List<AIChatRecord>>

    @Query("SELECT * FROM ai_chat_records")
    suspend fun getAllChatRecords(): List<AIChatRecord>

    @Query("DELETE FROM ai_chat_records")
    suspend fun deleteAllChatRecords()
}
