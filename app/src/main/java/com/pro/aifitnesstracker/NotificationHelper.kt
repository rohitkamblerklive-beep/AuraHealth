package com.pro.aifitnesstracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(val context: Context) {
    private val CHANNEL_ID = "daily_reminders"
    private val CHANNEL_NAME = "Daily Health Reminders"
    private val NOTIFICATION_ID = 101

    fun createNotificationChannel(
        channelId: String = CHANNEL_ID,
        channelName: String = CHANNEL_NAME
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (channelId == "smart_alarm_channel" || channelId == "daily_goal_reminder_channel" || channelId == "feature_recommendations") {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                description = "Channel for health tracking reminders"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String, navigateTo: String? = null) {
        showCustomNotification(title, message, navigateTo = navigateTo)
    }

    fun showCustomNotification(
        title: String,
        message: String,
        badgeText: String = "NUDGE",
        notificationId: Int = NOTIFICATION_ID,
        channelId: String = CHANNEL_ID,
        channelName: String = CHANNEL_NAME,
        navigateTo: String? = null
    ) {
        createNotificationChannel(channelId, channelName)

        val prefsManager = SharedPrefsManager(context)
        val steps = prefsManager.getStepsToday()
        val stepGoal = prefsManager.getStepGoal()
        val water = prefsManager.getWaterGlassesToday()
        val waterGoal = prefsManager.getWaterGoal()
        val sleep = prefsManager.getSleepHoursToday()
        val sleepGoal = prefsManager.getSleepGoal()

        // Calculate progress percentage
        val stepProgress = steps.toFloat() / stepGoal.coerceAtLeast(1)
        val waterProgress = water.toFloat() / waterGoal.coerceAtLeast(1)
        val sleepProgress = if (sleepGoal > 0f) sleep / sleepGoal else 1f

        // Average progress
        val averageProgress = ((stepProgress.coerceAtMost(1f) + waterProgress.coerceAtMost(1f) + sleepProgress.coerceAtMost(1f)) / 3f * 100f).toInt()

        // Get daily quote
        val langCode = prefsManager.getLanguage()
        val todayQuote = com.pro.aifitnesstracker.utils.MotivationalQuotes.getQuoteForToday()
        val quoteText = when (langCode) {
            "hi" -> todayQuote.hindi
            "mr" -> todayQuote.marathi
            else -> todayQuote.english
        }

        // Layouts
        val collapsedView = RemoteViews(context.packageName, R.layout.custom_notification_collapsed)
        val expandedView = RemoteViews(context.packageName, R.layout.custom_notification_expanded)

        // Bind collapsed view data
        collapsedView.setTextViewText(R.id.notification_title, title)
        collapsedView.setTextViewText(R.id.notification_message, message)
        collapsedView.setProgressBar(R.id.notification_progress, 100, averageProgress, false)

        // Bind expanded view data
        expandedView.setTextViewText(R.id.notification_title, title)
        expandedView.setTextViewText(R.id.notification_message, message)
        expandedView.setTextViewText(R.id.notification_badge, badgeText.uppercase(java.util.Locale.getDefault()))
        expandedView.setProgressBar(R.id.notification_progress, 100, averageProgress, false)
        expandedView.setTextViewText(R.id.notification_progress_text, "$averageProgress%")

        // Stats binding — text labels
        expandedView.setTextViewText(R.id.notification_stat_steps, "$steps/$stepGoal")
        expandedView.setTextViewText(R.id.notification_stat_water, "$water/$waterGoal")
        expandedView.setTextViewText(R.id.notification_stat_sleep, String.format(java.util.Locale.US, "%.1f/%.1f", sleep, sleepGoal))

        // Smart Progress Bars — individual per metric
        val stepPct = (stepProgress.coerceAtMost(1f) * 100f).toInt()
        val waterPct = (waterProgress.coerceAtMost(1f) * 100f).toInt()
        val sleepPct = (sleepProgress.coerceAtMost(1f) * 100f).toInt()
        expandedView.setProgressBar(R.id.notif_progress_steps, 100, stepPct, false)
        expandedView.setProgressBar(R.id.notif_progress_water, 100, waterPct, false)
        expandedView.setProgressBar(R.id.notif_progress_sleep, 100, sleepPct, false)

        // Quote binding
        expandedView.setTextViewText(R.id.notification_quote_text, "\"" + quoteText + "\"")

        // Setup PendingIntents with dynamic destination route
        val targetRoute = navigateTo?.ifBlank { "DASHBOARD" } ?: "DASHBOARD"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", targetRoute)
            putExtra("NAVIGATE_TO", targetRoute)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open App Button click
        expandedView.setOnClickPendingIntent(R.id.notification_action_open, pendingIntent)

        // Log Water button click (sends broadcast to NotificationActionReceiver)
        val waterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DRANK_WATER"
            putExtra("notification_id", notificationId)
        }
        val waterPendingIntent = PendingIntent.getBroadcast(
            context, notificationId, waterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        expandedView.setOnClickPendingIntent(R.id.notification_action_water, waterPendingIntent)

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Handle permission issue
        }
    }
}
