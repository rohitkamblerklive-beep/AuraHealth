package com.pro.aifitnesstracker.data.sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return

        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "SLEEP")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (type == TYPE_BEDTIME) 101 else 102,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = when (type) {
            TYPE_BEDTIME -> {
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Time to Wind Down 🌙")
                    .setContentText("Your bedtime is approaching. Prepare your environment for a restful night.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
            }
            TYPE_WAKEUP -> {
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Good Morning ☀️")
                    .setContentText("Time to wake up and start your day with calm energy!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
            }
            else -> return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (type == TYPE_BEDTIME) 201 else 202, notification)

        // Reschedule for the next day
        CoroutineScope(Dispatchers.IO).launch {
            SleepReminderScheduler.reschedule(context)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep & Bedtime Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bedtime wind down and morning wake-up alerts"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sleep_reminders_channel"
        const val EXTRA_TYPE = "REMINDER_TYPE"
        const val TYPE_BEDTIME = "TYPE_BEDTIME"
        const val TYPE_WAKEUP = "TYPE_WAKEUP"
    }
}
