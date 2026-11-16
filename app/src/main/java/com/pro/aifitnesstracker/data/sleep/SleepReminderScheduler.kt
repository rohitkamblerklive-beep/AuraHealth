package com.pro.aifitnesstracker.data.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pro.aifitnesstracker.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object SleepReminderScheduler {

    suspend fun reschedule(context: Context) = withContext(Dispatchers.IO) {
        val schedule = AppDatabase.getDatabase(context).sleepTrackerDao().getScheduleSync()
            ?: SleepScheduleEntity()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@withContext

        // 1. Bedtime Reminder
        if (schedule.bedtimeReminderEnabled) {
            scheduleBedtimeAlarm(context, alarmManager, schedule)
        } else {
            cancelAlarm(context, alarmManager, SleepReminderReceiver.TYPE_BEDTIME, 101)
        }

        // 2. Wakeup Reminder
        if (schedule.wakeupReminderEnabled) {
            scheduleWakeupAlarm(context, alarmManager, schedule)
        } else {
            cancelAlarm(context, alarmManager, SleepReminderReceiver.TYPE_WAKEUP, 102)
        }
    }

    private fun scheduleBedtimeAlarm(
        context: Context,
        alarmManager: AlarmManager,
        schedule: SleepScheduleEntity
    ) {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()

        // Check if today is weekend
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && schedule.isWeekendDifferent

        val hour = if (isWeekend) schedule.weekendBedtimeHour else schedule.weekdayBedtimeHour
        val minute = if (isWeekend) schedule.weekendBedtimeMinute else schedule.weekdayBedtimeMinute

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Subtract lead reminder minutes
        calendar.add(Calendar.MINUTE, -schedule.reminderLeadMinutes)

        // If time already passed today, advance by 1 day
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            putExtra(SleepReminderReceiver.EXTRA_TYPE, SleepReminderReceiver.TYPE_BEDTIME)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun scheduleWakeupAlarm(
        context: Context,
        alarmManager: AlarmManager,
        schedule: SleepScheduleEntity
    ) {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()

        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && schedule.isWeekendDifferent

        val hour = if (isWeekend) schedule.weekendWakeupHour else schedule.weekdayWakeupHour
        val minute = if (isWeekend) schedule.weekendWakeupMinute else schedule.weekdayWakeupMinute

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            putExtra(SleepReminderReceiver.EXTRA_TYPE, SleepReminderReceiver.TYPE_WAKEUP)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, type: String, requestCode: Int) {
        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            putExtra(SleepReminderReceiver.EXTRA_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
