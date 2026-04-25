package com.xckrt.studentplanner.data

import com.google.gson.annotations.SerializedName
data class ScheduleItem(
    val id: Int,
    val lessonNumber: String,
    val dayOfWeek: Int,
    val startTime: String?,
    val endTime: String?,
    val subject: Subject?,
    val teacher: Teacher?,
    val auditorium: String?,
    val lessonType: String?,
    val originalTeacher: String?,
    val originalSubject: String? = null,
    val originalAuditorium: String? = null,
    val isChange: Boolean = false,
    val isCancelled: Boolean = false,
    val isRoomChangeOnly: Boolean = false,
    @SerializedName("sharedNote")
    val sharedNote: SharedNoteDTO? = null,
    val hasCloudNote: Boolean = false,
    val subGroupName: String? = null,
    val date: String? = null
)

data class Subject(
    val id: Int = 0,
    val title: String?
)

data class Teacher(
    val id: Int = 0,
    val fullName: String?
)

