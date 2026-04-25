package com.xckrt.studentplanner.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, deadlineTimestamp ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?
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
}