package com.pro.aifitnesstracker.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.NotificationActionReceiver
import com.pro.aifitnesstracker.NotificationHelper
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.SharedPrefsManager
import java.util.concurrent.TimeUnit

/**
 * SmartNotificationWorker — Context-Aware Health Nudges
 *
 * Fires every 30 minutes to evaluate the user's real-time health context
 * and sends a targeted, clean native notification only when genuinely needed.
 *
 * Smart Triggers:
 *  1. 💧 Hydration Gap   — No water logged in last 2+ hours
 *  2. 🚶 Inactivity      — Steps < 30% of goal by afternoon (after 2 PM)
 *  3. 🧘 Yoga Reminder   — No session started, reminder window (7–9 AM or 6–8 PM)
 *  4. 🔥 Step Milestone  — Just crossed 25%, 50%, 75% of step goal
 *  5. 🏆 Streak Risk     — Goal unmet and it's after 9 PM
 */
class SmartNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID_SMART = "smart_nudges"
        const val CHANNEL_NAME_SMART = "Smart Health Nudges"

        // Notification IDs per type (stable — Android deduplicates by ID)
        const val NOTIF_ID_HYDRATION = 2001
        const val NOTIF_ID_STEPS = 2002
        const val NOTIF_ID_YOGA = 2003
        const val NOTIF_ID_MILESTONE = 2004
        const val NOTIF_ID_STREAK = 2005

        // SharedPrefs key to track last water log timestamp
        const val KEY_LAST_WATER_NOTIF = "last_water_notif_time"
        const val KEY_LAST_STEP_MILESTONE = "last_step_milestone_pct"

        /** Schedules this worker to run every 30 minutes via WorkManager */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .addTag("smart_notification_worker")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SmartNotificationWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancel all smart nudge scheduling */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("SmartNotificationWorker")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = SharedPrefsManager(applicationContext)

        // Respect user's global notification preference
        if (!prefs.isDailyReminderEnabled()) return Result.success()

        ensureChannel()

        val nowMs = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        // ── 1. HYDRATION GAP NUDGE ────────────────────────────────────────
        val lastWaterNotifTime = prefs.getLong(KEY_LAST_WATER_NOTIF, 0L)
        val msSinceLastWaterNotif = nowMs - lastWaterNotifTime
        val waterGlasses = prefs.getWaterGlassesToday()
        val waterGoal = prefs.getWaterGoal().coerceAtLeast(1)

        if (waterGlasses < waterGoal && msSinceLastWaterNotif > TimeUnit.HOURS.toMillis(2)) {
            val remaining = waterGoal - waterGlasses
            sendHydrationNudge(waterGlasses, waterGoal, remaining)
            prefs.putLong(KEY_LAST_WATER_NOTIF, nowMs)
            return Result.success() // One nudge per check cycle
        }

        // ── 2. INACTIVITY / STEP NUDGE (afternoon check: 14:00–20:00) ────
        if (hour in 14..20) {
            val steps = prefs.getStepsToday()
            val stepGoal = prefs.getStepGoal().coerceAtLeast(1)
            val stepPct = steps.toFloat() / stepGoal

            if (stepPct < 0.3f) {
                sendInactivityNudge(steps, stepGoal)
                return Result.success()
            }
        }

        // ── 3. YOGA SESSION REMINDER (morning 7–9, evening 18–20) ────────
        if (hour in 7..9 || hour in 18..20) {
            val lastYogaDate = prefs.getString("last_yoga_session_date", "")
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            if (lastYogaDate != todayDate) {
                sendYogaReminder(prefs)
                return Result.success()
            }
        }

        // ── 4. STEP MILESTONE CELEBRATION ────────────────────────────────
        val steps = prefs.getStepsToday()
        val stepGoal = prefs.getStepGoal().coerceAtLeast(1)
        val stepPct = (steps.toFloat() / stepGoal * 100).toInt()
        val lastMilestonePct = prefs.getInt(KEY_LAST_STEP_MILESTONE, 0)

        val milestone = when {
            stepPct >= 75 && lastMilestonePct < 75 -> 75
            stepPct >= 50 && lastMilestonePct < 50 -> 50
            stepPct >= 25 && lastMilestonePct < 25 -> 25
            else -> 0
        }
        if (milestone > 0) {
            sendMilestoneNudge(steps, stepGoal, milestone)
            prefs.putInt(KEY_LAST_STEP_MILESTONE, milestone)
            return Result.success()
        }

        // ── 5. STREAK RISK (after 21:00, goal unmet) ─────────────────────
        if (hour >= 21) {
            val streak = prefs.getCurrentStreak()
            val goalMet = steps >= stepGoal && waterGlasses >= waterGoal
            if (!goalMet && streak > 0) {
                sendStreakRiskNudge(streak)
            }
        }

        return Result.success()
    }

    // ─────────────────────────────────────────────────────────────────────
    // NOTIFICATION BUILDERS — Native Android, Clean & Minimal
    // ─────────────────────────────────────────────────────────────────────

    private fun sendHydrationNudge(glasses: Int, goal: Int, remaining: Int) {
        val progressPct = (glasses.toFloat() / goal * 100).toInt()
        val builder = baseBuilder(
            title = "💧 Time to Hydrate",
            message = "You've had $glasses/$goal glasses today. Drink $remaining more to hit your goal!",
            notifId = NOTIF_ID_HYDRATION
        )
            .setProgress(100, progressPct, false)
            .addAction(buildWaterAction())
            .setColor(0xFF06B6D4.toInt()) // Cyan

        notify(NOTIF_ID_HYDRATION, builder)
    }

    private fun sendInactivityNudge(steps: Int, goal: Int) {
        val progressPct = (steps.toFloat() / goal * 100).toInt()
        val builder = baseBuilder(
            title = "🚶 Time to Move!",
            message = "Only $steps/$goal steps — you're at $progressPct%. A short walk will get you back on track.",
            notifId = NOTIF_ID_STEPS
        )
            .setProgress(100, progressPct, false)
            .setColor(0xFF10B981.toInt()) // Emerald

        notify(NOTIF_ID_STEPS, builder)
    }

    private fun sendYogaReminder(prefs: SharedPrefsManager) {
        val journeyId = prefs.getString("active_journey_id", "morning_flow") ?: "morning_flow"
        val yogaName = when (journeyId) {
            "mental_calm" -> "Mental Calm Flow"
            "energy_boost" -> "Energy Booster"
            "healing_detox" -> "Healing & Detox"
            else -> "Morning Flow"
        }
        val builder = baseBuilder(
            title = "🧘 Yoga Time — $yogaName",
            message = "Your 15-min session is ready. Start now to stay consistent with your wellness journey.",
            notifId = NOTIF_ID_YOGA,
            navigateTo = "YOGA_HUB"
        )
            .addAction(buildOpenAction("▶ Start Session", "YOGA_HUB"))
            .setColor(0xFF8B5CF6.toInt()) // Purple

        notify(NOTIF_ID_YOGA, builder)
    }

    private fun sendMilestoneNudge(steps: Int, goal: Int, pct: Int) {
        val emoji = when (pct) {
            25 -> "🌱"
            50 -> "⚡"
            75 -> "🔥"
            else -> "🏆"
        }
        val builder = baseBuilder(
            title = "$emoji $pct% of Your Step Goal!",
            message = "Great work — $steps steps done, ${goal - steps} to go. Keep the momentum!"
        )
            .setProgress(100, pct, false)
            .setColor(0xFFF59E0B.toInt()) // Amber

        notify(NOTIF_ID_MILESTONE, builder)
    }

    private fun sendStreakRiskNudge(streak: Int) {
        val builder = baseBuilder(
            title = "⚠️ Don't Break Your $streak-Day Streak!",
            message = "You haven't met today's goals yet. Log now to keep your streak alive — just a few minutes!"
        )
            .setColor(0xFFEF4444.toInt()) // Red
            .addAction(buildWaterAction())

        notify(NOTIF_ID_STREAK, builder)
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private fun baseBuilder(
        title: String,
        message: String,
        notifId: Int = NOTIF_ID_HYDRATION,
        navigateTo: String = "DASHBOARD"
    ): NotificationCompat.Builder {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", navigateTo)
        }
        val pi = PendingIntent.getActivity(
            applicationContext, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = BitmapFactory.decodeResource(
            applicationContext.resources, R.drawable.app_logo
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID_SMART)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)         // No repeated sound for same notif ID
            .setGroup("aurahealth_smart")   // Group all smart nudges together
    }

    private fun buildWaterAction(): NotificationCompat.Action {
        val waterIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DRANK_WATER"
            putExtra("notification_id", NOTIF_ID_HYDRATION)
        }
        val waterPi = PendingIntent.getBroadcast(
            applicationContext, NOTIF_ID_HYDRATION, waterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, "💧 Log Glass", waterPi)
    }

    private fun buildOpenAction(label: String, route: String): NotificationCompat.Action {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", route)
        }
        val pi = PendingIntent.getActivity(
            applicationContext, route.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, label, pi)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID_SMART,
                CHANNEL_NAME_SMART,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Context-aware smart health nudges"
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silent fail
        }
    }
}
