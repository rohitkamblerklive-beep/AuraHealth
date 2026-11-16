package com.pro.aifitnesstracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord as HCHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnect"

        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(HCHeartRateRecord::class),
            HealthPermission.getWritePermission(HCHeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
        )
    }

    val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Health Connect not available: ${e.message}")
            null
        }
    }

    fun isAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) { false }
    }

    fun openHealthConnectStore() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            )
            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(webIntent)
        }
    }

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(PERMISSIONS)
        } catch (e: Exception) { false }
    }

    // --- READ STEPS ---
    suspend fun readTodaySteps(): Int = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext 0
        try {
            val today = LocalDate.now()
            val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = Instant.now()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records.sumOf { it.count }.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Read steps error: ${e.message}")
            0
        }
    }

    // --- WRITE STEPS ---
    suspend fun writeSteps(steps: Long) = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext
        try {
            val now = Instant.now()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val record = StepsRecord(
                startTime = startOfDay,
                endTime = now,
                count = steps,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startOfDay),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
            )
            client.insertRecords(listOf(record))
            Log.d(TAG, "Steps written: $steps")
        } catch (e: Exception) {
            Log.e(TAG, "Write steps error: ${e.message}")
        }
    }

    // --- READ HEART RATE ---
    suspend fun readLatestHeartRate(): Int = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext 0
        try {
            val end = Instant.now()
            val start = end.minusSeconds(24 * 3600)
            val request = ReadRecordsRequest(
                recordType = HCHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Read heart rate error: ${e.message}")
            0
        }
    }

    // --- READ SLEEP ---
    suspend fun readLastNightSleepHours(): Float = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext 0f
        try {
            val end = Instant.now()
            val start = end.minusSeconds(48 * 3600)
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            val last = response.records.lastOrNull() ?: return@withContext 0f
            val durationMs = last.endTime.toEpochMilli() - last.startTime.toEpochMilli()
            durationMs / 3_600_000f
        } catch (e: Exception) {
            Log.e(TAG, "Read sleep error: ${e.message}")
            0f
        }
    }

    // --- READ CALORIES ---
    suspend fun readTodayCaloriesBurned(): Int = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext 0
        try {
            val today = LocalDate.now()
            val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = Instant.now()
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records.sumOf { it.energy.inKilocalories }.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Read calories error: ${e.message}")
            0
        }
    }
}
