package com.xckrt.studentplanner.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.TokenManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DailyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val tokenManager = TokenManager(applicationContext)
        val groupId = tokenManager.groupId.first() ?: return Result.success()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        var tomorrowDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (tomorrowDayOfWeek == 0) tomorrowDayOfWeek = 7
        val tomorrowDbFormat = tomorrowDayOfWeek
        return try {
            val allLessons = apiService.getSchedule(groupId)
            val hasLessonsTomorrow = allLessons.any { it.dayOfWeek == tomorrowDbFormat }
            if (hasLessonsTomorrow) {
                val helper = NotificationHelper(applicationContext)
                helper.createNotificationChannel()
                helper.showNotification(
                    title = "Собери сумку! 🎒",
                    message = "Завтра у тебя есть пары. Проверь тетради и зарядку."
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}