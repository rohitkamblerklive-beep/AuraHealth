package com.pro.aifitnesstracker.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.pro.aifitnesstracker.MainActivity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LocationTrackingService : Service(), LocationListener, SensorEventListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        const val ACTION_BROADCAST = "com.pro.aifitnesstracker.LOCATION_UPDATE"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
        const val EXTRA_DISTANCE_METERS = "EXTRA_DISTANCE_METERS"
        const val EXTRA_STEPS = "EXTRA_STEPS"
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val EXTRA_SPEED = "EXTRA_SPEED"

        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "location_tracking_channel"

        var isServiceRunning: Boolean = false
            private set
    }

    private var locationManager: LocationManager? = null
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private var initialStepCount = -1
    private var totalSteps = 0
    private var totalDistanceMeters = 0f
    private var lastLocation: Location? = null
    private var durationSeconds = 0

    private var scheduler: ScheduledExecutorService? = null
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        isServiceRunning = true

        initialStepCount = -1
        totalSteps = 0
        totalDistanceMeters = 0f
        lastLocation = null
        durationSeconds = 0

        // Start Foreground Service with notification and proper location type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        // Register Location Updates (prefer GPS, fallback to Network)
        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, // 2 seconds
                    2f, // 2 meters
                    this
                )
            } else {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2f,
                    this
                )
            }
        } catch (e: SecurityException) {
            // No permissions granted
        }

        // Register Step Sensor
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Timer scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler?.scheduleAtFixedRate({
            durationSeconds++
            updateNotification()
            broadcastUpdate()
        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        isServiceRunning = false

        scheduler?.shutdownNow()
        scheduler = null

        locationManager?.removeUpdates(this)
        sensorManager?.unregisterListener(this)

        stopForeground(true)
        stopSelf()
    }

    // LocationListener Callbacks
    override fun onLocationChanged(location: Location) {
        val lastLoc = lastLocation
        if (lastLoc != null) {
            val deltaDistance = lastLoc.distanceTo(location)
            // Filter out gps noise (jumps too large or speed impossible for human runner)
            if (deltaDistance > 0.5f && deltaDistance < 50f) {
                totalDistanceMeters += deltaDistance
                // Fallback step calculation if step sensor isn't firing
                if (stepSensor == null) {
                    totalSteps = (totalDistanceMeters * 1.31f).toInt()
                }
            }
        }
        lastLocation = location
        broadcastUpdate()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    // SensorEventListener Callbacks
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val stepsVal = event.values[0].toInt()
            if (initialStepCount == -1) {
                initialStepCount = stepsVal
            }
            totalSteps = stepsVal - initialStepCount
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_BROADCAST).apply {
            setPackage(packageName)
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(EXTRA_DISTANCE_METERS, totalDistanceMeters)
            putExtra(EXTRA_STEPS, totalSteps)
            lastLocation?.let {
                putExtra(EXTRA_LATITUDE, it.latitude)
                putExtra(EXTRA_LONGITUDE, it.longitude)
                putExtra(EXTRA_SPEED, it.speed)
            }
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(): Notification {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        val distanceKm = totalDistanceMeters / 1000f
        val distanceStr = String.format("%.2f km", distanceKm)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Active Run Tracking")
            .setContentText("Duration: $timeStr | Distance: $distanceStr")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Run Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications during location-based run tracking."
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }
}
