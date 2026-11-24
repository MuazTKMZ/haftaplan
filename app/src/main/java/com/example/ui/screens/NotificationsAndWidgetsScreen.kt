package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlanSettings
import com.example.data.WeekDateUtils
import com.example.data.WeeklyTask
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationHelper
import com.example.widget.HorizontalPlanWidgetProvider
import com.example.widget.SquarePlanWidgetProvider
import com.example.widget.VerticalPlanWidgetProvider
import com.example.widget.WidgetUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsAndWidgetsScreen(
    userEmail: String?,
    settings: PlanSettings,
    todayTasks: List<WeeklyTask>,
    weekTasksCount: Int,
    onUpdateSettings: (PlanSettings) -> Unit,
    onPreviewMorningBriefing: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var showMorningTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            onShowMessage("Bildirim izni verildi!")
        } else {
            onShowMessage("Bildirimler için Sistem Ayarlarından izin verebilirsiniz.")
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Bildirimler & Widget'lar",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "07:30 sabah özeti, kilit açılış ekranı ve 3 farklı boyutta ana ekran widget'ı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notification Permission Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (hasNotifPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(
                        1.dp,
                        if (hasNotifPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (hasNotifPermission) Icons.Rounded.CheckCircle
                            else Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = if (hasNotifPermission) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasNotifPermission) "Bildirimler Aktif" else "Bildirim İzni Gerekli",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (hasNotifPermission) {
                                    if (AlarmScheduler.canScheduleExact(context)) {
                                        "Tam saatli sabah özeti ve görev hatırlatmaları hazır."
                                    } else {
                                        "Standart zamanlı bildirimler aktif. Tam dakika için alarm izni verebilirsiniz."
                                    }
                                } else {
                                    "Sabah 07:30 plan özeti ve hatırlatmalar için bildirim izni verin."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.testTag("grant_notif_permission_button")
                            ) {
                                Text("İzin Ver")
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(context)) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    } catch (_: Exception) {
                                        onShowMessage("Alarm ayarları açılamadı")
                                    }
                                }
                            ) {
                                Text("Alarm İzni")
                            }
                        }
                    }
                }
            }

            // Morning Summary & Unlock Settings Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Sabah Özeti & Açılış Kuralları",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        SettingSwitchRow(
                            title = "Günlük Sabah Özeti Bildirimi",
                            subtitle = "Her sabah belirlenen saatte özet bildirim gönder",
                            checked = settings.morningSummaryEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(morningSummaryEnabled = it))
                            }
                        )

                        // Time selector row
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { showMorningTimePicker = true }
                                .testTag("morning_time_row")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text("Sabah Özeti Saati", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Varsayılan 07:30 — değiştirmek için dokun",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = settings.formattedMorningTime(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        SettingSwitchRow(
                            title = "Pazartesi Tüm Hafta Özeti",
                            subtitle = "Pazartesi sabahları o haftada yapılacak tüm görevleri özetle",
                            checked = settings.mondayWeeklyBriefingEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(mondayWeeklyBriefingEnabled = it))
                            }
                        )

                        SettingSwitchRow(
                            title = "Telefon Açılışında Göster",
                            subtitle = "Günün ilk açılışında bugünkü (Pazartesi ise haftalık) görevleri ekrana getir",
                            checked = settings.unlockBriefingEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(unlockBriefingEnabled = it))
                            }
                        )

                        // Preview & Test Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = { onPreviewMorningBriefing(true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pazartesi Önizle", maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = { onPreviewMorningBriefing(false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bugünü Önizle", maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.showMorningSummaryNotification(context)
                                        onShowMessage("Sabah özeti test bildirimi gönderildi!")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_test_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Bildirimi Gönder")
                            }
                        }
                    }
                }
            }

            // Widgets Section: Horizontal, Vertical, Square
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ana Ekran Widget'ları",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Yatay (4×1), Dikey (2×4) ve Kare (2×2) widget'ları doğrudan ana ekranına ekleyebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Yatay Widget Card
                    WidgetPreviewCard(
                        title = "Yatay Widget (4×1)",
                        subtitle = "Bugünün özetini, ilerleme rozetini ve hızlı görev ekleme butonunu yatay şeritte sunar.",
                        onPinClick = {
                            val pinned = WidgetUpdater.requestPinWidget(context, HorizontalPlanWidgetProvider::class.java)
                            if (!pinned) onShowMessage("Ana ekrana basılı tutup Widget'lar menüsünden 'HaftaPlan • Yatay' seçebilirsiniz.")
                        }
                    ) {
                        HorizontalWidgetPreview(todayTasks = todayTasks)
                    }

                    // 2. Dikey Widget Card
                    WidgetPreviewCard(
                        title = "Dikey Widget (2×4)",
                        subtitle = "Bugünün tüm görevlerini ve haftalık kalan görev sayısını dikey listede gösterir.",
                        onPinClick = {
                            val pinned = WidgetUpdater.requestPinWidget(context, VerticalPlanWidgetProvider::class.java)
                            if (!pinned) onShowMessage("Ana ekrana basılı tutup Widget'lar menüsünden 'HaftaPlan • Dikey' seçebilirsiniz.")
                        }
                    ) {
                        VerticalWidgetPreview(todayTasks = todayTasks, weekTasksCount = weekTasksCount)
                    }

                    // 3. Kare Widget Card
                    WidgetPreviewCard(
                        title = "Kare Widget (2×2)",
                        subtitle = "Gün adı, tamamlama oranı ve en öncelikli 3 görevi kompakt kare kutuda gösterir.",
                        onPinClick = {
                            val pinned = WidgetUpdater.requestPinWidget(context, SquarePlanWidgetProvider::class.java)
                            if (!pinned) onShowMessage("Ana ekrana basılı tutup Widget'lar menüsünden 'HaftaPlan • Kare' seçebilirsiniz.")
                        }
                    ) {
                        SquareWidgetPreview(todayTasks = todayTasks)
                    }
                }
            }

            // Account / Sign Out Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Hesabı", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = userEmail ?: "Bağlı hesap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Çıkış Yap")
                        }
                    }
                }
            }
        }
    }

    if (showMorningTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.morningHour.toInt().coerceIn(0, 23),
            initialMinute = settings.morningMinute.toInt().coerceIn(0, 59),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showMorningTimePicker = false },
            title = { Text("Sabah Özeti Saati") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSettings(
                            settings.copy(
                                morningHour = pickerState.hour.toLong(),
                                morningMinute = pickerState.minute.toLong()
                            )
                        )
                        showMorningTimePicker = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMorningTimePicker = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WidgetPreviewCard(
    title: String,
    subtitle: String,
    onPinClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                previewContent()
            }

            FilledTonalButton(
                onClick = onPinClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddToHomeScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ana Ekrana Ekle")
            }
        }
    }
}

@Composable
private fun HorizontalWidgetPreview(todayTasks: List<WeeklyTask>) {
    val todayName = WeekDateUtils.dayNameFull(WeekDateUtils.todayDayOfWeek())
    val done = todayTasks.count { it.completed }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE3DBD0)),
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF125B50),
                modifier = Modifier
                    .width(96.dp)
                    .height(72.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Bugün", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDFF1EC))
                    Text(todayName, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE06D53)) {
                        Text(
                            "$done/${todayTasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val previewList = todayTasks.take(3)
                if (previewList.isEmpty()) {
                    Text("Bugün için görev ekleyin", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1C2120))
                } else {
                    previewList.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (task.completed) Color(0xFF125B50) else Color(0xFFE06D53))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1C2120),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE06D53)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun VerticalWidgetPreview(todayTasks: List<WeeklyTask>, weekTasksCount: Int) {
    val todayName = WeekDateUtils.dayNameFull(WeekDateUtils.todayDayOfWeek())
    val done = todayTasks.count { it.completed }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE3DBD0)),
        modifier = Modifier.width(185.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("HAFTA PLANI", style = MaterialTheme.typography.labelSmall, color = Color(0xFF125B50))
                    Text(todayName, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1C2120))
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE06D53)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDFF1EC)) {
                Text(
                    "Bugün: $done / ${todayTasks.size} görev",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0A3831),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val list = todayTasks.take(4)
            if (list.isEmpty()) {
                Text("Bugün için görev ekleyin", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5A6360))
            } else {
                list.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (task.completed) Color(0xFF125B50) else Color(0xFFE06D53))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1C2120),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Haftalık toplam: $weekTasksCount görev",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF5A6360)
            )
        }
    }
}

@Composable
private fun SquareWidgetPreview(todayTasks: List<WeeklyTask>) {
    val todayName = WeekDateUtils.dayNameFull(WeekDateUtils.todayDayOfWeek())
    val done = todayTasks.count { it.completed }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE3DBD0)),
        modifier = Modifier.size(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    todayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF125B50),
                    fontWeight = FontWeight.Bold
                )
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF125B50)) {
                    Text(
                        "$done/${todayTasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text("Bugünkü Plan", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5A6360))
            Spacer(modifier = Modifier.height(8.dp))
            val list = todayTasks.take(3)
            if (list.isEmpty()) {
                Text("Görev ekleyin", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5A6360))
            } else {
                list.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (task.completed) Color(0xFF125B50) else Color(0xFFE06D53))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1C2120),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
