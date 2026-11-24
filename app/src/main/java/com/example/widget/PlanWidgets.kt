package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.LocalPlanCache
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask
import com.example.notifications.NotificationHelper

object WidgetUpdater {
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val appContext = context.applicationContext

        val hIds = manager.getAppWidgetIds(ComponentName(appContext, HorizontalPlanWidgetProvider::class.java))
        if (hIds.isNotEmpty()) HorizontalPlanWidgetProvider.updateWidgets(appContext, manager, hIds)

        val vIds = manager.getAppWidgetIds(ComponentName(appContext, VerticalPlanWidgetProvider::class.java))
        if (vIds.isNotEmpty()) VerticalPlanWidgetProvider.updateWidgets(appContext, manager, vIds)

        val sIds = manager.getAppWidgetIds(ComponentName(appContext, SquarePlanWidgetProvider::class.java))
        if (sIds.isNotEmpty()) SquarePlanWidgetProvider.updateWidgets(appContext, manager, sIds)
    }

    fun requestPinWidget(context: Context, providerClass: Class<out AppWidgetProvider>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) {
                val component = ComponentName(context, providerClass)
                return manager.requestPinAppWidget(component, null, null)
            }
        }
        return false
    }

    internal fun openAppPendingIntent(context: Context, requestCode: Int, openAddTask: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openAddTask) {
                putExtra(NotificationHelper.EXTRA_OPEN_ADD_TASK, true)
                putExtra(NotificationHelper.EXTRA_TASK_DAY, WeekDateUtils.todayDayOfWeek())
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun formatWidgetTaskLine(task: WeeklyTask): String {
        val time = task.formattedReminderTime()
        return if (time != null) "$time • ${task.title}" else task.title
    }
}

class HorizontalPlanWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val today = WeekDateUtils.todayDayOfWeek()
            val todayTasks = LocalPlanCache.getTodayTasks(context)
            val completedCount = todayTasks.count { it.completed }
            val totalCount = todayTasks.size
            val dayName = WeekDateUtils.dayNameFull(today)

            val rowIds = listOf(R.id.widget_h_row_1, R.id.widget_h_row_2, R.id.widget_h_row_3)
            val dotIds = listOf(R.id.widget_h_dot_1, R.id.widget_h_dot_2, R.id.widget_h_dot_3)
            val textIds = listOf(R.id.widget_h_task_1, R.id.widget_h_task_2, R.id.widget_h_task_3)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_horizontal)
                views.setTextViewText(R.id.widget_h_day, "Bugün")
                views.setTextViewText(R.id.widget_h_day_name, dayName)
                views.setTextViewText(R.id.widget_h_progress, "$completedCount/$totalCount")

                views.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(context, 100 + appWidgetId))
                views.setOnClickPendingIntent(R.id.widget_h_add_btn, WidgetUpdater.openAppPendingIntent(context, 200 + appWidgetId, openAddTask = true))

                if (todayTasks.isEmpty()) {
                    views.setViewVisibility(rowIds[0], View.VISIBLE)
                    views.setImageViewResource(dotIds[0], R.drawable.widget_dot_pending)
                    views.setTextViewText(textIds[0], "Bugün için görev ekleyin")
                    views.setViewVisibility(rowIds[1], View.GONE)
                    views.setViewVisibility(rowIds[2], View.GONE)
                } else {
                    for (i in 0..2) {
                        val task = todayTasks.getOrNull(i)
                        if (task != null) {
                            views.setViewVisibility(rowIds[i], View.VISIBLE)
                            views.setImageViewResource(
                                dotIds[i],
                                if (task.completed) R.drawable.widget_dot_done else R.drawable.widget_dot_pending
                            )
                            views.setTextViewText(textIds[i], WidgetUpdater.formatWidgetTaskLine(task))
                        } else {
                            views.setViewVisibility(rowIds[i], View.GONE)
                        }
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

class VerticalPlanWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val today = WeekDateUtils.todayDayOfWeek()
            val weekTasks = LocalPlanCache.getCurrentWeekTasks(context)
            val todayTasks = weekTasks.filter { it.dayInt == today }
            val completedToday = todayTasks.count { it.completed }
            val pendingWeek = weekTasks.count { !it.completed }

            val rowIds = listOf(
                R.id.widget_v_row_1, R.id.widget_v_row_2, R.id.widget_v_row_3,
                R.id.widget_v_row_4, R.id.widget_v_row_5
            )
            val dotIds = listOf(
                R.id.widget_v_dot_1, R.id.widget_v_dot_2, R.id.widget_v_dot_3,
                R.id.widget_v_dot_4, R.id.widget_v_dot_5
            )
            val textIds = listOf(
                R.id.widget_v_task_1, R.id.widget_v_task_2, R.id.widget_v_task_3,
                R.id.widget_v_task_4, R.id.widget_v_task_5
            )

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_vertical)
                views.setTextViewText(R.id.widget_v_week, "HAFTA PLANI")
                views.setTextViewText(R.id.widget_v_day, WeekDateUtils.dayNameFull(today))
                views.setTextViewText(
                    R.id.widget_v_progress,
                    "Bugün: $completedToday / ${todayTasks.size} görev"
                )
                views.setTextViewText(
                    R.id.widget_v_footer,
                    "Haftalık kalan: $pendingWeek görev"
                )

                views.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(context, 300 + appWidgetId))
                views.setOnClickPendingIntent(R.id.widget_v_add_btn, WidgetUpdater.openAppPendingIntent(context, 400 + appWidgetId, openAddTask = true))

                if (todayTasks.isEmpty()) {
                    views.setViewVisibility(rowIds[0], View.VISIBLE)
                    views.setImageViewResource(dotIds[0], R.drawable.widget_dot_pending)
                    views.setTextViewText(textIds[0], "Bugün için görev ekleyin")
                    for (i in 1..4) views.setViewVisibility(rowIds[i], View.GONE)
                } else {
                    for (i in 0..4) {
                        val task = todayTasks.getOrNull(i)
                        if (task != null) {
                            views.setViewVisibility(rowIds[i], View.VISIBLE)
                            views.setImageViewResource(
                                dotIds[i],
                                if (task.completed) R.drawable.widget_dot_done else R.drawable.widget_dot_pending
                            )
                            views.setTextViewText(textIds[i], WidgetUpdater.formatWidgetTaskLine(task))
                        } else {
                            views.setViewVisibility(rowIds[i], View.GONE)
                        }
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

class SquarePlanWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val today = WeekDateUtils.todayDayOfWeek()
            val todayTasks = LocalPlanCache.getTodayTasks(context)
            val completedCount = todayTasks.count { it.completed }
            val totalCount = todayTasks.size

            val rowIds = listOf(R.id.widget_s_row_1, R.id.widget_s_row_2, R.id.widget_s_row_3)
            val dotIds = listOf(R.id.widget_s_dot_1, R.id.widget_s_dot_2, R.id.widget_s_dot_3)
            val textIds = listOf(R.id.widget_s_task_1, R.id.widget_s_task_2, R.id.widget_s_task_3)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_square)
                views.setTextViewText(R.id.widget_s_day, WeekDateUtils.dayNameFull(today))
                views.setTextViewText(R.id.widget_s_ratio, "$completedCount/$totalCount")
                views.setTextViewText(
                    R.id.widget_s_subtitle,
                    if (today == 1) "Yeni Hafta • Pazartesi" else "Bugünkü Plan"
                )

                views.setOnClickPendingIntent(R.id.widget_root, WidgetUpdater.openAppPendingIntent(context, 500 + appWidgetId))

                if (todayTasks.isEmpty()) {
                    views.setViewVisibility(rowIds[0], View.VISIBLE)
                    views.setImageViewResource(dotIds[0], R.drawable.widget_dot_pending)
                    views.setTextViewText(textIds[0], "Görev ekleyin")
                    views.setViewVisibility(rowIds[1], View.GONE)
                    views.setViewVisibility(rowIds[2], View.GONE)
                } else {
                    for (i in 0..2) {
                        val task = todayTasks.getOrNull(i)
                        if (task != null) {
                            views.setViewVisibility(rowIds[i], View.VISIBLE)
                            views.setImageViewResource(
                                dotIds[i],
                                if (task.completed) R.drawable.widget_dot_done else R.drawable.widget_dot_pending
                            )
                            views.setTextViewText(textIds[i], task.title)
                        } else {
                            views.setViewVisibility(rowIds[i], View.GONE)
                        }
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
