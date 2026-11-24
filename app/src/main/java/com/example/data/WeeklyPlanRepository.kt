package com.example.data

import android.content.Context
import com.example.R
import com.example.notifications.AlarmScheduler
import com.example.widget.WidgetUpdater
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

class WeeklyPlanRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        appContext = context.applicationContext
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Firestore erişimi için Google ile giriş yapmalısınız.")
    }

    fun observeTasks(userId: String): Flow<List<WeeklyTask>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Oturum değişti; farklı kullanıcı verisi okunamıyor." }
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WeeklyTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<WeeklyTask> { it.weekKey }
                            .thenBy { it.dayInt }
                            .thenBy { it.completed }
                            .thenBy { it.sortOrder }
                    )
                }
                .onEach { tasks ->
                    LocalPlanCache.saveTasks(appContext, uid, tasks)
                    AlarmScheduler.rescheduleTaskReminders(appContext, tasks)
                    WidgetUpdater.refreshAll(appContext)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSettings(userId: String): Flow<PlanSettings> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Oturum değişti." }
        val docRef = db.collection("users").document(uid)
            .collection("settings").document("preferences")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlanSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: PlanSettings(userId = uid)
                    } else {
                        PlanSettings(userId = uid)
                    }
                }
                .onEach { settings ->
                    LocalPlanCache.saveSettings(appContext, settings)
                    AlarmScheduler.rescheduleMorningAlarm(appContext, settings)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultSettings(userId: String): Result<Unit> {
        val docPath = "users/$userId/settings/preferences"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("settings").document("preferences")
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "morningHour" to 7L,
                    "morningMinute" to 30L,
                    "morningSummaryEnabled" to true,
                    "mondayWeeklyBriefingEnabled" to true,
                    "unlockBriefingEnabled" to true,
                    "weekStartMonday" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveSettings(settings: PlanSettings): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Giriş gerekli"))
        val docPath = "users/$uid/settings/preferences"
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("settings").document("preferences")
            val snap = docRef.get().await()
            val hour = settings.morningHour.coerceIn(0L, 23L)
            val minute = settings.morningMinute.coerceIn(0L, 59L)
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "morningHour" to hour,
                        "morningMinute" to minute,
                        "morningSummaryEnabled" to settings.morningSummaryEnabled,
                        "mondayWeeklyBriefingEnabled" to settings.mondayWeeklyBriefingEnabled,
                        "unlockBriefingEnabled" to settings.unlockBriefingEnabled,
                        "weekStartMonday" to settings.weekStartMonday,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "morningHour" to hour,
                        "morningMinute" to minute,
                        "morningSummaryEnabled" to settings.morningSummaryEnabled,
                        "mondayWeeklyBriefingEnabled" to settings.mondayWeeklyBriefingEnabled,
                        "unlockBriefingEnabled" to settings.unlockBriefingEnabled,
                        "weekStartMonday" to settings.weekStartMonday,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            LocalPlanCache.saveSettings(appContext, settings)
            AlarmScheduler.rescheduleMorningAlarm(appContext, settings)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTask(
        weekKey: String,
        dayOfWeek: Int,
        title: String,
        notes: String?,
        category: TaskCategory,
        priority: TaskPriority,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        sortOrder: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Görev başlığı boş olamaz." }
            val cleanNotes = notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() }
            val validDay = dayOfWeek.coerceIn(1, 7).toLong()
            val validOrder = sortOrder.coerceIn(0L, 10000L)

            val docRef = db.collection("users").document(uid).collection("tasks").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "weekKey" to weekKey,
                "dayOfWeek" to validDay,
                "title" to cleanTitle,
                "notes" to cleanNotes,
                "category" to category.name,
                "priority" to priority.name,
                "completed" to false,
                "reminderEnabled" to (reminderEnabled && reminderHour != null && reminderMinute != null),
                "reminderHour" to if (reminderEnabled) reminderHour?.coerceIn(0, 23)?.toLong() else null,
                "reminderMinute" to if (reminderEnabled) reminderMinute?.coerceIn(0, 59)?.toLong() else null,
                "sortOrder" to validOrder,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        taskId: String,
        weekKey: String,
        dayOfWeek: Int,
        title: String,
        notes: String?,
        category: TaskCategory,
        priority: TaskPriority,
        completed: Boolean,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        sortOrder: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "Görev kimliği boş olamaz." }
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Görev başlığı boş olamaz." }
            val cleanNotes = notes?.trim()?.take(300) ?: ""
            val hasReminder = reminderEnabled && reminderHour != null && reminderMinute != null

            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any>(
                "weekKey" to weekKey,
                "dayOfWeek" to dayOfWeek.coerceIn(1, 7).toLong(),
                "title" to cleanTitle,
                "notes" to cleanNotes,
                "category" to category.name,
                "priority" to priority.name,
                "completed" to completed,
                "reminderEnabled" to hasReminder,
                "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (hasReminder) {
                updates["reminderHour"] = reminderHour!!.coerceIn(0, 23).toLong()
                updates["reminderMinute"] = reminderMinute!!.coerceIn(0, 59).toLong()
            } else {
                updates["reminderHour"] = FieldValue.delete()
                updates["reminderMinute"] = FieldValue.delete()
            }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, completed: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank())
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank())
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Copies selected tasks (unfinished or template routines) into [targetWeekKey]
     * as fresh uncompleted tasks so user can easily update their weekly plan after each week ends.
     */
    suspend fun copyTasksToWeek(
        sourceTasks: List<WeeklyTask>,
        targetWeekKey: String,
        resetCompleted: Boolean = true
    ): Result<Int> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Giriş gerekli"))
        val path = "users/$uid/tasks"
        return try {
            if (sourceTasks.isEmpty()) return Result.success(0)
            val batch = db.batch()
            val colRef = db.collection("users").document(uid).collection("tasks")
            sourceTasks.forEachIndexed { idx, task ->
                val docRef = colRef.document()
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "weekKey" to targetWeekKey,
                    "dayOfWeek" to task.dayOfWeek.coerceIn(1L, 7L),
                    "title" to task.title.trim().take(120),
                    "notes" to task.notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                    "category" to task.categoryEnum.name,
                    "priority" to task.priorityEnum.name,
                    "completed" to if (resetCompleted) false else task.completed,
                    "reminderEnabled" to task.reminderEnabled,
                    "reminderHour" to if (task.reminderEnabled) task.reminderHour else null,
                    "reminderMinute" to if (task.reminderEnabled) task.reminderMinute else null,
                    "sortOrder" to idx.toLong().coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(sourceTasks.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
