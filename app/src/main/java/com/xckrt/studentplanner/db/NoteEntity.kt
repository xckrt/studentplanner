package com.xckrt.studentplanner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val subjectTitle: String,
    val noteText: String,
    val isCompleted:Boolean = false,
    val isPrivate: Boolean = false
)