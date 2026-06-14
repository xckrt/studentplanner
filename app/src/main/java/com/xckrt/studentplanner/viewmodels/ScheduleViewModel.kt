package com.xckrt.studentplanner.viewmodels

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xckrt.studentplanner.RetrofitClient
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.NoteHistoryDTO
import com.xckrt.studentplanner.data.NoteRequest
import com.xckrt.studentplanner.data.ScheduleItem
import com.xckrt.studentplanner.data.TaskDto
import com.xckrt.studentplanner.db.NoteDao
import com.xckrt.studentplanner.db.NoteEntity
import com.xckrt.studentplanner.db.TaskDao
import com.xckrt.studentplanner.db.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleViewModel(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val tokenManager: TokenManager
) : ViewModel() {


    var groupName by mutableStateOf("Загрузка...")
        private set
    var scheduleList = mutableStateOf<List<ScheduleItem>>(emptyList())
    var isLoading = mutableStateOf(false)
    val tutorialStep: StateFlow<Int> = tokenManager.tutorialStep
        .onEach { Log.d("Tutorial", "VM УВИДЕЛА ШАГ: $it") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 1
        )
    var noteHistoryList by mutableStateOf<List<NoteHistoryDTO>>(emptyList())
        private set
    var currentWeekParity by mutableStateOf(0)
        private set
    var weekOffset by mutableStateOf(0)
        private set

    val textWeekParity: String
        get() = getWeekParity(currentWeekParity)
    private fun displayedMonday(): Calendar {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        if (weekOffset == 0 && cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        return cal
    }
    val weekRangeLabel: String
        get() {
            val cal = displayedMonday()
            val fmt = SimpleDateFormat("d MMM", Locale("ru"))
            val mon = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 5)
            val sat = fmt.format(cal.time)
            return "$mon – $sat"
        }

    fun changeWeek(groupId: Int, delta: Int) {
        weekOffset += delta
        fetchSchedule(groupId)
    }

    fun resetWeek(groupId: Int) {
        if (weekOffset != 0) {
            weekOffset = 0
            fetchSchedule(groupId)
        }
    }


    var isHistoryLoading by mutableStateOf(false)
        private set
    fun nextStep(targetStep: Int? = null) {
        viewModelScope.launch {
            val next = targetStep ?: (tutorialStep.value + 1)

            Log.d("Tutorial", "Сохраняем шаг: $next")
            tokenManager.saveTutorialStep(next)
        }
    }
    fun saveSharedNote(
        groupId: Int,
        subjectTitle: String,
        content: String,
        deadlineType: String?,
        customDate: String?,
        isPrivate: Boolean
    ) {
        viewModelScope.launch {
            try {

                val request = NoteRequest(
                    groupId = groupId,
                    subjectTitle = subjectTitle,
                    content = content,
                    deadlineType = deadlineType,
                    deadlineDate = customDate,
                    isPrivate = isPrivate
                )

                val response = RetrofitClient.apiService.saveSharedNote(request)

                if (response.isSuccessful) {
                    Log.d("SharedNote", "Облачная заметка сохранена")
                    if (deadlineType != null) {
                        saveNoteAsTaskLocally(subjectTitle, content)
                    }
                    fetchSchedule(groupId)
                }
            } catch (e: Exception) {
                Log.e("SharedNote", "Ошибка: ${e.message}")
            }
        }
    }
    private suspend fun saveNoteAsTaskLocally(subjectTitle: String, content: String) {
        val draft = TaskEntity(
            serverId = null,
            title = "Заметка: $subjectTitle",
            description = content,
            subjectName = subjectTitle,
            isFromNote = true,
            weight = 1
        )
        val localId = taskDao.insertTask(draft).toInt()

        try {
            val response = RetrofitClient.apiService.createTask(
                TaskDto(
                    id = 0,
                    title = draft.title,
                    description = draft.description,
                    subjectName = draft.subjectName,
                    isCompleted = false,
                    deadline = null,
                    weight = draft.weight
                )
            )

            if (response.isSuccessful) {
                val actualServerId = response.body()?.id ?: 0
                if (actualServerId > 0) {
                    taskDao.updateTaskServerId(localId = localId, serverId = actualServerId)
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Ошибка синхронизации ID: ${e.message}")
        }
    }

    fun fetchSchedule(groupId: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val baseLessons = RetrofitClient.apiService.getSchedule(groupId)

                val allGroups = RetrofitClient.apiService.getGroups()
                val groupName = allGroups.find { it.id == groupId }?.name
                    ?: throw Exception("Группа с ID $groupId не найдена на сервере")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val isCurrentWeek = weekOffset == 0
                val calendar = displayedMonday()
                val displayedMondayStr = dateFormat.format(calendar.time)
                fetchCurrentWeekParity(groupName, displayedMondayStr)
                val daysToFetch = mutableListOf<Pair<String, Int>>()
                for (i in 1..6) {
                    daysToFetch.add(Pair(dateFormat.format(calendar.time), i))
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (isCurrentWeek && (todayDayOfWeek == Calendar.FRIDAY || todayDayOfWeek == Calendar.SATURDAY)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    daysToFetch.add(Pair(dateFormat.format(calendar.time), 8))
                    if (todayDayOfWeek == Calendar.SATURDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        daysToFetch.add(Pair(dateFormat.format(calendar.time), 9))
                    }
                }
                val extendedBaseLessons = baseLessons.toMutableList()
                if (daysToFetch.any { it.second == 8 }) {
                    extendedBaseLessons.addAll(baseLessons.filter { it.dayOfWeek == 1 }.map { it.copy(dayOfWeek = 8) })
                }
                if (daysToFetch.any { it.second == 9 }) {
                    extendedBaseLessons.addAll(baseLessons.filter { it.dayOfWeek == 2 }.map { it.copy(dayOfWeek = 9) })
                }
                val allChanges = mutableListOf<ScheduleItem>()
                for ((dateStr, virtualDay) in daysToFetch) {
                    try {
                        val daily = RetrofitClient.apiService.getDailySchedule(groupName, dateStr)
                        allChanges.addAll(daily.map { it.copy(date = dateStr, dayOfWeek = virtualDay) })
                    } catch (e: Exception) {
                        Log.e("API", "Нет изменений на $dateStr")
                    }
                }
                Log.d("MERGE_DEBUG", "=========================================")
                Log.d("MERGE_DEBUG", "ВСЕГО ИЗМЕНЕНИЙ СКАЧАНО С СЕРВЕРА: ${allChanges.size}")
                allChanges.forEach { ch ->
                    Log.d("MERGE_DEBUG", "[СЕРВЕР ПРИСЛАЛ] День: ${ch.dayOfWeek}, Пара: ${ch.lessonNumber}, Предмет: '${ch.subject?.title}', Отмена: ${ch.isCancelled}")
                }
                Log.d("MERGE_DEBUG", "=========================================")
                val finalSchedule = mutableListOf<ScheduleItem>()
                val cleanBaseLessons = extendedBaseLessons.filter { b ->
                    val t = b.subject?.title ?: ""
                    !t.contains("Кл.час", true) &&
                            !t.contains("Кл час", true) &&
                            !t.contains("Классный", true) &&
                            !t.contains("Разговоры о", true)
                }

                val baseGrouped = cleanBaseLessons.groupBy { "${it.dayOfWeek}_${it.lessonNumber}" }
                val changesGrouped = allChanges.groupBy { "${it.dayOfWeek}_${it.lessonNumber}" }
                val allKeys = (baseGrouped.keys + changesGrouped.keys).distinct()

                for (key in allKeys) {
                    val bItems = baseGrouped[key] ?: emptyList()
                    val cItems = changesGrouped[key] ?: emptyList()

                    if (cItems.isNotEmpty()) {
                        Log.d("MERGE_DEBUG", "--- СЛОТ ВРЕМЕНИ $key ---")
                        Log.d("MERGE_DEBUG", "В базе: ${bItems.joinToString { it.subject?.title ?: "" }}")
                        Log.d("MERGE_DEBUG", "Изменения для этого слота: ${cItems.joinToString { it.subject?.title ?: "" }}")
                    }

                    val splitBaseItems = mutableListOf<ScheduleItem>()
                    bItems.forEach { b ->
                        val safeTitle = b.subject?.title?.replace("п/г", "п_г", true) ?: ""
                        if (safeTitle.contains("/")) {
                            val subjectsRaw = safeTitle.split("/").map { it.replace("п_г", "п/г").trim() }
                            val audsRaw = (b.auditorium?.replace("п/г", "п_г", true) ?: "").split("/").map { it.replace("п_г", "п/г").trim() }
                            val teachRaw = (b.teacher?.fullName?.replace("п/г", "п_г", true) ?: "").split("/").map { it.replace("п_г", "п/г").trim() }
                            var subj1 = subjectsRaw.getOrNull(0) ?: b.subject?.title ?: ""
                            var subj2 = subjectsRaw.getOrNull(1) ?: b.subject?.title ?: ""
                            var aud1 = audsRaw.getOrNull(0) ?: b.auditorium
                            var aud2 = audsRaw.getOrNull(1) ?: audsRaw.getOrNull(0) ?: b.auditorium
                            var teach1 = teachRaw.getOrNull(0) ?: b.teacher?.fullName
                            var teach2 = teachRaw.getOrNull(1) ?: teachRaw.getOrNull(0) ?: b.teacher?.fullName

                            if (subj1.contains("2 п", true) || subj2.contains("1 п", true)) {
                                val ts = subj1; subj1 = subj2; subj2 = ts
                                val ta = aud1; aud1 = aud2; aud2 = ta
                                val tt = teach1; teach1 = teach2; teach2 = tt
                            }
                            if (!subj1.contains("1 п", true)) subj1 = "$subj1 1 п/г"
                            if (!subj2.contains("2 п", true)) subj2 = "$subj2 2 п/г"

                            splitBaseItems.add(b.copy(subject = b.subject?.copy(title = subj1), auditorium = aud1, teacher = b.teacher?.copy(fullName = teach1)))
                            splitBaseItems.add(b.copy(subject = b.subject?.copy(title = subj2), auditorium = aud2, teacher = b.teacher?.copy(fullName = teach2)))
                        } else {
                            val bTitle = b.subject?.title ?: ""
                            val isBaseAlreadySubgroup = bTitle.contains("1 п", true) || bTitle.contains("2 п", true)
                            val hasSubgroupChanges = cItems.any {
                                it.subject?.title?.contains("1 п", true) == true ||
                                        it.subject?.title?.contains("2 п", true) == true
                            }
                            if (hasSubgroupChanges && !isBaseAlreadySubgroup) {
                                splitBaseItems.add(b.copy(
                                    subject = b.subject?.copy(title = "$bTitle 1 п/г"),
                                    originalSubject = bTitle
                                ))
                                splitBaseItems.add(b.copy(
                                    subject = b.subject?.copy(title = "$bTitle 2 п/г"),
                                    originalSubject = bTitle
                                ))
                            } else  {
                                splitBaseItems.add(b)
                            }
                        }
                    }

                    if (cItems.isEmpty()) {
                        finalSchedule.addAll(splitBaseItems)
                        continue
                    }

                    val matchedChanges = mutableSetOf<ScheduleItem>()
                    val processedSchedule = mutableListOf<ScheduleItem>()

                    for (base in splitBaseItems) {
                        val bTitle = base.subject?.title ?: ""
                        val bIsPg1 = bTitle.contains("1 п", true)
                        val bIsPg2 = bTitle.contains("2 п", true)
                        val bClean = bTitle.replace(Regex("(?i)[12]\\s*п/?г?"), "").trim()

                        val bestChange = cItems.find { c ->
                            if (matchedChanges.contains(c)) return@find false
                            val cTitle = c.subject?.title ?: ""
                            val cIsPg1 = cTitle.contains("1 п", true)
                            val cIsPg2 = cTitle.contains("2 п", true)
                            val cClean = cTitle.replace(Regex("(?i)[12]\\s*п/?г?"), "").replace("ЗАМЕНА", "", true).trim()
                            if (bIsPg1 && cIsPg1) return@find true
                            if (bIsPg2 && cIsPg2) return@find true
                            if (!bIsPg1 && !bIsPg2 && !cIsPg1 && !cIsPg2) return@find true
                            if (!cIsPg1 && !cIsPg2 && cClean.isNotBlank() && bClean.contains(cClean, true)) return@find true
                            if (!cIsPg1 && !cIsPg2 && cClean.equals("НЕТ", true)) return@find true
                            false
                        }

                        if (bestChange != null) {
                            matchedChanges.add(bestChange)
                            val changeTitle = bestChange.subject?.title?.trim().orEmpty()
                            val isCancelled = bestChange.isCancelled || changeTitle.equals("НЕТ", true)
                            val isRoomOnly = !isCancelled && (
                                changeTitle.startsWith("Смена аудитории", true) ||
                                changeTitle.startsWith("Смена кабинета", true)
                            )
                            processedSchedule.add(base.copy(
                                isChange = true,
                                isCancelled = isCancelled,
                                isRoomChangeOnly = isRoomOnly,
                                originalSubject = base.originalSubject ?: base.subject?.title,
                                originalAuditorium = base.auditorium,
                                originalTeacher = base.teacher?.fullName,
                                subject = if (isCancelled || isRoomOnly) base.subject else bestChange.subject,
                                auditorium = bestChange.auditorium?.takeIf { it.isNotBlank() } ?: base.auditorium,
                                teacher = bestChange.teacher?.takeIf { it.fullName?.isNotBlank() == true } ?: base.teacher,
                                date = bestChange.date
                            ))
                        } else {
                            processedSchedule.add(base)
                        }
                    }

                    val correctBaseTimeLesson = splitBaseItems.find {
                        !it.startTime.isNullOrBlank() && !it.startTime.startsWith("00:00")
                    }
                    val validStartTime = correctBaseTimeLesson?.startTime
                    val validEndTime = correctBaseTimeLesson?.endTime

                    for (c in cItems) {
                        if (!matchedChanges.contains(c)) {
                            val isCancelled = c.isCancelled || c.subject?.title?.trim()?.equals("НЕТ", true) == true
                            val actualStart = if (c.startTime.isNullOrBlank() || c.startTime.startsWith("00:00")) validStartTime else c.startTime
                            val actualEnd = if (c.endTime.isNullOrBlank() || c.endTime.startsWith("00:00")) validEndTime else c.endTime

                            processedSchedule.add(c.copy(
                                isChange = true,
                                isCancelled = isCancelled,
                                originalSubject = null,
                                originalAuditorium = null,
                                originalTeacher = null,
                                startTime = actualStart,
                                endTime = actualEnd
                            ))
                        }
                    }
                    finalSchedule.addAll(processedSchedule)
                }

                scheduleList.value = finalSchedule.sortedWith(compareBy(
                    { it.dayOfWeek },
                    { it.lessonNumber },
                    { it.subject?.title?.contains("2 п", true) == true }
                ))

            } catch (e: Exception) {
                Log.e("API", "Критическая ошибка: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    fun fetchCurrentWeekParity(groupName: String, date: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getWeekParity(groupName, date)
                if (response.weekParity != 0) currentWeekParity = response.weekParity
                Log.d("WeekParity","Сервер выдал ${response.weekParity}")
            } catch (e: Exception) {
                Log.e("Parity", "Ошибка загрузки четности: ${e.message}")
            }
        }
    }
    fun clearNoteHistory() {
        noteHistoryList = emptyList()
    }
    fun fetchNoteHistory(groupId: Int, subjectTitle: String) {
        viewModelScope.launch {
            isHistoryLoading = true
            try {
                noteHistoryList = RetrofitClient.apiService.getNoteHistory(groupId, subjectTitle)
            } catch (e: Exception) {
                noteHistoryList = emptyList()
            } finally {
                isHistoryLoading = false
            }
        }
    }
    fun getWeekParity(weekParity: Int): String {
        Log.d("weekParity","$weekParity")
        return when (weekParity) {
            1 -> "ЧИСЛИТЕЛЬ"
            2 -> "ЗНАМЕНАТЕЛЬ"
            else -> ""
        }
    }
    fun getGroupName(groupId: Int) {
        viewModelScope.launch {
            try {
                val name = RetrofitClient.apiService.getGroupName(groupId)
                groupName = name.name
            } catch (e: Exception) {
                Log.e("GroupName", "Ошибка загрузки имени группы: ${e.message}")
                groupName = "Ошибка"
            }
        }
    }
}
class ScheduleViewModelFactory(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScheduleViewModel(noteDao, taskDao,tokenManager) as T
    }
}