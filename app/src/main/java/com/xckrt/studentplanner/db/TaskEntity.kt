package com.xckrt.studentplanner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int? = null,
    val title: String,
    val description: String = "",
    val subjectName: String? = null,
    val deadlineTimestamp: Long? = null,
    val isCompleted: Boolean = false,
    val priority: Int = 0,
    val weight: Int = 1,
    val isFromNote: Boolean = false
)