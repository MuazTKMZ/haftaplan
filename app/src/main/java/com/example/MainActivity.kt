package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.data.LocalPlanCache
import com.example.data.UiState
import com.example.data.WeekDateUtils
import com.example.data.WeeklyPlanRepository
import com.example.data.WeeklyTask
import com.example.notifications.NotificationHelper
import com.example.ui.WeeklyPlannerViewModel
import com.example.ui.screens.MorningBriefingDialog
import com.example.ui.screens.NotificationsAndWidgetsScreen
import com.example.ui.screens.TaskEditorSheet
import com.example.ui.screens.WeekOverviewScreen
import com.example.ui.screens.WeekRolloverDialog
import com.example.ui.screens.WeeklyPlanScreen
import com.example.ui.theme.HaftaPlanTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val intentTriggerState = mutableStateOf<LaunchTrigger?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        handleLaunchIntent(intent)

        setContent {
            HaftaPlanTheme {
                AppContent(
                    launchTrigger = intentTriggerState.value,
                    onConsumeTrigger = { intentTriggerState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (intentTriggerState.value == null && LocalPlanCache.shouldShowDailyBriefingOnOpen(this)) {
            val isMonday = WeekDateUtils.todayDayOfWeek() == 1 &&
                LocalPlanCache.getSettings(this).mondayWeeklyBriefingEnabled
            intentTriggerState.value = LaunchTrigger.ShowBriefing(mondayMode = isMonday)
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        when {
            intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_BRIEFING, false) -> {
                val mondayMode = intent.getBooleanExtra(
                    NotificationHelper.EXTRA_BRIEFING_MONDAY,
                    WeekDateUtils.todayDayOfWeek() == 1
                )
                intentTriggerState.value = LaunchTrigger.ShowBriefing(mondayMode = mondayMode)
            }
            intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ADD_TASK, false) -> {
                val day = intent.getIntExtra(
                    NotificationHelper.EXTRA_TASK_DAY,
                    WeekDateUtils.todayDayOfWeek()
                )
                intentTriggerState.value = LaunchTrigger.OpenAddTask(dayOfWeek = day)
            }
        }
    }
}

sealed interface LaunchTrigger {
    data class ShowBriefing(val mondayMode: Boolean) : LaunchTrigger
    data class OpenAddTask(val dayOfWeek: Int) : LaunchTrigger
}

@Composable
fun AppContent(
    launchTrigger: LaunchTrigger?,
    onConsumeTrigger: () -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            HaftaPlanMainScreen(
                currentUserId = user.uid,
                userEmail = user.email,
                launchTrigger = launchTrigger,
                onConsumeTrigger = onConsumeTrigger
            )
        }
    }
}

@Composable
private fun HaftaPlanMainScreen(
    currentUserId: String,
    userEmail: String?,
    launchTrigger: LaunchTrigger?,
    onConsumeTrigger: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: WeeklyPlannerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WeeklyPlannerViewModel(
                    repository = WeeklyPlanRepository(db = db, appContext = app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val selectedWeekKey by viewModel.selectedWeekKey.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<WeeklyTask?>(null) }
    var editorInitialDay by remember { mutableIntStateOf(selectedDay) }
    var showBriefing by remember { mutableStateOf(false) }
    var briefingMondayMode by remember { mutableStateOf(WeekDateUtils.todayDayOfWeek() == 1) }
    var showWeekRollover by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(launchTrigger) {
        when (launchTrigger) {
            is LaunchTrigger.ShowBriefing -> {
                briefingMondayMode = launchTrigger.mondayMode
                showBriefing = true
                LocalPlanCache.markBriefingShownToday(context)
                onConsumeTrigger()
            }
            is LaunchTrigger.OpenAddTask -> {
                editingTask = null
                editorInitialDay = launchTrigger.dayOfWeek
                viewModel.jumpToToday()
                showTaskEditor = true
                onConsumeTrigger()
            }
            null -> Unit
        }
    }

    if (selectedTab != 0) {
        BackHandler { selectedTab = 0 }
    }

    val allTasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val currentWeekTasks = allTasks.filter { it.weekKey == WeekDateUtils.currentWeekKey() }
    val selectedWeekTasks = allTasks.filter { it.weekKey == selectedWeekKey }
    val todayTasks = currentWeekTasks.filter { it.dayInt == WeekDateUtils.todayDayOfWeek() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingTask = null
                            editorInitialDay = selectedDay
                            showTaskEditor = true
                        },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Görev Ekle") },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("fab_add_task")
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    if (selectedTab == 0) Icons.Rounded.Today else Icons.Outlined.Today,
                                    contentDescription = null
                                )
                            },
                            label = { Text("Haftam") },
                            modifier = Modifier.testTag("nav_tab_plan")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    if (selectedTab == 1) Icons.Rounded.CalendarViewWeek else Icons.Outlined.CalendarViewWeek,
                                    contentDescription = null
                                )
                            },
                            label = { Text("Genel Bakış") },
                            modifier = Modifier.testTag("nav_tab_overview")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    if (selectedTab == 2) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = null
                                )
                            },
                            label = { Text("Bildirim & Widget") },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Rounded.Today, contentDescription = null) },
                            label = { Text("Haftam") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Rounded.CalendarViewWeek, contentDescription = null) },
                            label = { Text("Bakış") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                            label = { Text("Ayarlar") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> WeeklyPlanScreen(
                            tasksState = tasksState,
                            selectedWeekKey = selectedWeekKey,
                            selectedDayOfWeek = selectedDay,
                            selectedCategory = selectedCategory,
                            onPreviousWeek = { viewModel.shiftWeek(-1) },
                            onNextWeek = { viewModel.shiftWeek(1) },
                            onJumpToday = { viewModel.jumpToToday() },
                            onSelectDay = { viewModel.selectDay(it) },
                            onSelectCategory = { viewModel.selectCategoryFilter(it) },
                            onToggleTask = { viewModel.toggleTask(it) },
                            onEditTask = { task ->
                                editingTask = task
                                editorInitialDay = task.dayInt
                                showTaskEditor = true
                            },
                            onDeleteTask = { viewModel.deleteTask(it) },
                            onAddTaskForDay = { day ->
                                editingTask = null
                                editorInitialDay = day
                                showTaskEditor = true
                            },
                            onOpenBriefing = {
                                briefingMondayMode = WeekDateUtils.todayDayOfWeek() == 1 &&
                                    settings.mondayWeeklyBriefingEnabled
                                showBriefing = true
                            },
                            onOpenWeekRollover = { showWeekRollover = true }
                        )

                        1 -> WeekOverviewScreen(
                            tasksState = tasksState,
                            selectedWeekKey = selectedWeekKey,
                            onPreviousWeek = { viewModel.shiftWeek(-1) },
                            onNextWeek = { viewModel.shiftWeek(1) },
                            onJumpToday = { viewModel.jumpToToday() },
                            onDaySelected = { day ->
                                viewModel.selectDay(day)
                                selectedTab = 0
                            },
                            onAddTaskForDay = { day ->
                                editingTask = null
                                editorInitialDay = day
                                showTaskEditor = true
                            },
                            onToggleTask = { viewModel.toggleTask(it) },
                            onCarryOverUnfinished = { list, targetWeek ->
                                viewModel.copyTasksToNextWeek(list, targetWeek, resetCompleted = true)
                            },
                            onCopyAllToNextWeek = { list, targetWeek ->
                                viewModel.copyTasksToNextWeek(list, targetWeek, resetCompleted = true)
                            }
                        )

                        2 -> NotificationsAndWidgetsScreen(
                            userEmail = userEmail,
                            settings = settings,
                            todayTasks = todayTasks,
                            weekTasksCount = currentWeekTasks.size,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onPreviewMorningBriefing = { mondayMode ->
                                briefingMondayMode = mondayMode
                                showBriefing = true
                            },
                            onShowMessage = { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onSignOut = {
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTaskEditor) {
        val currentDayTasksCount = selectedWeekTasks.count { it.dayInt == editorInitialDay }.toLong()
        TaskEditorSheet(
            editingTask = editingTask,
            initialWeekKey = selectedWeekKey,
            initialDayOfWeek = editorInitialDay,
            onDismiss = {
                showTaskEditor = false
                editingTask = null
            },
            onSave = { weekKey, day, title, notes, category, priority, remEnabled, remHour, remMin ->
                viewModel.saveTask(
                    editingTask = editingTask,
                    weekKey = weekKey,
                    dayOfWeek = day,
                    title = title,
                    notes = notes,
                    category = category,
                    priority = priority,
                    reminderEnabled = remEnabled,
                    reminderHour = remHour,
                    reminderMinute = remMin,
                    nextSortOrder = currentDayTasksCount
                )
                viewModel.selectDay(day)
                showTaskEditor = false
                editingTask = null
            }
        )
    }

    if (showBriefing) {
        MorningBriefingDialog(
            isMondayWeeklyMode = briefingMondayMode,
            weekKey = WeekDateUtils.currentWeekKey(),
            weekTasks = currentWeekTasks,
            onToggleTask = { viewModel.toggleTask(it) },
            onAddTask = {
                editingTask = null
                editorInitialDay = WeekDateUtils.todayDayOfWeek()
                viewModel.jumpToToday()
                showTaskEditor = true
            },
            onDismiss = {
                showBriefing = false
                LocalPlanCache.markBriefingShownToday(context)
            }
        )
    }

    if (showWeekRollover) {
        WeekRolloverDialog(
            currentWeekKey = selectedWeekKey,
            weekTasks = selectedWeekTasks,
            onCarryOverUnfinished = { list, targetWeek ->
                viewModel.copyTasksToNextWeek(list, targetWeek, resetCompleted = true)
            },
            onCopyAllToNextWeek = { list, targetWeek ->
                viewModel.copyTasksToNextWeek(list, targetWeek, resetCompleted = true)
            },
            onDismiss = { showWeekRollover = false }
        )
    }
}
