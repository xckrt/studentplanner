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



    var scheduleList = mutableStateOf<List<ScheduleItem>>(emptyList())
    var isLoading = mutableStateOf(false)
    val tutorialStep: StateFlow<Int> = tokenManager.tutorialStep
        .onEach { Log.d("Tutorial", "VM УВИДЕЛА ШАГ: $it") } // Лог при каждом изменении
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Eagerly заставит его загрузиться СРАЗУ
            initialValue = 1
        )
    var noteHistoryList by mutableStateOf<List<NoteHistoryDTO>>(emptyList())
        private set
    var isHistoryLoading by mutableStateOf(false)
        private set
    fun nextStep(targetStep: Int? = null) {
        viewModelScope.launch {
            val next = targetStep ?: (tutorialStep.value + 1)

            Log.d("Tutorial", "Сохраняем шаг: $next")
            tokenManager.saveTutorialStep(next)
        }
    }
    /**
     * ГЛАВНЫЙ МЕТОД: Сохранение заметки + автоматическое создание задачи
     */
    fun saveSharedNote(
        groupId: Int,
        subjectTitle: String,
        content: String,
        deadlineType: String?,
        customDate: String?,
        isPrivate: Boolean // Запятую в конце параметров лучше убрать
    ) {
        viewModelScope.launch {
            try {

                val request = NoteRequest(
                    groupId = groupId,
                    subjectTitle = subjectTitle,
                    content = content,
                    deadlineType = deadlineType,
                    deadlineDate = customDate,
                    isPrivate = isPrivate// Передаем полученный ID
                )

                val response = RetrofitClient.apiService.saveSharedNote(request)

                if (response.isSuccessful) {
                    Log.d("SharedNote", "Облачная заметка сохранена")

                    // --- АВТОМАТИЗАЦИЯ ---
                    if (deadlineType != null) {
                        saveNoteAsTaskLocally(subjectTitle, content)
                    }

                    // Обновляем расписание, чтобы увидеть изменения
                    fetchSchedule(groupId)
                }
            } catch (e: Exception) {
                Log.e("SharedNote", "Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Внутренний метод для создания локальной задачи из заметки
     */
    private suspend fun saveNoteAsTaskLocally(subjectTitle: String, content: String) {
        // 1. Создаем задачу в Room (она получит временный локальный ID)
        val localId = taskDao.insertTask(TaskEntity(
            title = "Заметка: $subjectTitle",
            description = content,
            subjectName = subjectTitle,
            isFromNote = true,
            weight = 1 // Можем задать дефолтный вес для заметок
        )).toInt()

        // 2. Отправляем на сервер
        try {
            val response = RetrofitClient.apiService.createTask(
                TaskDto(
                    id = 0,
                    title = "Заметка: $subjectTitle",
                    description = content,
                    subjectName = subjectTitle,
                    isCompleted = false, // <-- Вот где должно быть false!
                    deadline = null,     // <-- У заметки пока нет дедлайна
                    weight = 1           // <-- Не забываем про вес
                )
            )

            if (response.isSuccessful) {
                // Обрати внимание: если createTask возвращает TaskDto,
                // то нужно писать response.body()?.id
                // Если он возвращает JsonObject/Map, то твой вариант с get("id") сработает.
                // Я напишу безопасный вариант для TaskDto:
                val serverId = response.body()?.id ?: 0

                if (serverId > 0) {
                    // 3. ОБНОВЛЯЕМ ID В ЛОКАЛЬНОЙ БАЗЕ
                    val task = taskDao.getTaskById(localId)
                    if (task != null) {
                        taskDao.deleteTask(task)
                        taskDao.insertTask(task.copy(id = serverId))
                    }
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
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val allChanges = mutableListOf<ScheduleItem>()
                val allGroups = RetrofitClient.apiService.getGroups()
                val groupName = allGroups.find { it.id == groupId }?.name
                    ?: throw Exception("Группа с ID $groupId не найдена на сервере")
                for (i in 0..5) {
                    val dateStr = dateFormat.format(calendar.time)
                    try {
                        val daily = RetrofitClient.apiService.getDailySchedule(groupName, dateStr)
                        allChanges.addAll(daily.map { it.copy(date = dateStr) })
                    } catch (e: Exception) {
                        Log.e("API", "Нет изменений на $dateStr")
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                Log.d("MERGE_DEBUG", "=========================================")
                Log.d("MERGE_DEBUG", "ВСЕГО ИЗМЕНЕНИЙ СКАЧАНО С СЕРВЕРА: ${allChanges.size}")
                allChanges.forEach { ch ->
                    Log.d("MERGE_DEBUG", "[СЕРВЕР ПРИСЛАЛ] День: ${ch.dayOfWeek}, Пара: ${ch.lessonNumber}, Предмет: '${ch.subject?.title}', Отмена: ${ch.isCancelled}")
                }
                Log.d("MERGE_DEBUG", "=========================================")
                val finalSchedule = mutableListOf<ScheduleItem>()
                val cleanBaseLessons = baseLessons.filter { b ->
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
                            splitBaseItems.add(b)
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
                            if (!cIsPg1 && !cIsPg2 && cClean.isNotBlank() && bClean.contains(cClean, true)) return@find true
                            if (!cIsPg1 && !cIsPg2 && cClean.equals("НЕТ", true)) return@find true
                            false
                        }

                        if (bestChange != null) {
                            matchedChanges.add(bestChange)
                            val isCancelled = bestChange.isCancelled || bestChange.subject?.title?.trim()?.equals("НЕТ", true) == true
                            processedSchedule.add(base.copy(
                                isChange = true,
                                isCancelled = isCancelled,
                                originalSubject = base.subject?.title,
                                originalAuditorium = base.auditorium,
                                originalTeacher = base.teacher?.fullName,
                                subject = if (isCancelled) base.subject else bestChange.subject,
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