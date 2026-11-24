package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.LocalPlanCache
import com.example.widget.WidgetUpdater
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MorningSummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = LocalPlanCache.getSettings(context)
        if (settings.morningSummaryEnabled) {
            LocalPlanCache.markPendingBriefingForToday(context)
            NotificationHelper.showMorningSummaryNotification(context)
        }
        AlarmScheduler.rescheduleMorningAlarm(context, settings)
        WidgetUpdater.refreshAll(context)
    }
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "Plan"
        val timeLabel = intent.getStringExtra(EXTRA_TASK_TIME) ?: ""

        val cachedTask = LocalPlanCache.getCachedTasks(context).firstOrNull { it.id == taskId }
        if (cachedTask != null && cachedTask.completed) return

        NotificationHelper.showTaskReminderNotification(
            context = context,
            taskId = taskId,
            title = title,
            categoryLabel = category,
            timeLabel = timeLabel
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
        const val EXTRA_TASK_TIME = "extra_task_time"
    }
}

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_TASK) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)

        NotificationManagerCompat.from(context).cancel(notifId)
        LocalPlanCache.markTaskCompletedLocally(context, taskId, true)
        WidgetUpdater.refreshAll(context)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val pendingResult = goAsync()
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        db.collection("users").document(uid).collection("tasks").document(taskId)
            .update(
                mapOf(
                    "completed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnCompleteListener {
                pendingResult.finish()
            }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.ACTION_COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            NotificationHelper.ensureChannels(context)
            val settings = LocalPlanCache.getSettings(context)
            val tasks = LocalPlanCache.getCachedTasks(context)
            AlarmScheduler.rescheduleMorningAlarm(context, settings)
            AlarmScheduler.rescheduleTaskReminders(context, tasks)
            WidgetUpdater.refreshAll(context)
        }
    }
}
