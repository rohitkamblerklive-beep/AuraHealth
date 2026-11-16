package com.pro.aifitnesstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Android 14+ safe bootstrapping via WorkManager
            StepBootWorker.enqueue(context)

            // Direct start for pre-Android 14 devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    val serviceIntent = Intent(context, StepCounterService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            com.pro.aifitnesstracker.utils.SmartAlarmManager.scheduleGoalReminder(context)
            MidnightResetReceiver.scheduleMidnightAlarm(context)
            FeatureRecommendationReceiver.scheduleRecommendationAlarm(context)
            com.pro.aifitnesstracker.data.water.WaterReminderScheduler.schedule(context)
            CoroutineScope(Dispatchers.IO).launch {
                com.pro.aifitnesstracker.data.sleep.SleepReminderScheduler.reschedule(context)
            }
        }
    }
}
