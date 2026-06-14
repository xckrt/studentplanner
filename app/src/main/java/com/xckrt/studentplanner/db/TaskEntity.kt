package com.xckrt.studentplanner.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["serverId"], unique = true)
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int? = null,
    val title: String,
    val description: String = "",
    val subjectName: String? = null,
    val deadlineTimestamp: Long? = null,
    val isCompleted: Boolean = false,
    val weight: Int = 1,
    val isFromNote: Boolean = false
)
