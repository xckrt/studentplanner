package com.xckrt.studentplanner.utils

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xckrt.studentplanner.AppForeground
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.AuthManager
import com.xckrt.studentplanner.data.TaskSyncManager
import com.xckrt.studentplanner.db.AppDatabase
import com.xckrt.studentplanner.notifications.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
class TaskSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = TokenManager(applicationContext).token.first()
        if (token.isNullOrBlank()) return Result.success()
        AuthManager.token = token

        return try {
            val dao = AppDatabase.getDatabase(applicationContext).taskDao()
            val manager = TaskSyncManager(dao, apiService, AlarmScheduler(applicationContext))
            val result = manager.sync()

            if (result.ok && result.notable) {
                val msg = result.summary()
                if (AppForeground.isForeground) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    val helper = NotificationHelper(applicationContext)
                    helper.createNotificationChannel()
                    helper.showSyncNotification("Задачи обновлены", msg)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
