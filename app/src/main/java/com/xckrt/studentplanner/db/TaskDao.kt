package com.xckrt.studentplanner.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, weight DESC, deadlineTimestamp ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?
    @Query("SELECT * FROM tasks WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): TaskEntity?
    @Query(
        "SELECT * FROM tasks WHERE serverId IS NULL AND title = :title " +
        "AND ((deadlineTimestamp IS NULL AND :deadline IS NULL) OR deadlineTimestamp = :deadline) " +
        "LIMIT 1"
    )
    suspend fun findOrphanDraft(title: String, deadline: Long?): TaskEntity?

    @Query("SELECT * FROM tasks WHERE subjectName = :subjectName ORDER BY isCompleted ASC")
    fun getTasksBySubject(subjectName: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("UPDATE tasks SET serverId = :serverId WHERE id = :localId")
    suspend fun updateTaskServerId(localId: Int, serverId: Int)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
    @Transaction
    suspend fun upsertFromServer(task: TaskEntity) {
        val serverId = task.serverId
        if (serverId != null) {
            val existing = getByServerId(serverId)
            if (existing != null) {
                updateTask(task.copy(id = existing.id, isFromNote = existing.isFromNote))
                return
            }
            val orphan = findOrphanDraft(task.title, task.deadlineTimestamp)
            if (orphan != null) {
                updateTask(task.copy(id = orphan.id, isFromNote = orphan.isFromNote))
                return
            }
        }
        insertTask(task)
    }
}
