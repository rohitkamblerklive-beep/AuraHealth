package com.pro.aifitnesstracker.data.water

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pro.aifitnesstracker.SharedPrefsManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WaterReminderScheduler {
    private const val WORK_NAME = "water_hydration_periodic_work"

    /**
     * Schedules or reschedules periodic water reminders using WorkManager based on user preferences.
     */
    fun schedule(context: Context) {
        val prefs = SharedPrefsManager(context)
        val workManager = WorkManager.getInstance(context)

        if (!prefs.isWaterReminderEnabled()) {
            cancel(context)
            return
        }

        val intervalMinutes = prefs.getWaterReminderIntervalMinutes().coerceAtLeast(15)

        val periodicWorkRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .addTag("water_hydration_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    /**
     * Cancels scheduled reminders immediately.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Computes human-readable next reminder time string.
     */
    fun getNextReminderTimeString(context: Context): String {
        val prefs = SharedPrefsManager(context)
        if (!prefs.isWaterReminderEnabled()) {
            return "Reminders Off"
        }

        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = prefs.getWaterReminderStartTime()
        val endMinutes = prefs.getWaterReminderEndTime()
        val intervalMins = prefs.getWaterReminderIntervalMinutes().coerceAtLeast(15)

        if (prefs.isWaterDndDuringSleep()) {
            val isSleeping = if (startMinutes <= endMinutes) {
                currentMinutes !in startMinutes..endMinutes
            } else {
                currentMinutes < startMinutes && currentMinutes > endMinutes
            }

            if (isSleeping) {
                val startHour = startMinutes / 60
                val startMin = startMinutes % 60
                val amPm = if (startHour >= 12) "PM" else "AM"
                val displayHour = if (startHour % 12 == 0) 12 else startHour % 12
                return String.format(java.util.Locale.US, "Tomorrow at %02d:%02d %s", displayHour, startMin, amPm)
            }
        }

        // Calculate next interval step
        val nextMinutes = (currentMinutes + intervalMins).coerceAtMost(24 * 60 - 1)
        val h = nextMinutes / 60
        val m = nextMinutes % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val displayH = if (h % 12 == 0) 12 else h % 12
        return String.format(java.util.Locale.US, "%02d:%02d %s (in %d min)", displayH, m, amPm, intervalMins)
    }

    /**
     * Sends an immediate test notification so the user can verify notifications and sounds.
     */
    fun sendTestNotification(context: Context) {
        val prefs = SharedPrefsManager(context)
        val consumedMl = prefs.getWaterMlToday()
        val goalMl = prefs.getWaterGoalMl()
        WaterReminderWorker.showWaterNotification(
            context,
            "Hydration Check 💧",
            "This is a preview reminder! You've logged $consumedMl ml of your $goalMl ml goal.",
            prefs.isWaterReminderSound()
        )
    }
}
