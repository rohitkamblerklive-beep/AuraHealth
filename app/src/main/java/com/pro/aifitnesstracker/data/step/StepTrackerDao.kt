package com.pro.aifitnesstracker.data.step

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepTrackerDao {

    // ── Daily Step Records ──

    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    fun getDailyRecord(date: String): Flow<DailyStepEntity?>

    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    suspend fun getDailyRecordSync(date: String): DailyStepEntity?

    @Query("SELECT * FROM daily_step_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM daily_step_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getRecordsInRangeSync(startDate: String, endDate: String): List<DailyStepEntity>

    @Query("SELECT * FROM daily_step_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM daily_step_records ORDER BY date DESC LIMIT 60")
    suspend fun getRecentRecordsSync(): List<DailyStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyRecord(record: DailyStepEntity)

    // ── Walking Sessions ──

    @Query("SELECT * FROM walking_sessions WHERE date = :date ORDER BY startTimeMs DESC")
    fun getSessionsForDate(date: String): Flow<List<WalkingSessionEntity>>

    @Query("SELECT * FROM walking_sessions WHERE date = :date ORDER BY startTimeMs DESC")
    suspend fun getSessionsForDateSync(date: String): List<WalkingSessionEntity>

    @Query("SELECT * FROM walking_sessions ORDER BY startTimeMs DESC LIMIT 50")
    fun getAllRecentSessions(): Flow<List<WalkingSessionEntity>>

    @Query("SELECT * FROM walking_sessions ORDER BY startTimeMs DESC LIMIT 100")
    suspend fun getAllRecentSessionsSync(): List<WalkingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalkingSession(session: WalkingSessionEntity): Long

    @Query("DELETE FROM walking_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM walking_sessions WHERE date = :date")
    suspend fun deleteSessionsForDate(date: String)

    // ── Achievements ──

    @Query("SELECT * FROM step_achievements")
    fun getAllAchievements(): Flow<List<StepAchievementEntity>>

    @Query("SELECT * FROM step_achievements WHERE achievementId = :id LIMIT 1")
    suspend fun getAchievement(id: String): StepAchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAchievement(achievement: StepAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAchievements(achievements: List<StepAchievementEntity>)
}
