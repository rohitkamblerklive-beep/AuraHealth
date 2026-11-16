package com.pro.aifitnesstracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val type = inputData.getString("type") ?: "water"
        val channelId = if (type == "water") "water_reminders" else "fitness_reminders"
        val channelName = if (type == "water") "Water Reminders" else "Fitness Reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = if (type == "water") {
            com.pro.aifitnesstracker.utils.WaterSoundGenerator.getWaterDropSoundUri(applicationContext)
        } else {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }

        try {
            applicationContext.grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Create Notification Channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { 
                description = if (type == "water") "Reminders to stay hydrated" else "Daily fitness reminders"
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val prefs = SharedPrefsManager(applicationContext)
        
        // Check if disabled by user
        if (type == "water" && !prefs.isWaterReminderEnabled()) {
            return Result.success()
        }

        val isHindi = prefs.getLanguage() == "hi"

        val (title, message) = when (type) {
            "water" -> {
                val water = prefs.getWaterGlassesToday()
                val waterGoal = prefs.getWaterGoal()
                if (water >= waterGoal) {
                    Pair(
                        if (isHindi) "💧 लक्ष्य पूरा हुआ!" else "💧 Goal Achieved!",
                        if (isHindi) "बधाई हो! आपने आज का पानी का लक्ष्य ($waterGoal गिलास) पूरा कर लिया है।" else "Congratulations! You have met your hydration goal of $waterGoal glasses today."
                    )
                } else {
                    Pair(
                        if (isHindi) "💧 पानी पीने का समय!" else "💧 Time to hydrate!",
                        if (isHindi) "आज आपने $water गिलास पानी पिया है। लक्ष्य पूरा करने के लिए ${waterGoal - water} गिलास और पिएं!" else "You've drank $water glasses today. Drink ${waterGoal - water} more glasses to reach your goal!"
                    )
                }
            }
            "workout" -> {
                val streak = prefs.getCurrentStreak()
                Pair(
                    if (isHindi) "🏋️ वर्कआउट का समय!" else "🏋️ Workout Time!",
                    if (isHindi) "अपने $streak दिनों की स्ट्राइक को बनाए रखें। आज का वर्कआउट अभी शुरू करें!" else "Keep your $streak-day streak alive. Start today's workout now!"
                )
            }
            "steps" -> {
                val steps = prefs.getStepsToday()
                val stepGoal = prefs.getStepGoal()
                if (steps >= stepGoal) {
                    Pair(
                        if (isHindi) "👣 कदम लक्ष्य पूरा! 🎉" else "👣 Steps Goal Achieved! 🎉",
                        if (isHindi) "शानदार! आपने आज अपने $stepGoal कदम का लक्ष्य पूरा कर लिया है।" else "Amazing! You have completed your step goal of $stepGoal steps today."
                    )
                } else {
                    Pair(
                        if (isHindi) "👣 कदम बढ़ाएं!" else "👣 Keep Moving!",
                        if (isHindi) "आज आपने $steps कदम चले हैं। $stepGoal कदमों का लक्ष्य पूरा करने के लिए ${stepGoal - steps} कदम और चलें!" else "You have walked $steps steps today. Just ${stepGoal - steps} steps left to reach your goal of $stepGoal!"
                    )
                }
            }
            else -> Pair(
                "🌟 AuraHealth",
                if (isHindi) "अपनी सेहत का ख्याल रखें। ऐप खोलें!" else "Take care of your health. Open the app!"
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.app_logo))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add Progress Bar for Water
        if (type == "water") {
            val glasses = prefs.getWaterGlassesToday()
            val progress = (glasses * 100 / 8).coerceIn(0, 100)
            notificationBuilder.setProgress(100, progress, false)
        }

        // Add Action Button for Water
        if (type == "water") {
            val actionIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DRANK_WATER"
                putExtra("notification_id", type.hashCode())
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                applicationContext, 
                1, 
                actionIntent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val actionTitle = if (isHindi) "मैंने 1 गिलास पी लिया ✅" else "I Drank 1 Glass ✅"
            notificationBuilder.addAction(0, actionTitle, actionPendingIntent)
        }

        val notification = notificationBuilder.build()

        manager.notify(type.hashCode(), notification)
        return Result.success()
    }
}
