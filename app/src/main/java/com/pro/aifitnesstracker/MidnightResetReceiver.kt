package com.pro.aifitnesstracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that fires at midnight (00:00:00) via AlarmManager.
 * Resets the daily step counter and re-schedules itself for the next midnight.
 * This is reliable even in Doze mode because we use setExactAndAllowWhileIdle.
 */
class MidnightResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefsManager = SharedPrefsManager(context)

        // Only reset if it's actually a new day (safety check)
        if (prefsManager.checkIfNewDay()) {
            val yesterdayDate = prefsManager.getLastStepDate()
            val yesterdaySteps = prefsManager.getStepsToday()
            if (yesterdayDate.isNotEmpty() && yesterdaySteps > 0) {
                try {
                    val db = AppDatabase.getDatabase(context)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        db.healthDao().insertStepRecord(StepRecord(yesterdayDate, yesterdaySteps))
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            FirebaseSyncManager.syncSingleStep(currentUser.uid, yesterdayDate, yesterdaySteps)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Reset day in prefs
            prefsManager.resetDay()
            prefsManager.saveLastSensorValue(-1)

            // Broadcast step reset to any active UI
            try {
                context.sendBroadcast(Intent("com.pro.aifitnesstracker.STEP_UPDATE").apply {
                    putExtra("steps", 0)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Update the foreground notification to show 0 steps
            try {
                val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                    action = "com.pro.aifitnesstracker.ACTION_MIDNIGHT_RESET"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Re-schedule for the next midnight
        scheduleMidnightAlarm(context)
    }

    companion object {
        private const val REQUEST_CODE = 9999

        /**
         * Schedule an exact alarm for the next midnight (00:00:00).
         * Uses setExactAndAllowWhileIdle to work in Doze mode.
         */
        fun scheduleMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, MidnightResetReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate next midnight
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)  // 5 seconds past midnight for safety
                set(Calendar.MILLISECOND, 0)
            }

            // Use setExactAndAllowWhileIdle for Doze mode compatibility, with safe fallback for Android 12+ (API 31+)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            midnight.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            midnight.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}
