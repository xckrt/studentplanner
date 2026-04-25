package com.xckrt.studentplanner.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.data.TaskDto
import com.xckrt.studentplanner.db.NoteDao
import com.xckrt.studentplanner.db.NoteEntity
import com.xckrt.studentplanner.db.TaskDao
import com.xckrt.studentplanner.db.TaskEntity
import com.xckrt.studentplanner.notifications.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

sealed class TaskDisplayItem {
    data class ManualTask(val task: TaskEntity) : TaskDisplayItem()
    data class NoteTask(val note: NoteEntity) : TaskDisplayItem()
}

class TaskViewModel(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    val tutorialStep: StateFlow<Int> = tokenManager.tutorialStep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val isFirstLaunch: StateFlow<Boolean> = tokenManager.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun nextStep() {
        viewModelScope.launch {
            val current = tutorialStep.value
            tokenManager.saveTutorialStep(current + 1)
            Log.d("Tutorial", "Кофи перевел тебя на шаг: ${current + 1}")
        }
    }

    fun finishTutorial() {
        viewModelScope.launch {
            tokenManager.setFirstLaunchCompleted()
            tokenManager.saveTutorialStep(11)
            Log.d("Tutorial", "Обучение завершено!")
        }
    }
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val combinedTasks: StateFlow<List<TaskDisplayItem>> = combine(
        taskDao.getAllTasks(),
        _selectedDate
    ) { tasks, date ->
        tasks.filter { task ->
            task.deadlineTimestamp == null || isSameDay(task.deadlineTimestamp, date)
        }
            .sortedWith(compareByDescending<TaskEntity> { it.weight }.thenBy { it.isCompleted })
            .map { TaskDisplayItem.ManualTask(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progress: StateFlow<Float> = combinedTasks.map { list ->
        if (list.isEmpty()) 0f
        else {
            val completed = list.count { (it as TaskDisplayItem.ManualTask).task.isCompleted }
            completed.toFloat() / list.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    var subjectsList by mutableStateOf<List<String>>(emptyList())
    var newTaskTitle by mutableStateOf("")
    var newTaskDescription by mutableStateOf("")
    var newTaskSubject by mutableStateOf<String?>(null)
    var newTaskWeight by mutableIntStateOf(1)
    var newTaskDeadline by mutableStateOf<Long?>(null)

    init {
        loadSubjectsFromSchedule()
        syncWithServer()
    }
    fun onDateSelected(date: Long) {
        if (tutorialStep.value == 7) nextStep()
        _selectedDate.value = date
    }

    fun addTask() {
        if (newTaskTitle.isBlank()) return
        if (tutorialStep.value == 8) nextStep()

        val title = newTaskTitle
        val desc = newTaskDescription
        val subject = newTaskSubject
        val weight = newTaskWeight
        val deadline = newTaskDeadline

        clearFields()

        viewModelScope.launch {
            val tempTask = TaskEntity(
                serverId = 0,
                title = title,
                description = desc,
                subjectName = subject,
                weight = weight,
                deadlineTimestamp = deadline,
                isCompleted = false
            )
            val localId = taskDao.insertTask(tempTask).toInt()
            alarmScheduler.scheduleTaskAlarm(tempTask.copy(id = localId))

            try {
                val response = apiService.createTask(
                    TaskDto(
                        id = 0,
                        title = title,
                        description = desc,
                        subjectName = subject,
                        isCompleted = false,
                        deadline = formatDeadlineForServer(deadline),
                        weight = weight
                    )
                )

                if (response.isSuccessful) {
                    val actualServerId = response.body()?.id ?: 0
                    if (actualServerId > 0) {
                        taskDao.updateTaskServerId(localId = localId, serverId = actualServerId)
                    }
                }
            } catch (e: Exception) {
                Log.e("Sync", "Ошибка сервера: ${e.message}")
            }
        }
    }

    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            if (tutorialStep.value == 10) nextStep()
            val newStatus = !task.isCompleted
            taskDao.updateTaskStatus(task.id, newStatus)
            alarmScheduler.cancelTaskAlarm(task)

            if (task.serverId != null && task.serverId > 0) {
                try {
                    apiService.updateTaskStatus(task.serverId, newStatus)
                } catch (e: Exception) {
                    Log.e("Sync", "Ошибка обновления статуса в облаке")
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (tutorialStep.value == 11) nextStep()
            taskDao.deleteTask(task)
            alarmScheduler.cancelTaskAlarm(task)
            try {
                apiService.deleteTask(task.serverId!!)
            } catch (e: Exception) {
                Log.e("Sync", "Ошибка удаления на сервере")
            }
        }
    }

    fun toggleNoteStatus(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNoteStatus(note.subjectTitle, !note.isCompleted)
        }
    }
    fun syncWithServer() {
        viewModelScope.launch {
            try {
                val cloudTasks = apiService.getTasks()
                cloudTasks.forEach { dto ->
                    taskDao.insertTask(TaskEntity(
                        serverId = dto.id,
                        title = dto.title,
                        description = dto.description ?: "",
                        subjectName = dto.subjectName,
                        isCompleted = dto.isCompleted,
                        weight = dto.weight,
                        deadlineTimestamp = parseServerDeadline(dto.deadline)
                    ))
                }
            } catch (e: Exception) {
                Log.e("Sync", "Синхронизация не удалась")
            }
        }
    }

    private fun loadSubjectsFromSchedule() {
        viewModelScope.launch {
            try {
                val groupId = tokenManager.groupId.first() ?: 0
                if (groupId > 0) {
                    val schedule = apiService.getSchedule(groupId)
                    subjectsList = schedule.mapNotNull { it.subject?.title }.distinct().sorted()
                }
            } catch (e: Exception) { }
        }
    }

    private fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun clearFields() {
        newTaskTitle = ""; newTaskDescription = ""; newTaskSubject = null
        newTaskWeight = 1; newTaskDeadline = null
    }

    private fun formatDeadlineForServer(millis: Long?): String? {
        if (millis == null) return null
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    private fun parseServerDeadline(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val cleanString = dateString.substringBefore("+").substringBefore("Z").substringBefore(".")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.parse(cleanString)?.time
        } catch (e: Exception) { null }
    }
}

class TaskViewModelFactory(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(taskDao, noteDao, apiService, tokenManager, alarmScheduler) as T
    }
}