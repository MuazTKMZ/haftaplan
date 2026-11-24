package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight local snapshot cache backed by SharedPreferences so AppWidgets,
 * BootReceiver, MorningSummaryReceiver, and TaskReminderReceiver work reliably
 * even when the app process is cold or offline.
 */
object LocalPlanCache {
    private const val PREFS_NAME = "haftaplan_local_cache"
    private const val KEY_TASKS_JSON = "tasks_json"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_MORNING_ENABLED = "morning_enabled"
    private const val KEY_MONDAY_WEEKLY = "monday_weekly"
    private const val KEY_UNLOCK_BRIEFING = "unlock_briefing"
    private const val KEY_LAST_BRIEFING_DATE = "last_briefing_date"
    private const val KEY_PENDING_BRIEFING_DATE = "pending_briefing_date"
    private const val KEY_USER_ID = "cached_user_id"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTasks(context: Context, userId: String, tasks: List<WeeklyTask>) {
        val arr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("userId", t.userId)
                put("weekKey", t.weekKey)
                put("dayOfWeek", t.dayOfWeek)
                put("title", t.title)
                put("notes", t.notes ?: "")
                put("category", t.category)
                put("priority", t.priority)
                put("completed", t.completed)
                put("reminderEnabled", t.reminderEnabled)
                put("reminderHour", t.reminderHour ?: -1L)
                put("reminderMinute", t.reminderMinute ?: -1L)
                put("sortOrder", t.sortOrder)
            }
            arr.put(obj)
        }
        prefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_TASKS_JSON, arr.toString())
            .apply()
    }

    fun getCachedTasks(context: Context): List<WeeklyTask> {
        val raw = prefs(context).getString(KEY_TASKS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<WeeklyTask>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val rHour = o.optLong("reminderHour", -1L)
                val rMin = o.optLong("reminderMinute", -1L)
                val notes = o.optString("notes", "").takeIf { it.isNotEmpty() }
                list.add(
                    WeeklyTask(
                        id = o.optString("id", ""),
                        userId = o.optString("userId", ""),
                        weekKey = o.optString("weekKey", ""),
                        dayOfWeek = o.optLong("dayOfWeek", 1L),
                        title = o.optString("title", ""),
                        notes = notes,
                        category = o.optString("category", TaskCategory.IS.name),
                        priority = o.optString("priority", TaskPriority.NORMAL.name),
                        completed = o.optBoolean("completed", false),
                        reminderEnabled = o.optBoolean("reminderEnabled", false),
                        reminderHour = if (rHour >= 0L) rHour else null,
                        reminderMinute = if (rMin >= 0L) rMin else null,
                        sortOrder = o.optLong("sortOrder", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getCurrentWeekTasks(context: Context): List<WeeklyTask> {
        val currentWeek = WeekDateUtils.currentWeekKey()
        return getCachedTasks(context)
            .filter { it.weekKey == currentWeek }
            .sortedWith(compareBy<WeeklyTask> { it.dayInt }.thenBy { it.completed }.thenBy { it.sortOrder })
    }

    fun getTodayTasks(context: Context): List<WeeklyTask> {
        val today = WeekDateUtils.todayDayOfWeek()
        return getCurrentWeekTasks(context).filter { it.dayInt == today }
    }

    fun markTaskCompletedLocally(context: Context, taskId: String, completed: Boolean) {
        val current = getCachedTasks(context).map {
            if (it.id == taskId) it.copy(completed = completed) else it
        }
        val uid = prefs(context).getString(KEY_USER_ID, "") ?: ""
        saveTasks(context, uid, current)
    }

    fun saveSettings(context: Context, settings: PlanSettings) {
        prefs(context).edit()
            .putInt(KEY_MORNING_HOUR, settings.morningHour.toInt().coerceIn(0, 23))
            .putInt(KEY_MORNING_MINUTE, settings.morningMinute.toInt().coerceIn(0, 59))
            .putBoolean(KEY_MORNING_ENABLED, settings.morningSummaryEnabled)
            .putBoolean(KEY_MONDAY_WEEKLY, settings.mondayWeeklyBriefingEnabled)
            .putBoolean(KEY_UNLOCK_BRIEFING, settings.unlockBriefingEnabled)
            .apply()
    }

    fun getSettings(context: Context): PlanSettings {
        val p = prefs(context)
        return PlanSettings(
            userId = p.getString(KEY_USER_ID, "") ?: "",
            morningHour = p.getInt(KEY_MORNING_HOUR, 7).toLong(),
            morningMinute = p.getInt(KEY_MORNING_MINUTE, 30).toLong(),
            morningSummaryEnabled = p.getBoolean(KEY_MORNING_ENABLED, true),
            mondayWeeklyBriefingEnabled = p.getBoolean(KEY_MONDAY_WEEKLY, true),
            unlockBriefingEnabled = p.getBoolean(KEY_UNLOCK_BRIEFING, true),
            weekStartMonday = true
        )
    }

    fun markPendingBriefingForToday(context: Context) {
        prefs(context).edit()
            .putString(KEY_PENDING_BRIEFING_DATE, WeekDateUtils.todayDateKey())
            .apply()
    }

    fun markBriefingShownToday(context: Context) {
        val today = WeekDateUtils.todayDateKey()
        prefs(context).edit()
            .putString(KEY_LAST_BRIEFING_DATE, today)
            .remove(KEY_PENDING_BRIEFING_DATE)
            .apply()
    }

    fun shouldShowDailyBriefingOnOpen(context: Context): Boolean {
        val p = prefs(context)
        val settings = getSettings(context)
        if (!settings.unlockBriefingEnabled) return false
        val today = WeekDateUtils.todayDateKey()
        val lastShown = p.getString(KEY_LAST_BRIEFING_DATE, null)
        if (lastShown == today) return false
        val pendingDate = p.getString(KEY_PENDING_BRIEFING_DATE, null)
        return pendingDate == today ||
            WeekDateUtils.isAfterMorningTimeToday(
                settings.morningHour.toInt(),
                settings.morningMinute.toInt()
            )
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
