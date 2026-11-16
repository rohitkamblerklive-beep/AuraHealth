package com.pro.aifitnesstracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pro.aifitnesstracker.AppDatabase
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefsManager = SharedPrefsManager(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val stepRecord = db.healthDao().getStepsForDate(today)
            val waterRecord = db.healthDao().getWaterForDate(today)
            
            val stepsToday = stepRecord?.steps ?: 0
            val waterToday = waterRecord?.glasses ?: 0
            
            val stepGoal = prefsManager.getStepGoal()
            val waterGoal = prefsManager.getWaterGoal()
            
            val stepsUnfinished = stepsToday < stepGoal
            val waterUnfinished = waterToday < waterGoal
            
            if (stepsUnfinished || waterUnfinished) {
                // Build alert message
                val title = "Daily Goals Reminder"
                val body = when {
                    stepsUnfinished && waterUnfinished -> 
                        "You're almost there! Don't forget to complete your steps ($stepsToday/$stepGoal) and water ($waterToday/$waterGoal glasses) goals!"
                    stepsUnfinished -> 
                        "Keep moving! You have $stepsToday of $stepGoal steps completed. Let's finish it!"
                    else -> 
                        "Stay hydrated! You have logged $waterToday of $waterGoal glasses of water. Take a sip!"
                }
                
                val target = if (stepsUnfinished) "STEP_DETAIL" else "WATER"
                showReminderNotification(context, title, body, target)
            }
        }
    }

    private fun showReminderNotification(context: Context, title: String, message: String, targetRoute: String) {
        val helper = com.pro.aifitnesstracker.NotificationHelper(context)
        helper.showCustomNotification(
            title = title,
            message = message,
            badgeText = "REMINDER",
            notificationId = 2002,
            channelId = "daily_goal_reminder_channel",
            channelName = "Daily Goal Reminders",
            navigateTo = targetRoute
        )
    }
}
