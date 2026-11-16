package com.pro.aifitnesstracker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pro.aifitnesstracker.data.step.StepTrackerRepository

class StepBootWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "StepBootWorker"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<StepBootWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "StepBootWorker enqueued after boot event")
        }
    }

    override suspend fun doWork(): Result {
        try {
            val prefs = SharedPrefsManager(applicationContext)
            if (prefs.isStepTrackingPaused()) {
                Log.d(TAG, "Step tracking is paused by user, skipping service start")
                return Result.success()
            }

            // Bootstrap repository and verify today's date state
            val repo = StepTrackerRepository.getInstance(applicationContext)
            if (prefs.checkIfNewDay()) {
                prefs.resetDay()
                prefs.saveLastSensorValue(-1)
            }

            // Attempt foreground service launch safely
            val serviceIntent = Intent(applicationContext, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    applicationContext.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Foreground service restricted at boot by OS (Android 14+), state prepared: ${e.message}")
                }
            } else {
                applicationContext.startService(serviceIntent)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in StepBootWorker", e)
            return Result.failure()
        }
    }
}
