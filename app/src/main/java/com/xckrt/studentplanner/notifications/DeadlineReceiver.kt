package com.xckrt.studentplanner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xckrt.studentplanner.R // Убедись, что тут твой пакет!

class DeadlineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Неизвестная задача"
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "DEADLINE_CHANNEL",
                "Дедлайны задач",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о горящих дедлайнах"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, "DEADLINE_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏳ Скоро дедлайн!")
            .setContentText("Через 30 минут нужно сдать: $taskTitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Через 30 минут нужно сдать: $taskTitle"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

// Показываем!
        notificationManager.notify(taskId, notification)
    }
}