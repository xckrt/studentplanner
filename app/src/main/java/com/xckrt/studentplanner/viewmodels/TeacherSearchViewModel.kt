package com.xckrt.studentplanner.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xckrt.studentplanner.RetrofitClient
import com.xckrt.studentplanner.data.TeacherAbsenceDto // Твой DTO для отсутствующих
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
sealed class TeacherStatus {
    object Absent : TeacherStatus()
    object DayOff : TeacherStatus()
    data class OnLesson(val subject: String, val groupName: String, val room: String, val lessonNumber: Int?, val isChange: Boolean) : TeacherStatus()
    data class InBreak(val nextSubject: String, val nextTime: String, val nextRoom: String) : TeacherStatus()
    data class Lunch(val nextSubject: String?, val nextTime: String?, val nextRoom: String?) : TeacherStatus()
    data class Free(val message: String) : TeacherStatus()
}

class TeacherSearchViewModel : ViewModel() {

    val searchQuery = mutableStateOf("")
    val liveStatus = mutableStateOf<TeacherStatus?>(null)

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    val allTeachers = mutableStateOf<List<String>>(emptyList())
    val filteredTeachers = mutableStateOf<List<String>>(emptyList())
    private var absentTeachers = emptyList<TeacherAbsenceDto>()

    init {
        loadTeachersList()
        loadAbsentTeachers()
    }

    private fun loadTeachersList() {
        viewModelScope.launch {
            try {
                val list = RetrofitClient.apiService.getAllTeachers()
                allTeachers.value = list
                filteredTeachers.value = list.take(MAX_SUGGESTIONS)
            } catch (e: Exception) {
                Log.e("TeacherSearch", "Не удалось загрузить список преподавателей: ${e.message}")
            }
        }
    }

    private fun loadAbsentTeachers() {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                absentTeachers = RetrofitClient.apiService.getAbsentTeachers(today)
            } catch (e: Exception) {
                Log.e("TeacherSearch", "Ошибка загрузки отсутствующих: ${e.message}")
            }
        }
    }

    fun filterList(query: String) {
        searchQuery.value = query
        liveStatus.value = null
        errorMessage.value = null
        val base = if (query.isBlank()) {
            allTeachers.value
        } else {
            allTeachers.value.filter { it.contains(query, ignoreCase = true) }
        }
        filteredTeachers.value = base.take(MAX_SUGGESTIONS)
    }

    fun clearQuery() {
        filterList("")
    }

    fun searchTeacher() {
        if (searchQuery.value.isBlank()) return
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            liveStatus.value = TeacherStatus.DayOff
            return
        }
        val lastName = searchQuery.value.trim().split(" ").firstOrNull() ?: searchQuery.value
        val isAbsent = absentTeachers.any { it.teacherName.contains(lastName, ignoreCase = true) }
        if (isAbsent) {
            liveStatus.value = TeacherStatus.Absent
            return
        }

        val isLunchWindow = currentMinutes in 710..750
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            liveStatus.value = null

            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val currentTime = sdf.format(Date())

                val response = RetrofitClient.apiService.getTeacherNow(lastName, currentTime)

                liveStatus.value = when {
                    response.status == "found" -> TeacherStatus.OnLesson(
                        subject = response.subject ?: "Занятие",
                        groupName = response.groupName ?: "Неизвестно",
                        room = response.auditorium ?: "???",
                        lessonNumber = response.lessonNumber ?: 0,
                        isChange = response.isChange
                    )
                    isLunchWindow -> TeacherStatus.Lunch(
                        nextSubject = response.nextSubject,
                        nextTime = response.nextTime,
                        nextRoom = response.nextRoom
                    )
                    response.nextSubject != null -> TeacherStatus.InBreak(
                        nextSubject = response.nextSubject,
                        nextTime = response.nextTime ?: "",
                        nextRoom = response.nextRoom ?: "???"
                    )
                    else -> TeacherStatus.Free(response.message ?: "Свободен")
                }

            } catch (e: Exception) {
                errorMessage.value = "Ошибка соединения с сервером"
                Log.e("TeacherSearch", "Ошибка: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    private companion object {
        const val MAX_SUGGESTIONS = 25
    }
}