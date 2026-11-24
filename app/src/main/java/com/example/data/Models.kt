package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskCategory(val displayName: String) {
    IS("İş"),
    KISISEL("Kişisel"),
    SAGLIK("Sağlık"),
    EGITIM("Eğitim"),
    DIGER("Diğer");

    companion object {
        fun fromRaw(raw: String?): TaskCategory {
            return entries.firstOrNull { it.name == raw } ?: DIGER
        }
    }
}

enum class TaskPriority(val displayName: String) {
    YUKSEK("Yüksek"),
    NORMAL("Normal"),
    DUSUK("Düşük");

    companion object {
        fun fromRaw(raw: String?): TaskPriority {
            return entries.firstOrNull { it.name == raw } ?: NORMAL
        }
    }
}

@Keep
@IgnoreExtraProperties
data class WeeklyTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weekKey: String = "",
    val dayOfWeek: Long = 1L,
    val title: String = "",
    val notes: String? = null,
    val category: String = TaskCategory.IS.name,
    val priority: String = TaskPriority.NORMAL.name,
    val completed: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Long? = null,
    val reminderMinute: Long? = null,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: TaskCategory
        get() = TaskCategory.fromRaw(category)

    val priorityEnum: TaskPriority
        get() = TaskPriority.fromRaw(priority)

    val dayInt: Int
        get() = dayOfWeek.toInt().coerceIn(1, 7)

    fun formattedReminderTime(): String? {
        if (!reminderEnabled) return null
        val h = reminderHour?.toInt() ?: return null
        val m = reminderMinute?.toInt() ?: return null
        return "%02d:%02d".format(h.coerceIn(0, 23), m.coerceIn(0, 59))
    }
}

@Keep
@IgnoreExtraProperties
data class PlanSettings(
    @DocumentId val id: String = "preferences",
    val userId: String = "",
    val morningHour: Long = 7L,
    val morningMinute: Long = 30L,
    val morningSummaryEnabled: Boolean = true,
    val mondayWeeklyBriefingEnabled: Boolean = true,
    val unlockBriefingEnabled: Boolean = true,
    val weekStartMonday: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedMorningTime(): String {
        val h = morningHour.toInt().coerceIn(0, 23)
        val m = morningMinute.toInt().coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
