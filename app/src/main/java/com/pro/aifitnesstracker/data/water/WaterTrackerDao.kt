package com.pro.aifitnesstracker.data.water

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterTrackerDao {

    // ── Water Intake Entries ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WaterIntakeEntry): Long

    @Update
    suspend fun updateEntry(entry: WaterIntakeEntry)

    @Delete
    suspend fun deleteEntry(entry: WaterIntakeEntry)

    @Query("DELETE FROM water_intake_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT * FROM water_intake_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getEntriesForDate(date: String): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getEntriesForDateSync(date: String): List<WaterIntakeEntry>

    @Query("SELECT * FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    suspend fun getEntriesBetweenDatesSync(startDate: String, endDate: String): List<WaterIntakeEntry>

    @Query("SELECT * FROM water_intake_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesSync(): List<WaterIntakeEntry>

    @Query("SELECT DISTINCT date FROM water_intake_entries ORDER BY date DESC")
    suspend fun getAllDistinctDates(): List<String>

    @Query("DELETE FROM water_intake_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Query("SELECT SUM(amountMl) FROM water_intake_entries WHERE date = :date")
    suspend fun getTotalMlForDate(date: String): Int?

    // ── Custom Water Containers ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: CustomWaterContainer): Long

    @Update
    suspend fun updateContainer(container: CustomWaterContainer)

    @Delete
    suspend fun deleteContainer(container: CustomWaterContainer)

    @Query("DELETE FROM custom_water_containers WHERE id = :id")
    suspend fun deleteContainerById(id: Long)

    @Query("SELECT * FROM custom_water_containers ORDER BY id ASC")
    fun getAllContainers(): Flow<List<CustomWaterContainer>>

    @Query("SELECT * FROM custom_water_containers ORDER BY id ASC")
    suspend fun getAllContainersSync(): List<CustomWaterContainer>

    @Query("SELECT COUNT(*) FROM custom_water_containers")
    suspend fun getContainerCount(): Int
}
