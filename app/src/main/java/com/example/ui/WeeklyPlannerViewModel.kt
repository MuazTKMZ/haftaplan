package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlanSettings
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.UiState
import com.example.data.WeekDateUtils
import com.example.data.WeeklyPlanRepository
import com.example.data.WeeklyTask
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeeklyPlannerViewModel(
    private val repository: WeeklyPlanRepository,
    private val currentUserId: String
) : ViewModel() {

    val tasksState: StateFlow<UiState<List<WeeklyTask>>> = repository.observeTasks(currentUserId)
        .map<List<WeeklyTask>, UiState<List<WeeklyTask>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Görevler yüklenemedi."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<PlanSettings> = repository.observeSettings(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing settings", firestoreError ?: error)
            emit(PlanSettings(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PlanSettings(userId = currentUserId)
        )

    private val _selectedWeekKey = MutableStateFlow(WeekDateUtils.currentWeekKey())
    val selectedWeekKey: StateFlow<String> = _selectedWeekKey.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(WeekDateUtils.todayDayOfWeek())
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TaskCategory?>(null)
    val selectedCategoryFilter: StateFlow<TaskCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings(currentUserId)
        }
    }

    fun selectWeek(weekKey: String) {
        _selectedWeekKey.value = weekKey
    }

    fun shiftWeek(delta: Int) {
        _selectedWeekKey.value = WeekDateUtils.offsetWeekKey(_selectedWeekKey.value, delta)
    }

    fun jumpToToday() {
        _selectedWeekKey.value = WeekDateUtils.currentWeekKey()
        _selectedDayOfWeek.value = WeekDateUtils.todayDayOfWeek()
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDayOfWeek.value = dayOfWeek.coerceIn(1, 7)
    }

    fun selectCategoryFilter(category: TaskCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveTask(
        editingTask: WeeklyTask?,
        weekKey: String,
        dayOfWeek: Int,
        title: String,
        notes: String?,
        category: TaskCategory,
        priority: TaskPriority,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        nextSortOrder: Long
    ) {
        viewModelScope.launch {
            val result = if (editingTask == null) {
                repository.addTask(
                    weekKey = weekKey,
                    dayOfWeek = dayOfWeek,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    sortOrder = nextSortOrder
                )
            } else {
                repository.updateTask(
                    taskId = editingTask.id,
                    weekKey = weekKey,
                    dayOfWeek = dayOfWeek,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    completed = editingTask.completed,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    sortOrder = editingTask.sortOrder
                )
            }
            result.onSuccess {
                _statusMessage.value = if (editingTask == null) "Görev eklendi" else "Görev güncellendi"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Görev kaydedilemedi"
            }
        }
    }

    fun toggleTask(task: WeeklyTask) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, !task.completed)
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Durum güncellenemedi"
                }
        }
    }

    fun deleteTask(task: WeeklyTask) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
                .onSuccess {
                    _statusMessage.value = "Görev silindi"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Görev silinemedi"
                }
        }
    }

    fun updateSettings(updated: PlanSettings) {
        viewModelScope.launch {
            repository.saveSettings(updated.copy(userId = currentUserId))
                .onSuccess {
                    _statusMessage.value = "Bildirim ayarları kaydedildi"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Ayarlar kaydedilemedi"
                }
        }
    }

    fun copyTasksToNextWeek(
        sourceTasks: List<WeeklyTask>,
        targetWeekKey: String,
        resetCompleted: Boolean = true
    ) {
        viewModelScope.launch {
            repository.copyTasksToWeek(sourceTasks, targetWeekKey, resetCompleted)
                .onSuccess { count ->
                    _selectedWeekKey.value = targetWeekKey
                    _statusMessage.value = "$count görev yeni haftaya aktarıldı"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Hafta aktarımı başarısız"
                }
        }
    }

    private companion object {
        const val TAG = "WeeklyPlannerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
