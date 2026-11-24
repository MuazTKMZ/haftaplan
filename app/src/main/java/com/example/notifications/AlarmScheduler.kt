package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.PlanSettings
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask

object AlarmScheduler {
    private const val MORNING_REQUEST_CODE = 9001

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun rescheduleMorningAlarm(context: Context, settings: PlanSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MorningSummaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.morningSummaryEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAt = WeekDateUtils.nextMorningAlarmMillis(
            settings.morningHour.toInt(),
            settings.morningMinute.toInt()
        )
        scheduleSafe(alarmManager, triggerAt, pendingIntent)
    }

    fun rescheduleTaskReminders(context: Context, tasks: List<WeeklyTask>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentWeek = WeekDateUtils.currentWeekKey()

        tasks.forEach { task ->
            val reqCode = ("reminder_" + task.id).hashCode()
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
                putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
                putExtra(TaskReminderReceiver.EXTRA_TASK_CATEGORY, task.categoryEnum.displayName)
                putExtra(TaskReminderReceiver.EXTRA_TASK_TIME, task.formattedReminderTime() ?: "")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val hour = task.reminderHour?.toInt()
            val minute = task.reminderMinute?.toInt()
            if (task.weekKey != currentWeek || task.completed || !task.reminderEnabled || hour == null || minute == null) {
                alarmManager.cancel(pendingIntent)
            } else {
                val triggerAt = WeekDateUtils.taskReminderMillisInWeek(
                    weekKey = task.weekKey,
                    dayOfWeek = task.dayInt,
                    hour = hour,
                    minute = minute
                )
                if (triggerAt != null) {
                    scheduleSafe(alarmManager, triggerAt, pendingIntent)
                } else {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    private fun scheduleSafe(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
