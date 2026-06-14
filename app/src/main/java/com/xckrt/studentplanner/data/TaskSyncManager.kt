package com.xckrt.studentplanner.data

import android.util.Log
import com.xckrt.studentplanner.db.TaskDao
import com.xckrt.studentplanner.db.TaskEntity
import com.xckrt.studentplanner.notifications.AlarmScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
data class SyncResult(
    val ok: Boolean,
    val pushed: Int = 0,
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0
) {
    val notable: Boolean get() = pushed + added + deleted > 0

    fun summary(): String {
        if (!ok) return "Нет связи с сервером"
        if (!notable) return "Задачи уже синхронизированы"
        val parts = buildList {
            if (added > 0) add("новых: $added")
            if (deleted > 0) add("удалено: $deleted")
            if (pushed > 0) add("отправлено: $pushed")
        }
        return "Синхронизация — " + parts.joinToString(", ")
    }
}
class TaskSyncManager(
    private val taskDao: TaskDao,
    private val apiService: ApiService,
    private val alarmScheduler: AlarmScheduler
) {

    suspend fun sync(): SyncResult = syncMutex.withLock { doSync() }

    private suspend fun doSync(): SyncResult {
        var pushed = 0
        for (draft in taskDao.getAllTasksOnce().filter { it.serverId == null }) {
            try {
                val resp = apiService.createTask(
                    TaskDto(
                        id = 0,
                        title = draft.title,
                        description = draft.description,
                        subjectName = draft.subjectName,
                        isCompleted = draft.isCompleted,
                        deadline = DeadlineCodec.toServer(draft.deadlineTimestamp),
                        weight = draft.weight
                    )
                )
                val sid = if (resp.isSuccessful) resp.body()?.id ?: 0 else 0
                if (sid > 0) {
                    taskDao.updateTaskServerId(localId = draft.id, serverId = sid)
                    pushed++
                }
            } catch (e: Exception) {
                Log.e("TaskSync", "Не удалось отправить черновик '${draft.title}': ${e.message}")
            }
        }
        val cloud = try {
            apiService.getTasks()
        } catch (e: Exception) {
            Log.e("TaskSync", "PULL не удался: ${e.message}")
            return SyncResult(ok = false, pushed = pushed)
        }
        val cloudIds = cloud.map { it.id }.toSet()
        var added = 0
        var updated = 0
        for (dto in cloud) {
            val existed = taskDao.getByServerId(dto.id) != null
            taskDao.upsertFromServer(
                TaskEntity(
                    serverId = dto.id,
                    title = dto.title,
                    description = dto.description ?: "",
                    subjectName = dto.subjectName,
                    isCompleted = dto.isCompleted,
                    weight = dto.weight,
                    deadlineTimestamp = DeadlineCodec.fromServer(dto.deadline)
                )
            )
            if (existed) updated++ else added++
        }
        var deleted = 0
        for (gone in taskDao.getAllTasksOnce().filter { it.serverId != null && it.serverId !in cloudIds }) {
            taskDao.deleteTask(gone)
            alarmScheduler.cancelTaskAlarm(gone)
            deleted++
        }
        for (t in taskDao.getAllTasksOnce()) {
            if (!t.isCompleted && t.deadlineTimestamp != null) {
                alarmScheduler.scheduleTaskAlarm(t)
            }
        }

        return SyncResult(ok = true, pushed = pushed, added = added, updated = updated, deleted = deleted)
    }

    companion object {
        private val syncMutex = Mutex()
    }
}
