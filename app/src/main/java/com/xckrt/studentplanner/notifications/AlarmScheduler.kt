package com.xckrt.studentplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xckrt.studentplanner.db.TaskEntity

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    fun scheduleTaskAlarm(task: TaskEntity) {
        val deadline = task.deadlineTimestamp ?: return
        val thirtyMinutesMillis = 30 * 60 * 1000L
        var triggerTime = deadline - thirtyMinutesMillis
        val currentTime = System.currentTimeMillis()
        if (triggerTime < currentTime) {
            if (deadline > currentTime) {
                triggerTime = currentTime + 10_000L
            } else {
                return
            }
        }
        val intent = Intent(context, DeadlineReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTime,
                pendingIntent
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelTaskAlarm(task: TaskEntity) {
        val intent = Intent(context, DeadlineReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}