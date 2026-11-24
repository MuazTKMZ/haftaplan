package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WeekDateUtils {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    private fun isoCalendar(): Calendar {
        return Calendar.getInstance(turkishLocale).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) into 1 (Pazartesi) .. 7 (Pazar).
     */
    fun calendarDayToIsoDay(calDayOfWeek: Int): Int {
        return when (calDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun isoDayToCalendarDay(isoDay: Int): Int {
        return when (isoDay.coerceIn(1, 7)) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }

    fun todayDayOfWeek(): Int {
        val cal = isoCalendar()
        return calendarDayToIsoDay(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun todayDateKey(): String {
        val cal = isoCalendar()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(cal.time)
    }

    fun currentWeekKey(): String {
        val cal = isoCalendar()
        val weekYear = cal.weekYear
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        return "%04d-W%02d".format(weekYear, weekOfYear)
    }

    fun mondayCalendarForWeek(weekKey: String): Calendar {
        val cal = isoCalendar()
        val parts = weekKey.split("-W")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull()
            val week = parts[1].toIntOrNull()
            if (year != null && week != null) {
                cal.clear()
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.minimalDaysInFirstWeek = 4
                cal.setWeekDate(year, week, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal
            }
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return cal
    }

    fun offsetWeekKey(weekKey: String, deltaWeeks: Int): String {
        val cal = mondayCalendarForWeek(weekKey)
        cal.add(Calendar.WEEK_OF_YEAR, deltaWeeks)
        return "%04d-W%02d".format(cal.weekYear, cal.get(Calendar.WEEK_OF_YEAR))
    }

    fun weekDateRangeLabel(weekKey: String): String {
        val start = mondayCalendarForWeek(weekKey)
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val sameMonth = start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
        val dayFmt = SimpleDateFormat("d", turkishLocale)
        val monthYearFmt = SimpleDateFormat("d MMMM yyyy", turkishLocale)
        val monthOnlyFmt = SimpleDateFormat("d MMMM", turkishLocale)
        return if (sameMonth) {
            "${dayFmt.format(start.time)} – ${monthYearFmt.format(end.time)}"
        } else {
            "${monthOnlyFmt.format(start.time)} – ${monthYearFmt.format(end.time)}"
        }
    }

    fun relativeWeekLabel(weekKey: String): String {
        val current = currentWeekKey()
        val next = offsetWeekKey(current, 1)
        val prev = offsetWeekKey(current, -1)
        return when (weekKey) {
            current -> "Bu Hafta"
            next -> "Gelecek Hafta"
            prev -> "Geçen Hafta"
            else -> weekKey
        }
    }

    fun dayDateNumber(weekKey: String, dayOfWeek: Int): String {
        val cal = mondayCalendarForWeek(weekKey)
        cal.add(Calendar.DAY_OF_YEAR, (dayOfWeek - 1).coerceIn(0, 6))
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }

    fun dayFullWithDate(weekKey: String, dayOfWeek: Int): String {
        val cal = mondayCalendarForWeek(weekKey)
        cal.add(Calendar.DAY_OF_YEAR, (dayOfWeek - 1).coerceIn(0, 6))
        val fmt = SimpleDateFormat("d MMMM, EEEE", turkishLocale)
        return fmt.format(cal.time)
    }

    fun dayNameFull(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Pazartesi"
        2 -> "Salı"
        3 -> "Çarşamba"
        4 -> "Perşembe"
        5 -> "Cuma"
        6 -> "Cumartesi"
        7 -> "Pazar"
        else -> "Pazartesi"
    }

    fun dayNameShort(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Pzt"
        2 -> "Sal"
        3 -> "Çar"
        4 -> "Per"
        5 -> "Cum"
        6 -> "Cmt"
        7 -> "Paz"
        else -> "Pzt"
    }

    fun nextMorningAlarmMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis + 1000L) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun taskReminderMillisInWeek(weekKey: String, dayOfWeek: Int, hour: Int, minute: Int): Long? {
        val cal = mondayCalendarForWeek(weekKey)
        cal.add(Calendar.DAY_OF_YEAR, (dayOfWeek - 1).coerceIn(0, 6))
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = System.currentTimeMillis()
        return if (cal.timeInMillis > now + 1000L) cal.timeInMillis else null
    }

    fun isAfterMorningTimeToday(morningHour: Int, morningMinute: Int): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val morningMinutes = morningHour * 60 + morningMinute
        return currentMinutes >= morningMinutes
    }
}
