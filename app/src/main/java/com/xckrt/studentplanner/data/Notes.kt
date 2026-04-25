package com.xckrt.studentplanner.data

data class SharedNoteDTO(
    val id: Int = 0,
    val subjectTitle: String,
    val content: String,
    val deadline: String? = null,
    val authorName: String? = null,
    val updatedAt: String? = null,

)
data class NoteRequest(
    val groupId: Int,
    val subjectTitle: String,
    val content: String,
    val deadlineType: String? = null,
    val deadlineDate: String? = null,
    val isPrivate:Boolean
)