package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.data.LocalPlanCache
import com.example.data.WeekDateUtils
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationHelper
import com.example.widget.WidgetUpdater

class HaftaPlanApplication : Application() {

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                val settings = LocalPlanCache.getSettings(context)
                if (settings.unlockBriefingEnabled &&
                    WeekDateUtils.isAfterMorningTimeToday(
                        settings.morningHour.toInt(),
                        settings.morningMinute.toInt()
                    )
                ) {
                    LocalPlanCache.markPendingBriefingForToday(context)
                    if (settings.morningSummaryEnabled && LocalPlanCache.shouldShowDailyBriefingOnOpen(context)) {
                        NotificationHelper.showMorningSummaryNotification(context)
                    }
                }
                WidgetUpdater.refreshAll(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        val settings = LocalPlanCache.getSettings(this)
        AlarmScheduler.rescheduleMorningAlarm(this, settings)
        AlarmScheduler.rescheduleTaskReminders(this, LocalPlanCache.getCachedTasks(this))
        WidgetUpdater.refreshAll(this)

        ContextCompat.registerReceiver(
            this,
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
