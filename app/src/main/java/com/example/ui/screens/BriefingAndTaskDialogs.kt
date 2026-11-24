package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.TaskCategory
import com.example.data.TaskPriority
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask

@Composable
fun MorningBriefingDialog(
    isMondayWeeklyMode: Boolean,
    weekKey: String,
    weekTasks: List<WeeklyTask>,
    onToggleTask: (WeeklyTask) -> Unit,
    onAddTask: () -> Unit,
    onDismiss: () -> Unit
) {
    val todayDay = WeekDateUtils.todayDayOfWeek()
    val todayTasks = weekTasks.filter { it.dayInt == todayDay }
    val pendingWeek = weekTasks.filter { !it.completed }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Visual Banner Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_morning_briefing),
                        contentDescription = "Sabah özeti görseli",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color(0xFF0A3831).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Özeti kapat",
                            tint = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isMondayWeeklyMode) "PAZARTESİ HAFTALIK ÖZET"
                                    else "GÜNÜN PLANI • ${WeekDateUtils.dayNameFull(todayDay).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMondayWeeklyMode) "Bu Hafta Yapılacaklar"
                            else "Bugün Yapılacaklar",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isMondayWeeklyMode) {
                        Text(
                            text = "${WeekDateUtils.weekDateRangeLabel(weekKey)} • Toplam ${pendingWeek.size} bekleyen görev",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (weekTasks.isEmpty()) {
                            BriefingEmptyState(
                                message = "Bu hafta için henüz görev eklemedin. Haftanı planlamaya hemen başla!",
                                onAddTask = {
                                    onDismiss()
                                    onAddTask()
                                }
                            )
                        } else {
                            (1..7).forEach { day ->
                                val dayList = weekTasks.filter { it.dayInt == day }
                                if (dayList.isNotEmpty()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${WeekDateUtils.dayNameFull(day)} (${dayList.size})",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            dayList.forEach { task ->
                                                BriefingTaskRow(
                                                    task = task,
                                                    onToggle = { onToggleTask(task) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${WeekDateUtils.dayFullWithDate(weekKey, todayDay)} • ${todayTasks.count { !it.completed }} kalan görev",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (todayTasks.isEmpty()) {
                            BriefingEmptyState(
                                message = "Bugün için bekleyen bir görev görünmüyor. İstersen yeni bir görev ekleyebilirsin.",
                                onAddTask = {
                                    onDismiss()
                                    onAddTask()
                                }
                            )
                        } else {
                            todayTasks.forEach { task ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        BriefingTaskRow(
                                            task = task,
                                            onToggle = { onToggleTask(task) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddTask()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Görev Ekle")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dismiss_briefing_button")
                    ) {
                        Text("Planıma Geç")
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingTaskRow(
    task: WeeklyTask,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun BriefingEmptyState(
    message: String,
    onAddTask: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onAddTask) {
                Text("+ Şimdi Görev Oluştur")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    editingTask: WeeklyTask?,
    initialWeekKey: String,
    initialDayOfWeek: Int,
    onDismiss: () -> Unit,
    onSave: (
        weekKey: String,
        dayOfWeek: Int,
        title: String,
        notes: String?,
        category: TaskCategory,
        priority: TaskPriority,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
    var notes by remember(editingTask) { mutableStateOf(editingTask?.notes ?: "") }
    var selectedDay by remember(editingTask, initialDayOfWeek) {
        mutableIntStateOf(editingTask?.dayInt ?: initialDayOfWeek)
    }
    var selectedCategory by remember(editingTask) {
        mutableStateOf(editingTask?.categoryEnum ?: TaskCategory.IS)
    }
    var selectedPriority by remember(editingTask) {
        mutableStateOf(editingTask?.priorityEnum ?: TaskPriority.NORMAL)
    }
    var reminderEnabled by remember(editingTask) {
        mutableStateOf(editingTask?.reminderEnabled ?: false)
    }
    var reminderHour by remember(editingTask) {
        mutableIntStateOf(editingTask?.reminderHour?.toInt() ?: 9)
    }
    var reminderMinute by remember(editingTask) {
        mutableIntStateOf(editingTask?.reminderMinute?.toInt() ?: 0)
    }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingTask == null) "Yeni Görev Planla" else "Görevi Düzenle",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Görev Başlığı") },
                placeholder = { Text("Örn: Haftalık raporları tamamla") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 300) notes = it },
                label = { Text("Not (İsteğe bağlı)") },
                placeholder = { Text("Ek detaylar veya alt adımlar") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )

            // Day of Week Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gün", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(WeekDateUtils.dayNameShort(day)) }
                        )
                    }
                }
            }

            // Category Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kategori", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }

            // Priority Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Öncelik", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Reminder Toggle & Time Picker
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Görev Hatırlatıcısı", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (reminderEnabled) {
                                        "Saat %02d:%02d'da bildirim gönderilecek".format(reminderHour, reminderMinute)
                                    } else {
                                        "Bu görev için özel saat hatırlatması kapalı"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            modifier = Modifier.testTag("task_reminder_switch")
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Hatırlatma Saati: %02d:%02d (Değiştir)".format(reminderHour, reminderMinute),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Vazgeç")
                }
                Button(
                    onClick = {
                        onSave(
                            editingTask?.weekKey ?: initialWeekKey,
                            selectedDay,
                            title,
                            notes.takeIf { it.isNotBlank() },
                            selectedCategory,
                            selectedPriority,
                            reminderEnabled,
                            if (reminderEnabled) reminderHour else null,
                            if (reminderEnabled) reminderMinute else null
                        )
                    },
                    enabled = title.trim().isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_task_button")
                ) {
                    Text(if (editingTask == null) "Ekle" else "Kaydet")
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hatırlatma Saati Seç") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderHour = pickerState.hour
                        reminderMinute = pickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun WeekRolloverDialog(
    currentWeekKey: String,
    weekTasks: List<WeeklyTask>,
    onCarryOverUnfinished: (List<WeeklyTask>, String) -> Unit,
    onCopyAllToNextWeek: (List<WeeklyTask>, String) -> Unit,
    onDismiss: () -> Unit
) {
    val nextWeekKey = WeekDateUtils.offsetWeekKey(currentWeekKey, 1)
    val unfinished = weekTasks.filter { !it.completed }
    val completed = weekTasks.count { it.completed }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haftalık Planı Güncelle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${WeekDateUtils.weekDateRangeLabel(currentWeekKey)} haftasında $completed görev tamamladın, ${unfinished.size} görev bekliyor.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Hedef hafta: ${WeekDateUtils.weekDateRangeLabel(nextWeekKey)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (unfinished.isNotEmpty()) {
                    Button(
                        onClick = {
                            onCarryOverUnfinished(unfinished, nextWeekKey)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tamamlanmayanları Aktar (${unfinished.size})")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onCopyAllToNextWeek(weekTasks, nextWeekKey)
                        onDismiss()
                    },
                    enabled = weekTasks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tüm Haftayı Yeni Haftaya Kopyala (${weekTasks.size})")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
