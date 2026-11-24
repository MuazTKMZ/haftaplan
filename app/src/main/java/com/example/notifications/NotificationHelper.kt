package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LocalPlanCache
import com.example.data.WeekDateUtils

object NotificationHelper {
    const val CHANNEL_MORNING = "channel_morning_summary"
    const val CHANNEL_REMINDER = "channel_task_reminders"
    const val MORNING_NOTIFICATION_ID = 7301

    const val EXTRA_OPEN_BRIEFING = "extra_open_briefing"
    const val EXTRA_BRIEFING_MONDAY = "extra_briefing_monday"
    const val EXTRA_OPEN_ADD_TASK = "extra_open_add_task"
    const val EXTRA_TASK_DAY = "extra_task_day"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val morningChannel = NotificationChannel(
                CHANNEL_MORNING,
                context.getString(R.string.channel_morning_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_morning_desc)
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableVibration(true)
            }

            manager.createNotificationChannel(morningChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showMorningSummaryNotification(
        context: Context,
        forceMondayMode: Boolean? = null
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val settings = LocalPlanCache.getSettings(context)
        val weekTasks = LocalPlanCache.getCurrentWeekTasks(context)
        val todayDay = WeekDateUtils.todayDayOfWeek()
        val todayTasks = weekTasks.filter { it.dayInt == todayDay && !it.completed }
        val pendingWeekTasks = weekTasks.filter { !it.completed }
        val isMonday = forceMondayMode ?: (todayDay == 1 && settings.mondayWeeklyBriefingEnabled)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_BRIEFING, true)
            putExtra(EXTRA_BRIEFING_MONDAY, isMonday)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            MORNING_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
        val title: String
        val summaryText: String

        if (isMonday) {
            title = "Yeni Hafta Planın Hazır! (Pazartesi)"
            summaryText = if (pendingWeekTasks.isEmpty()) {
                "Bu hafta için henüz görev eklemedin. Haftanı planlamak için dokun."
            } else {
                "Bu hafta ${pendingWeekTasks.size} görev, bugün ${todayTasks.size} görev seni bekliyor."
            }
            inboxStyle.setBigContentTitle("Bu Hafta Yapılacaklar (${pendingWeekTasks.size})")
            if (pendingWeekTasks.isEmpty()) {
                inboxStyle.addLine("Haftalık planını oluşturmak için aç.")
            } else {
                pendingWeekTasks.take(6).forEach { task ->
                    val dayShort = WeekDateUtils.dayNameShort(task.dayInt)
                    inboxStyle.addLine("$dayShort • ${task.title}")
                }
                if (pendingWeekTasks.size > 6) {
                    inboxStyle.setSummaryText("+${pendingWeekTasks.size - 6} görev daha")
                }
            }
        } else {
            val dayName = WeekDateUtils.dayNameFull(todayDay)
            title = "Günaydın! $dayName Planın"
            summaryText = if (todayTasks.isEmpty()) {
                "Bugün için bekleyen görev yok. Yeni görev eklemek için dokun."
            } else {
                "Bugün yapman gereken ${todayTasks.size} görev var."
            }
            inboxStyle.setBigContentTitle("$dayName Görevlerin (${todayTasks.size})")
            if (todayTasks.isEmpty()) {
                inboxStyle.addLine("Bugün planlanmış aktif görev yok.")
            } else {
                todayTasks.take(6).forEach { task ->
                    val timePrefix = task.formattedReminderTime()?.let { "$it • " } ?: ""
                    inboxStyle.addLine("$timePrefix${task.title}")
                }
                if (todayTasks.size > 6) {
                    inboxStyle.setSummaryText("+${todayTasks.size - 6} görev daha")
                }
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(R.drawable.ic_notif_calendar)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(MORNING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun showTaskReminderNotification(
        context: Context,
        taskId: String,
        title: String,
        categoryLabel: String,
        timeLabel: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notifId = taskId.hashCode()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notif_calendar)
            .setContentTitle("Hatırlatma • $timeLabel")
            .setContentText("$title ($categoryLabel)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Tamamlandı", completePendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }
}
