package com.pro.aifitnesstracker.data.sleep

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepTrackerDao {

    // --- SESSIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Delete
    suspend fun deleteSession(session: SleepSessionEntity)

    @Query("DELETE FROM sleep_session_records WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM sleep_session_records ORDER BY bedTimeMs DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_session_records ORDER BY bedTimeMs DESC")
    suspend fun getAllSessionsList(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_session_records ORDER BY bedTimeMs DESC LIMIT 1")
    fun getLatestSession(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_session_records ORDER BY bedTimeMs DESC LIMIT 1")
    suspend fun getLatestSessionSync(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session_records WHERE date = :date ORDER BY bedTimeMs DESC LIMIT 1")
    suspend fun getSessionForDate(date: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, bedTimeMs ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_session_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, bedTimeMs ASC")
    suspend fun getSessionsListBetweenDates(startDate: String, endDate: String): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_session_records WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session_records WHERE (bedTimeMs < :wakeTime AND wakeTimeMs > :bedTime)")
    suspend fun findOverlappingSessions(bedTime: Long, wakeTime: Long): List<SleepSessionEntity>

    // --- SCHEDULE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchedule(schedule: SleepScheduleEntity)

    @Query("SELECT * FROM sleep_schedule_config WHERE id = 1 LIMIT 1")
    fun getSchedule(): Flow<SleepScheduleEntity?>

    @Query("SELECT * FROM sleep_schedule_config WHERE id = 1 LIMIT 1")
    suspend fun getScheduleSync(): SleepScheduleEntity?

    // --- ACHIEVEMENTS ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(list: List<SleepAchievementEntity>)

    @Query("SELECT * FROM sleep_achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<SleepAchievementEntity>>

    @Query("UPDATE sleep_achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id AND isUnlocked = 0")
    suspend fun unlockAchievement(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM sleep_session_records")
    suspend fun getTotalSessionsCount(): Int
}
