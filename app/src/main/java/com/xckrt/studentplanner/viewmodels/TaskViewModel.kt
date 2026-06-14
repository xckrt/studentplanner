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
import com.xckrt.studentplanner.data.DeadlineCodec
import com.xckrt.studentplanner.data.TaskDto
import com.xckrt.studentplanner.data.TaskSyncManager
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
sealed class TaskDateFilter {
    data object All : TaskDateFilter()
    data class Date(val millis: Long) : TaskDateFilter()
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
            Log.d("Tutorial", "Кофи перевёл тебя на шаг: ${current + 1}")
        }
    }

    fun finishTutorial() {
        viewModelScope.launch {
            tokenManager.setFirstLaunchCompleted()
            tokenManager.saveTutorialStep(13)
            Log.d("Tutorial", "Обучение завершено!")
        }
    }
    private val _filter = MutableStateFlow<TaskDateFilter>(TaskDateFilter.Date(System.currentTimeMillis()))
    val filter: StateFlow<TaskDateFilter> = _filter.asStateFlow()

    val selectedDate: StateFlow<Long> = _filter.map {
        if (it is TaskDateFilter.Date) it.millis else System.currentTimeMillis()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    fun onDateSelected(date: Long) {
        if (tutorialStep.value == 7) nextStep()
        _filter.value = TaskDateFilter.Date(date)
    }
    fun showAll() {
        _filter.value = TaskDateFilter.All
    }
    val combinedTasks: StateFlow<List<TaskDisplayItem>> = combine(
        taskDao.getAllTasks(),
        _filter
    ) { tasks, filter ->
        val today = System.currentTimeMillis()
        val filtered = when (filter) {
            is TaskDateFilter.All -> tasks
            is TaskDateFilter.Date -> tasks.filter { task ->
                val deadline = task.deadlineTimestamp
                when {
                    deadline == null -> true
                    isSameDay(deadline, filter.millis) -> true
                    isSameDay(filter.millis, today) && deadline < today && !task.isCompleted -> true
                    else -> false
                }
            }
        }
        filtered
            .sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenByDescending { it.weight }
                    .thenBy { it.deadlineTimestamp ?: Long.MAX_VALUE }
            )
            .map { TaskDisplayItem.ManualTask(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progress: StateFlow<Float> = combinedTasks.map { list ->
        if (list.isEmpty()) 0f
        else {
            val completed = list.count { item ->
                when (item) {
                    is TaskDisplayItem.ManualTask -> item.task.isCompleted
                    is TaskDisplayItem.NoteTask -> item.note.isCompleted
                }
            }
            completed.toFloat() / list.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    var subjectsList by mutableStateOf<List<String>>(emptyList())
    var newTaskTitle by mutableStateOf("")
    var newTaskDescription by mutableStateOf("")
    var newTaskSubject by mutableStateOf<String?>(null)
    var newTaskWeight by mutableIntStateOf(1)
    var newTaskDeadline by mutableStateOf<Long?>(null)
    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()
    private val syncManager = TaskSyncManager(taskDao, apiService, alarmScheduler)

    init {
        loadSubjectsFromSchedule()
        syncWithServer()
    }

    fun addTask() {
        val title = newTaskTitle.trim()
        if (title.isBlank()) {
            viewModelScope.launch { _errorEvents.emit("Название задачи не может быть пустым") }
            return
        }
        if (tutorialStep.value == 8) nextStep()

        val desc = newTaskDescription
        val subject = newTaskSubject?.takeIf { it.isNotBlank() }
        val weight = newTaskWeight.coerceIn(1, 5)
        val deadline = newTaskDeadline

        clearFields()

        viewModelScope.launch {
            val draft = TaskEntity(
                serverId = null,
                title = title,
                description = desc,
                subjectName = subject,
                weight = weight,
                deadlineTimestamp = deadline,
                isCompleted = false
            )
            val localId = taskDao.insertTask(draft).toInt()
            val localTask = draft.copy(id = localId)
            alarmScheduler.scheduleTaskAlarm(localTask)

            try {
                val response = apiService.createTask(
                    TaskDto(
                        id = 0,
                        title = title,
                        description = desc,
                        subjectName = subject,
                        isCompleted = false,
                        deadline = DeadlineCodec.toServer(deadline),
                        weight = weight
                    )
                )

                if (response.isSuccessful) {
                    val actualServerId = response.body()?.id ?: 0
                    if (actualServerId > 0) {
                        taskDao.updateTaskServerId(localId = localId, serverId = actualServerId)
                    }
                } else {
                    _errorEvents.emit("Не удалось сохранить задачу в облако (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e("TaskSync", "Ошибка сервера: ${e.message}")
                _errorEvents.emit("Нет связи с сервером. Сохранено локально.")
            }
        }
    }

    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            if (tutorialStep.value == 10) nextStep()
            val newStatus = !task.isCompleted
            taskDao.updateTaskStatus(task.id, newStatus)
            if (newStatus) {
                alarmScheduler.cancelTaskAlarm(task)
            } else {
                alarmScheduler.scheduleTaskAlarm(task.copy(isCompleted = false))
            }

            val serverId = task.serverId
            if (serverId != null && serverId > 0) {
                try {
                    val response = apiService.updateTaskStatus(serverId, newStatus)
                    if (!response.isSuccessful) {
                        _errorEvents.emit("Статус не сохранён в облаке (${response.code()})")
                    }
                } catch (e: Exception) {
                    Log.e("TaskSync", "Ошибка обновления статуса в облаке: ${e.message}")
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (tutorialStep.value == 11) nextStep()
            taskDao.deleteTask(task)
            alarmScheduler.cancelTaskAlarm(task)

            val serverId = task.serverId
            if (serverId != null && serverId > 0) {
                try {
                    val response = apiService.deleteTask(serverId)
                    if (!response.isSuccessful) {
                        _errorEvents.emit("Удаление не дошло до сервера (${response.code()})")
                    }
                } catch (e: Exception) {
                    Log.e("TaskSync", "Ошибка удаления на сервере: ${e.message}")
                }
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
            val result = syncManager.sync()
            if (!result.ok) {
                Log.e("TaskSync", "Синхронизация не удалась (нет сети)")
            } else {
                Log.d("TaskSync", result.summary())
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
            } catch (_: Exception) {
            }
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
}

class TaskViewModelFactory(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(taskDao, noteDao, apiService, tokenManager, alarmScheduler) as T
    }
}
