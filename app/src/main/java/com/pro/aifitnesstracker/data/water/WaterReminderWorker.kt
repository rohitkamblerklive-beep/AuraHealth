package com.pro.aifitnesstracker.data.water

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.NotificationActionReceiver
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.SharedPrefsManager
import java.util.Calendar

class WaterReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = SharedPrefsManager(appContext)

        // 1. Check if reminders are enabled
        if (!prefs.isWaterReminderEnabled()) {
            return Result.success()
        }

        // 2. Check Sleep / Do Not Disturb time window
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = prefs.getWaterReminderStartTime() // e.g. 8 * 60 = 480
        val endMinutes = prefs.getWaterReminderEndTime()     // e.g. 22 * 60 = 1320

        if (prefs.isWaterDndDuringSleep()) {
            val isWithinAllowedHours = if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Wraps across midnight (e.g. 07:00 to 01:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }

            if (!isWithinAllowedHours) {
                // In sleep hours - suppress notification
                return Result.success()
            }
        }

        // 3. Prepare Notification Content
        val consumedMl = prefs.getWaterMlToday()
        val goalMl = prefs.getWaterGoalMl().coerceAtLeast(500)
        val remainingMl = (goalMl - consumedMl).coerceAtLeast(0)
        val progressPct = ((consumedMl.toFloat() / goalMl.toFloat()) * 100).toInt()

        val title = "Time to Hydrate! 💧"
        val message = if (remainingMl > 0) {
            "Take a moment to drink some water. $consumedMl ml of $goalMl ml logged ($progressPct%). Keep moving towards your daily goal!"
        } else {
            "Daily goal reached ($consumedMl ml)! Remember to keep sipping water to stay energized."
        }

        showWaterNotification(appContext, title, message, prefs.isWaterReminderSound())
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "water_hydration_reminders"
        const val NOTIFICATION_ID = 404

        fun showWaterNotification(
            context: Context,
            title: String,
            message: String,
            playSound: Boolean = true
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Hydration Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Regular notifications reminding you to drink water"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Intent to open Water Tracking Screen
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO", "WATER")
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action Intent: Quick Log +250ml
            val quickDrinkIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DRANK_WATER"
            }
            val quickDrinkPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                quickDrinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = if (playSound) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .addAction(
                    android.R.drawable.ic_input_add,
                    "+250 ml Drink",
                    quickDrinkPendingIntent
                )
                .apply {
                    if (soundUri != null) setSound(soundUri)
                }
                .build()

            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Notification permission not granted
            }
        }
    }
}
