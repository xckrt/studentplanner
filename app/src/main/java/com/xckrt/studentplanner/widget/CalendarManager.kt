package com.xckrt.studentplanner.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.xckrt.studentplanner.data.ScheduleItem
import java.util.*

class CalendarManager(private val context: Context) {
    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )
        return 1L
    }
    fun addLessonToCalendar(lesson: ScheduleItem): Boolean {
        val calendarId = getPrimaryCalendarId() ?: return false
        val startMillis = getNextOccurrenceMillis(lesson.dayOfWeek, lesson.startTime!!)
        val endMillis = getNextOccurrenceMillis(lesson.dayOfWeek, lesson.endTime ?: "23:59")
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, lesson.subject?.title ?: "Пара")
            put(CalendarContract.Events.DESCRIPTION, "${lesson.lessonType} • Преподаватель: ${lesson.teacher?.fullName}")
            put(CalendarContract.Events.EVENT_LOCATION, "ауд. ${lesson.auditorium}")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri != null
    }
    private fun getNextOccurrenceMillis(dayOfWeek: Int, time: String): Long {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].take(2).toInt()
        val calendar = Calendar.getInstance().apply {
            val javaDay = if (dayOfWeek == 7) Calendar.SUNDAY else dayOfWeek + 1
            set(Calendar.DAY_OF_WEEK, javaDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}