package com.xckrt.studentplanner.data

import com.google.gson.annotations.SerializedName

data class NoteHistoryDTO(
    val id: Int,
    val contentBefore: String,
    val contentAfter: String,
    val changeTimestamp: String,
    val authorId: Int,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    @SerializedName("isPrivate") val isPrivate: Boolean = false,
    @SerializedName("hasTask") val hasTask: Boolean = false,
    @SerializedName("isTaskCompleted") val isTaskCompleted: Boolean = false
)