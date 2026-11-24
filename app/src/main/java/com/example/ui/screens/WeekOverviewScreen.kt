package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask
import com.example.ui.components.WeekSelectorBanner

@Composable
fun WeekOverviewScreen(
    tasksState: UiState<List<WeeklyTask>>,
    selectedWeekKey: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToday: () -> Unit,
    onDaySelected: (Int) -> Unit,
    onAddTaskForDay: (Int) -> Unit,
    onToggleTask: (WeeklyTask) -> Unit,
    onCarryOverUnfinished: (List<WeeklyTask>, String) -> Unit,
    onCopyAllToNextWeek: (List<WeeklyTask>, String) -> Unit
) {
    val allTasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val weekTasks = allTasks.filter { it.weekKey == selectedWeekKey }
    val tasksByDay = weekTasks.groupBy { it.dayInt }
    val nextWeekKey = WeekDateUtils.offsetWeekKey(selectedWeekKey, 1)
    val unfinished = weekTasks.filter { !it.completed }
    val completedCount = weekTasks.count { it.completed }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
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
            item {
                Column {
                    Text(
                        text = "Haftalık Genel Bakış",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pazartesi'den Pazar'a tüm planın ve hafta sonu güncelleme araçları",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                WeekSelectorBanner(
                    weekKey = selectedWeekKey,
                    totalTasks = weekTasks.size,
                    completedTasks = completedCount,
                    onPreviousWeek = onPreviousWeek,
                    onNextWeek = onNextWeek,
                    onJumpToday = onJumpToday
                )
            }

            // Week End Rollover / Update Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Haftayı Güncelle → ${WeekDateUtils.weekDateRangeLabel(nextWeekKey)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Bu haftada $completedCount tamamlanan, ${unfinished.size} bekleyen görev var.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onCarryOverUnfinished(unfinished, nextWeekKey) },
                                enabled = unfinished.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("carry_unfinished_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kalanları Aktar (${unfinished.size})")
                            }

                            FilledTonalButton(
                                onClick = { onCopyAllToNextWeek(weekTasks, nextWeekKey) },
                                enabled = weekTasks.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_all_week_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tümünü Kopyala")
                            }
                        }
                    }
                }
            }

            // 7 Day Summary Cards
            items((1..7).toList()) { day ->
                val dayTasks = tasksByDay[day].orEmpty()
                val isToday = selectedWeekKey == WeekDateUtils.currentWeekKey() &&
                    day == WeekDateUtils.todayDayOfWeek()

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isToday) 1.5.dp else 1.dp,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onDaySelected(day) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = WeekDateUtils.dayDateNumber(selectedWeekKey, day),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column {
                                    Text(
                                        text = WeekDateUtils.dayNameFull(day) + if (isToday) " • Bugün" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (dayTasks.isEmpty()) "Görev planlanmadı"
                                        else "${dayTasks.count { it.completed }}/${dayTasks.size} tamamlandı",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onAddTaskForDay(day) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "${WeekDateUtils.dayNameFull(day)} için görev ekle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (dayTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dayTasks.forEach { task ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onToggleTask(task) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (task.completed) Icons.Rounded.CheckCircle
                                            else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (task.completed) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        task.formattedReminderTime()?.let { t ->
                                            Text(
                                                text = t,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
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
}
