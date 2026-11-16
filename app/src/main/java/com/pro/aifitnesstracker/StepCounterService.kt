package com.pro.aifitnesstracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepCounterService : Service(), SensorEventListener {

    private val CHANNEL_ID      = "step_counter_service"
    private val CHANNEL_NAME    = "Background Step Counter"
    private val NOTIFICATION_ID = 1002
    private val MILESTONE_CHANNEL_ID   = "step_milestone_channel"
    private val MILESTONE_CHANNEL_NAME = "Step Milestones"
    private val MILESTONE_NOTIFICATION_ID = 1003

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private lateinit var prefsManager: SharedPrefsManager

    private var lastNotificationSteps = -1
    private var lastStepEventTime = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastAccelMagnitude = 0f
    private var isAccelPeak = false
    private var lastAccelStepTime = 0L
    private var lastDbSaveSteps = -1

    // Dual-Layer Vehicle & Cadence Filter
    private var isVehicleSpeedDetected = false
    private val recentStepTimestamps = java.util.ArrayDeque<Long>()
    private var locationManager: android.location.LocationManager? = null
    private val speedLocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            if (location.hasSpeed()) {
                val speedKmh = location.speed * 3.6f
                isVehicleSpeedDetected = speedKmh > 18.0f
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun isStepCadenceValid(now: Long): Boolean {
        if (isVehicleSpeedDetected) return false
        while (recentStepTimestamps.isNotEmpty() && (now - recentStepTimestamps.first) > 2500L) {
            recentStepTimestamps.removeFirst()
        }
        val lastTime = if (recentStepTimestamps.isNotEmpty()) recentStepTimestamps.last else 0L
        if (lastTime > 0L && (now - lastTime) < 260L) {
            return false // High-frequency vehicle/engine vibration rejected
        }
        recentStepTimestamps.addLast(now)
        return true
    }

    private fun initSpeedGate() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                val provider = if (locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true) {
                    android.location.LocationManager.GPS_PROVIDER
                } else {
                    android.location.LocationManager.NETWORK_PROVIDER
                }
                locationManager?.requestLocationUpdates(
                    provider,
                    5000L,
                    15f,
                    speedLocationListener,
                    android.os.Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistStepsToDatabase(steps: Int, delta: Int = 1, force: Boolean = false) {
        if (!force && (steps - lastDbSaveSteps) < 10) return
        lastDbSaveSteps = steps

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val db = AppDatabase.getDatabase(applicationContext)
                db.healthDao().insertStepRecord(StepRecord(today, steps))

                // Record into StepTrackerRepository (Room entity, walking session, streaks, achievements)
                com.pro.aifitnesstracker.data.step.StepTrackerRepository.getInstance(applicationContext).recordStepEvent(steps, delta)

                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    FirebaseSyncManager.syncSingleStep(currentUser.uid, today, steps)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val dateChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_DATE_CHANGED || 
                action == Intent.ACTION_TIME_CHANGED || 
                action == Intent.ACTION_TIMEZONE_CHANGED) {
                
                if (prefsManager.checkIfNewDay()) {
                    prefsManager.resetDay()
                    prefsManager.saveLastSensorValue(-1)
                    updateNotification(0, true)
                    sendBroadcast(Intent("com.pro.aifitnesstracker.STEP_UPDATE").apply {
                        setPackage(packageName)
                        putExtra("steps", 0)
                    })
                }
            }
        }
    }

    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.pro.aifitnesstracker.PAUSE_TRACKING" -> {
                    prefsManager.setStepTrackingPaused(true)
                    updateNotification(prefsManager.getStepsToday(), force = true)
                }
                "com.pro.aifitnesstracker.RESUME_TRACKING" -> {
                    prefsManager.setStepTrackingPaused(false)
                    updateNotification(prefsManager.getStepsToday(), force = true)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = SharedPrefsManager(this)
        createNotificationChannels()
        initSpeedGate()
        startServiceForeground()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize WakeLock for transient operations without holding it permanently (prevents severe battery drain)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AuraHealth::StepCounterWakeLock").apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stepSensor?.let {
            try {
                // UI delay (~16-20 Hz) is optimal for human cadence while saving significant battery compared to GAME delay
                val delay = SensorManager.SENSOR_DELAY_UI
                sensorManager?.registerListener(this, it, delay)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val actionFilter = android.content.IntentFilter().apply {
            addAction("com.pro.aifitnesstracker.PAUSE_TRACKING")
            addAction("com.pro.aifitnesstracker.RESUME_TRACKING")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(dateChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(actionReceiver, actionFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(dateChangeReceiver, filter)
                registerReceiver(actionReceiver, actionFilter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Schedule reliable midnight alarm for day reset (works in Doze mode)
        try {
            MidnightResetReceiver.scheduleMidnightAlarm(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check if day changed while service was stopped (e.g. overnight)
        try {
            if (prefsManager.checkIfNewDay()) {
                prefsManager.resetDay()
                prefsManager.saveLastSensorValue(-1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServiceForeground()
        // Handle midnight reset action from MidnightResetReceiver
        if (intent?.action == "com.pro.aifitnesstracker.ACTION_MIDNIGHT_RESET") {
            lastNotificationSteps = -1
            updateNotification(0, force = true)
        }
        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps background step counting active"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(MILESTONE_CHANNEL_ID, MILESTONE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifies when you hit step goal milestones"
                }
            )
        }
    }

    private fun buildStepNotification(steps: Int, goal: Int): Notification {
        val pct = if (goal > 0) (steps * 100 / goal) else 0
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val isPaused = prefsManager.isStepTrackingPaused()

        val emoji = when {
            isPaused -> "⏸"
            pct >= 100 -> "🏆"
            pct >= 90  -> "🔥"
            pct >= 50  -> "💪"
            pct >= 25  -> "👣"
            else       -> "🚶"
        }
        
        val message = when {
            isPaused -> "Tracking paused. Tap Resume to continue."
            pct >= 100 -> "Goal achieved! Excellent work today!"
            pct >= 90 -> "Almost there! Keep pushing!"
            pct >= 50 -> "Halfway there! Keep it up!"
            else -> "Every step counts! You're doing great."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_logo))
            .setContentTitle("$emoji $steps / $goal steps")
            .setContentText(message)
            .setProgress(goal, steps.coerceAtMost(goal), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (isPaused) {
            val resumeIntent = Intent("com.pro.aifitnesstracker.RESUME_TRACKING").apply { setPackage(packageName) }
            val resumePending = PendingIntent.getBroadcast(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume Tracking", resumePending)
        } else {
            val pauseIntent = Intent("com.pro.aifitnesstracker.PAUSE_TRACKING").apply { setPackage(packageName) }
            val pausePending = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause Tracking", pausePending)
        }

        return builder.build()
    }

    private fun startServiceForeground() {
        try {
            val steps = if (::prefsManager.isInitialized) prefsManager.getStepsToday() else 0
            val goal  = if (::prefsManager.isInitialized) prefsManager.getStepGoal() else 6000
            val notif = buildStepNotification(steps, goal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
                } catch (e: Throwable) {
                    startForeground(NOTIFICATION_ID, notif)
                }
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
            lastNotificationSteps = steps
        } catch (e: Throwable) {
            e.printStackTrace()
            try {
                val fallback = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("AuraHealth Active")
                    .setContentText("Step counter running")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()
                startForeground(NOTIFICATION_ID, fallback)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun updateNotification(steps: Int, force: Boolean = false) {
        // Live updates every 10 steps for a real-time feel
        if (!force && lastNotificationSteps >= 0 && (steps - lastNotificationSteps) < 10 && steps < prefsManager.getStepGoal()) return
        lastNotificationSteps = steps
        val goal  = prefsManager.getStepGoal()
        val nm    = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildStepNotification(steps, goal))
        
        // Update Glance Widget live
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.pro.aifitnesstracker.ui.widget.FitnessWidget().updateAll(this@StepCounterService)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAndFireMilestone(steps: Int, goal: Int) {
        if (goal <= 0) return
        val pct = steps * 100 / goal
        val lastMilestone = prefsManager.getLastMilestonePct()

        val milestone = when {
            pct >= 100 && lastMilestone < 100 -> Triple(100, "🏆 Goal Achieved!", "You walked $steps steps today!")
            pct >= 75  && lastMilestone < 75  -> Triple(75,  "🔥 75% Done!",      "${goal - steps} steps left to reach your goal!")
            pct >= 50  && lastMilestone < 50  -> Triple(50,  "💪 Halfway There!", "Keep going — $steps steps so far!")
            pct >= 25  && lastMilestone < 25  -> Triple(25,  "👣 Good Start!",    "25% of your goal reached — $steps steps!")
            else -> null
        } ?: return

        prefsManager.saveLastMilestonePct(milestone.first)

        // Vibrate once on Goal Achieved (100%)
        if (milestone.first == 100) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, milestone.first, intent, PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, MILESTONE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_logo))
            .setContentTitle(milestone.second)
            .setContentText(milestone.third)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(MILESTONE_NOTIFICATION_ID, notif)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt()
                val lastValue = prefsManager.getLastSensorValue()
                val isNewDay  = prefsManager.checkIfNewDay()

                if (lastValue == -1 || isNewDay) {
                    if (isNewDay) prefsManager.resetDay()
                    prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                    updateNotification(0, true)
                    return
                }

                val delta = if (totalStepsSinceBoot < lastValue) {
                    // Device rebooted, sensor reset to 0
                    totalStepsSinceBoot
                } else {
                    totalStepsSinceBoot - lastValue
                }

                if (delta > 0) {
                    if (isVehicleSpeedDetected) {
                        prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                        return
                    }
                    // If tracking is paused, ignore steps and sync base sensor value
                    if (prefsManager.isStepTrackingPaused()) {
                        prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                        return
                    }

                    val currentStepsToday = prefsManager.getStepsToday()
                    val updatedSteps = currentStepsToday + delta

                    prefsManager.saveStepsToday(updatedSteps)
                    prefsManager.saveLastSensorValue(totalStepsSinceBoot)
                    prefsManager.addStepsToCurrentHour(delta)

                    updateNotification(updatedSteps)
                    checkAndFireMilestone(updatedSteps, prefsManager.getStepGoal())
                    persistStepsToDatabase(updatedSteps, delta = delta)

                    sendBroadcast(Intent("com.pro.aifitnesstracker.STEP_UPDATE").apply {
                        setPackage(packageName)
                        putExtra("steps", updatedSteps)
                    })
                }
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    if (prefsManager.isStepTrackingPaused()) return

                    val now = System.currentTimeMillis()
                    if (!isStepCadenceValid(now)) return
                    // Minimum 300ms between footsteps to eliminate false double-triggers
                    if (now - lastStepEventTime < 300L) {
                        return
                    }
                    lastStepEventTime = now

                    if (prefsManager.checkIfNewDay()) {
                        prefsManager.resetDay()
                    }

                    val currentStepsToday = prefsManager.getStepsToday()
                    val updatedSteps = currentStepsToday + 1

                    prefsManager.saveStepsToday(updatedSteps)
                    prefsManager.addStepsToCurrentHour(1)

                    updateNotification(updatedSteps)
                    checkAndFireMilestone(updatedSteps, prefsManager.getStepGoal())
                    persistStepsToDatabase(updatedSteps)

                    sendBroadcast(Intent("com.pro.aifitnesstracker.STEP_UPDATE").apply {
                        setPackage(packageName)
                        putExtra("steps", updatedSteps)
                    })
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (prefsManager.isStepTrackingPaused()) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                if (!isStepCadenceValid(System.currentTimeMillis())) return
                val currentTime = System.currentTimeMillis()

                // Peak detection: acceleration magnitude spike caused by human foot strike
                if (magnitude > 12.0f && (magnitude - lastAccelMagnitude) > 1.3f) {
                    if (!isAccelPeak && (currentTime - lastAccelStepTime) > 320L) {
                        isAccelPeak = true
                        lastAccelStepTime = currentTime

                        if (prefsManager.checkIfNewDay()) {
                            prefsManager.resetDay()
                        }

                        val currentStepsToday = prefsManager.getStepsToday()
                        val updatedSteps = currentStepsToday + 1

                        prefsManager.saveStepsToday(updatedSteps)
                        prefsManager.addStepsToCurrentHour(1)

                        updateNotification(updatedSteps)
                        checkAndFireMilestone(updatedSteps, prefsManager.getStepGoal())
                        persistStepsToDatabase(updatedSteps)

                        sendBroadcast(Intent("com.pro.aifitnesstracker.STEP_UPDATE").apply {
                            setPackage(packageName)
                            putExtra("steps", updatedSteps)
                        })
                    }
                } else if (magnitude < 10.0f) {
                    isAccelPeak = false
                }
                lastAccelMagnitude = magnitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        persistStepsToDatabase(prefsManager.getStepsToday(), force = true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.pro.aifitnesstracker.data.step.StepTrackerRepository.getInstance(applicationContext).flushActiveSession()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try { locationManager?.removeUpdates(speedLocationListener) } catch (_: Exception) {}
        sensorManager?.unregisterListener(this)
        try {
            unregisterReceiver(dateChangeReceiver)
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            // receiver may not be registered
        }

        // Release CPU WakeLock
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }
}
