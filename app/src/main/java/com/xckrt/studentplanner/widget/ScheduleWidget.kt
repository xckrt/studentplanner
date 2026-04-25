package com.xckrt.studentplanner.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.xckrt.studentplanner.MainActivity
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ScheduleItem
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tokenManager = TokenManager(context)
        val groupId = tokenManager.groupId.first()
        var nextLesson: ScheduleItem? = null
        if (groupId != null) {
            try {
                val allLessons = apiService.getSchedule(groupId)
                nextLesson = findNextLesson(allLessons)
            } catch (e: Exception) {
            }
        }
        provideContent {
            GlanceTheme {
                WidgetContent(nextLesson)
            }
        }
    }

    @Composable
    private fun WidgetContent(lesson: ScheduleItem?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .appWidgetBackground()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (lesson == null) {
                    Text(
                        text = "Пар больше нет",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Отдыхай! ✨",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "СЛЕДУЮЩАЯ ПАРА",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = lesson.subject?.title ?: "Предмет",
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${lesson.startTime!!.take(5)}",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = " • ",
                            style = TextStyle(color = GlanceTheme.colors.secondary)
                        )
                        Text(
                            text = "каб. ${lesson.auditorium}",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
    private fun findNextLesson(lessons: List<ScheduleItem>): ScheduleItem? {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK).let { if (it == 1) 6 else it - 2 }
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        return lessons
            .filter { it.dayOfWeek == currentDay }
            .filter { !it.startTime.isNullOrBlank() && it.startTime > currentTime }
            .minByOrNull { it.startTime!! }
    }
}