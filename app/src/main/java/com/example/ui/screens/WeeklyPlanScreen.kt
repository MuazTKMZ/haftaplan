package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.TaskCategory
import com.example.data.UiState
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DaySelectorStrip
import com.example.ui.components.EmptyDayCard
import com.example.ui.components.TaskCardItem
import com.example.ui.components.WeekSelectorBanner

@Composable
fun WeeklyPlanScreen(
    tasksState: UiState<List<WeeklyTask>>,
    selectedWeekKey: String,
    selectedDayOfWeek: Int,
    selectedCategory: TaskCategory?,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onSelectCategory: (TaskCategory?) -> Unit,
    onToggleTask: (WeeklyTask) -> Unit,
    onEditTask: (WeeklyTask) -> Unit,
    onDeleteTask: (WeeklyTask) -> Unit,
    onAddTaskForDay: (Int) -> Unit,
    onOpenBriefing: () -> Unit,
    onOpenWeekRollover: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (tasksState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Plan yüklenirken bir sorun oluştu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tasksState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is UiState.Success -> {
                val weekTasks = tasksState.data.filter { it.weekKey == selectedWeekKey }
                val tasksByDay = weekTasks.groupBy { it.dayInt }
                val dayTasksAll = tasksByDay[selectedDayOfWeek].orEmpty()
                val dayTasksFiltered = if (selectedCategory == null) {
                    dayTasksAll
                } else {
                    dayTasksAll.filter { it.categoryEnum == selectedCategory }
                }
                val pendingDayTasks = dayTasksFiltered.filter { !it.completed }
                val completedDayTasks = dayTasksFiltered.filter { it.completed }
                val totalWeek = weekTasks.size
                val completedWeek = weekTasks.count { it.completed }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Action Header
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HaftaPlan",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = WeekDateUtils.dayFullWithDate(selectedWeekKey, selectedDayOfWeek),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = onOpenBriefing,
                                modifier = Modifier.testTag("open_briefing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Özet")
                            }
                        }
                    }

                    // Week Selector & Progress
                    item {
                        WeekSelectorBanner(
                            weekKey = selectedWeekKey,
                            totalTasks = totalWeek,
                            completedTasks = completedWeek,
                            onPreviousWeek = onPreviousWeek,
                            onNextWeek = onNextWeek,
                            onJumpToday = onJumpToday
                        )
                    }

                    // 7-Day Selector Strip (Pzt .. Paz)
                    item {
                        DaySelectorStrip(
                            weekKey = selectedWeekKey,
                            selectedDay = selectedDayOfWeek,
                            tasksByDay = tasksByDay,
                            onSelectDay = onSelectDay
                        )
                    }

                    // Category Filter Chips
                    item {
                        CategoryFilterRow(
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory
                        )
                    }

                    // Selected Day Header
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${WeekDateUtils.dayNameFull(selectedDayOfWeek)} Görevleri (${dayTasksFiltered.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (weekTasks.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = onOpenWeekRollover,
                                    modifier = Modifier.testTag("week_rollover_chip")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Update,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Haftayı Güncelle", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Empty State
                    if (dayTasksFiltered.isEmpty()) {
                        item {
                            EmptyDayCard(
                                dayName = WeekDateUtils.dayNameFull(selectedDayOfWeek),
                                onAddClick = { onAddTaskForDay(selectedDayOfWeek) }
                            )
                        }
                    } else {
                        // Pending tasks
                        items(pendingDayTasks, key = { it.id }) { task ->
                            TaskCardItem(
                                task = task,
                                onToggleComplete = { onToggleTask(task) },
                                onEdit = { onEditTask(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }

                        // Completed tasks section
                        if (completedDayTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Tamamlananlar (${completedDayTasks.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(completedDayTasks, key = { it.id }) { task ->
                                TaskCardItem(
                                    task = task,
                                    onToggleComplete = { onToggleTask(task) },
                                    onEdit = { onEditTask(task) },
                                    onDelete = { onDeleteTask(task) }
                                )
                            }
                        }
                    }

                    // End-of-week prompt banner when tasks exist
                    item {
                        AnimatedVisibility(visible = weekTasks.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hafta Sonu Plan Güncelleme",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Hafta bittiğinde kalan görevleri veya rutinlerini tek dokunuşla yeni haftaya taşı.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    Button(onClick = onOpenWeekRollover) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Aktar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
