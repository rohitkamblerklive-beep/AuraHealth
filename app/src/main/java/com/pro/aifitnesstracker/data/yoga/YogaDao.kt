package com.pro.aifitnesstracker.data.yoga

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface YogaDao {

    // ── Session Records ───────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: YogaSessionRecord): Long

    @Query("SELECT * FROM yoga_session_records ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<YogaSessionRecord>>

    @Query("SELECT * FROM yoga_session_records WHERE date = :date ORDER BY timestamp DESC")
    fun getSessionsForDate(date: String): Flow<List<YogaSessionRecord>>

    @Query("SELECT * FROM yoga_session_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<YogaSessionRecord>>

    @Query("SELECT * FROM yoga_session_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<YogaSessionRecord>

    @Query("SELECT * FROM yoga_session_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<YogaSessionRecord>>

    @Query("SELECT SUM(completedDurationSeconds) FROM yoga_session_records")
    fun getTotalCompletedSeconds(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM yoga_session_records WHERE isFullyCompleted = 1")
    fun getTotalCompletedSessionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM yoga_session_records")
    suspend fun getTotalSessionsCountSync(): Int

    @Query("SELECT DISTINCT date FROM yoga_session_records WHERE completedDurationSeconds > 60 ORDER BY date DESC")
    fun getDistinctPracticedDates(): Flow<List<String>>

    @Query("SELECT DISTINCT date FROM yoga_session_records WHERE completedDurationSeconds > 60 ORDER BY date DESC")
    suspend fun getDistinctPracticedDatesSync(): List<String>

    @Query("DELETE FROM yoga_session_records WHERE id = :id")
    suspend fun deleteSession(id: Int)

    // ── Custom Routines ───────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomRoutine(routine: CustomYogaRoutineEntity)

    @Update
    suspend fun updateCustomRoutine(routine: CustomYogaRoutineEntity)

    @Query("SELECT * FROM custom_yoga_routines ORDER BY createdAt DESC")
    fun getAllCustomRoutines(): Flow<List<CustomYogaRoutineEntity>>

    @Query("DELETE FROM custom_yoga_routines WHERE id = :id")
    suspend fun deleteCustomRoutine(id: String)

    // ── Favorites ─────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: YogaFavoriteEntity)

    @Query("DELETE FROM yoga_favorites WHERE poseId = :poseId")
    suspend fun deleteFavorite(poseId: String)

    @Query("SELECT * FROM yoga_favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<YogaFavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM yoga_favorites WHERE poseId = :poseId)")
    fun isFavorite(poseId: String): Flow<Boolean>

    // ── Plan Progress ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlanProgress(progress: YogaPlanProgressEntity)

    @Query("SELECT * FROM yoga_plan_progress")
    fun getAllPlanProgress(): Flow<List<YogaPlanProgressEntity>>

    @Query("SELECT * FROM yoga_plan_progress WHERE planId = :planId")
    fun getPlanProgress(planId: String): Flow<YogaPlanProgressEntity?>

    @Query("SELECT * FROM yoga_plan_progress WHERE planId = :planId")
    suspend fun getPlanProgressSync(planId: String): YogaPlanProgressEntity?

    @Query("DELETE FROM yoga_plan_progress WHERE planId = :planId")
    suspend fun resetPlanProgress(planId: String)

    // ── Achievements ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(list: List<YogaAchievementEntity>)

    @Query("SELECT * FROM yoga_achievements")
    fun getAllAchievements(): Flow<List<YogaAchievementEntity>>

    @Query("UPDATE yoga_achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id AND isUnlocked = 0")
    suspend fun unlockAchievement(id: String, timestamp: Long = System.currentTimeMillis())
}
