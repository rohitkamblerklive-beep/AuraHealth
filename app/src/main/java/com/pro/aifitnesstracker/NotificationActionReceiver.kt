package com.pro.aifitnesstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val prefsManager = SharedPrefsManager(context)

        if (action == "ACTION_DRANK_WATER") {
            // Increment water glasses
            val currentWater = prefsManager.getWaterGlassesToday()
            val updatedWater = currentWater + 1
            prefsManager.saveWaterGlassesToday(updatedWater)
            
            // Sync to Room DB, WaterTrackerRepository, and Cloud
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.healthDao().insertWaterRecord(WaterRecord(today, updatedWater))
                    db.waterTrackerDao().insertEntry(
                        com.pro.aifitnesstracker.data.water.WaterIntakeEntry(
                            amountMl = 250,
                            drinkType = "WATER",
                            date = today,
                            time = time,
                            containerName = "Regular Glass",
                            note = "Logged via notification",
                            hydrationFactor = 1.0f
                        )
                    )
                    prefsManager.saveWaterMlToday(updatedWater * 250)
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        FirebaseSyncManager.syncSingleWater(currentUser.uid, today, updatedWater)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Check badges
            if (updatedWater >= 8) {
                prefsManager.unlockBadge("WATER_HERO")
            }

            // Re-post notification dynamically with the updated statistics
            val notificationHelper = NotificationHelper(context)
            val strings = getLanguageStrings(prefsManager.getLanguage())
            val waterGoal = prefsManager.getWaterGoal()
            
            val title = strings.waterNudgeTitle
            val message = strings.freeWaterMsg.format(currentWater + 1, (waterGoal - (currentWater + 1)).coerceAtLeast(0), waterGoal)

            val notificationId = intent.getIntExtra("notification_id", 101)
            notificationHelper.showCustomNotification(
                title = title,
                message = message,
                badgeText = "LOGGED",
                notificationId = notificationId
            )
        }
    }
}
