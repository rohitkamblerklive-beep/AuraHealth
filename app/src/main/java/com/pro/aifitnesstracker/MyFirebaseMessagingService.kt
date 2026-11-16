package com.pro.aifitnesstracker

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed token: $token")
        
        // Save token to SharedPreferences
        val prefs = SharedPrefsManager(this)
        prefs.saveFcmToken(token)
        
        // Sync to Firestore if user is logged in
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            FirebaseSyncManager.syncFcmTokenToCloud(firebaseUser.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "Message received from: ${remoteMessage.from}")
        
        // Extract title and body
        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body
        
        // If message has data payload
        if (remoteMessage.data.isNotEmpty()) {
            if (title == null) title = remoteMessage.data["title"]
            if (body == null) body = remoteMessage.data["body"]
        }
        
        // Extract deep link destination if present
        val navigateTo = remoteMessage.data["navigate_to"] 
            ?: remoteMessage.data["NAVIGATE_TO"] 
            ?: remoteMessage.data["screen"] 
            ?: remoteMessage.data["route"]

        // Display using NotificationHelper with dynamic deep link
        if (title != null || body != null) {
            val finalTitle = title ?: "AuraHealth"
            val finalBody = body ?: ""
            NotificationHelper(this).showCustomNotification(
                title = finalTitle,
                message = finalBody,
                badgeText = "UPDATE",
                notificationId = (System.currentTimeMillis() % 10000).toInt(),
                navigateTo = navigateTo
            )
        }
    }
}
