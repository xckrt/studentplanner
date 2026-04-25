package com.xckrt.studentplanner.utils

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.widget.ScheduleWidget
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleAudioWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AudioWorker", "Просыпаемся, проверяем расписание...")
        val tokenManager = TokenManager(context)
        val isSilentModeEnabled = tokenManager.isSilentModeEnabled.first()
        if (!isSilentModeEnabled) {
            Log.d("AudioWorker", "Фича выключена, спим дальше.")
            return Result.success()
        }
        val audioController = AudioController(context)
        if (!audioController.hasPermission()) {
            return Result.success()
        }
        val groupId = tokenManager.groupId.first()
        if (groupId == null) {
            Log.e("AudioWorker", "ID группы не найден. Отмена.")
            return Result.success()
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentMins = timeToMinutes(currentTime)
        val calendar = Calendar.getInstance()
        var currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (currentDayOfWeek == 0) currentDayOfWeek = 7
        val todayInDbFormat = currentDayOfWeek

        try {
            val allLessons = apiService.getSchedule(groupId)
            Log.d("AudioWorker", "1. Успешно скачано пар всего: ${allLessons.size}")
            Log.d("AudioWorker", "2. Ищем пары для дня: $todayInDbFormat. Время сейчас: $currentTime (в минутах: $currentMins)")
            val todayLessons = allLessons.filter { it.dayOfWeek == todayInDbFormat }
            Log.d("AudioWorker", "3. Пар на сегодняшний день найдено: ${todayLessons.size}")
            val isLessonNow = todayLessons.any { lesson ->
                val startMins = timeToMinutes(lesson.startTime!!)
                val endMins = timeToMinutes(lesson.endTime!!)
                val isMatch = currentMins in startMins..endMins
                Log.d("AudioWorker", " -> Проверяем пару '${lesson.subject?.title}': начало=$startMins, конец=$endMins. Подходит по времени? $isMatch")
                isMatch
            }
            if (isLessonNow) {
                Log.d("AudioWorker", "4. ИТОГ: Идет пара! Вырубаем звук.")
                audioController.mutePhone()
            } else {
                Log.d("AudioWorker", "4. ИТОГ: Пар нет. Включаем звук.")
                audioController.unmutePhone()
            }
            ScheduleWidget().updateAll(context)
            return Result.success()

        } catch (e: Exception) {
            Log.e("AudioWorker", "Ошибка при загрузке расписания: ${e.message}")
            return Result.retry()
        }

    }
    private fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }
}