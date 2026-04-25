package com.xckrt.studentplanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.LessonCard
import com.xckrt.studentplanner.components.ScheduleHeader
import com.xckrt.studentplanner.db.NoteDao
import com.xckrt.studentplanner.viewmodels.ScheduleViewModel
import java.util.Calendar

fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "ПОНЕДЕЛЬНИК"
        2 -> "ВТОРНИК"
        3 -> "СРЕДА"
        4 -> "ЧЕТВЕРГ"
        5 -> "ПЯТНИЦА"
        6 -> "СУББОТА"
        else -> "ВОСКРЕСЕНЬЕ"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    groupId: Int,
    noteDao: NoteDao,
    tokenManager: TokenManager
) {
    val lessons by viewModel.scheduleList
    val loading by viewModel.isLoading
    val listState = rememberLazyListState()
    val currentUserId by tokenManager.userId.collectAsState(initial = 0)
    val calendar = Calendar.getInstance()
    val currentDayInCalendar = calendar.get(Calendar.DAY_OF_WEEK)
    val originalModeDays = remember { mutableStateListOf<Int>() }
    val groupedSchedule = remember(lessons) {
        lessons
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
            .groupBy { it.dayOfWeek }
    }
    val scrollIndex = remember(groupedSchedule) {
        var index = 0
        val todayIndex = if (currentDayInCalendar == 1) 7 else currentDayInCalendar - 1

        for (day in listOf(1, 2, 3, 4, 5, 6, 7)) {
            if (day == todayIndex) break
            groupedSchedule[day]?.let {
                index += 1
                index += it.groupBy { l -> l.lessonNumber }.size
            }
        }
        index
    }

    LaunchedEffect(lessons) {
        if (lessons.isNotEmpty() && scrollIndex > 0) {
            listState.animateScrollToItem(scrollIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchSchedule(groupId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Расписание",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Группа №$groupId",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (lessons.isEmpty()) {
                Text(
                    text = "Расписание не загружено",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedSchedule.forEach { (dayOfWeek, dayLessons) ->
                        val scheduleDate = dayLessons.firstOrNull { !it.date.isNullOrBlank() }?.date
                        val dayHasChanges = dayLessons.any { it.isChange || it.isCancelled }
                        val isShowingOriginal = originalModeDays.contains(dayOfWeek)


                        item(key = "header_$dayOfWeek") {
                            ScheduleHeader(
                                dayName = getDayName(dayOfWeek),
                                date = scheduleDate,
                                hasChanges = dayHasChanges,
                                isShowingOriginal = isShowingOriginal,
                                onToggleClick = {
                                    if (isShowingOriginal) originalModeDays.remove(dayOfWeek)
                                    else originalModeDays.add(dayOfWeek)
                                }
                            )
                        }

                        val lessonsByNumber = dayLessons
                            .groupBy { it.lessonNumber }
                            .toSortedMap()


                        items(lessonsByNumber.keys.toList()) { lessonNum ->
                            val subLessons = lessonsByNumber[lessonNum] ?: emptyList()

                            LessonCard(
                                lessons = subLessons,
                                noteDao = noteDao,
                                viewModel = viewModel,
                                currentUserId = currentUserId,
                                tokenManager = tokenManager,
                                isOriginalMode = isShowingOriginal,
                                onSaveSharedNote = { subject, content, type, date, isPriv ->
                                    viewModel.saveSharedNote(groupId, subject, content, type, date, isPriv)
                                },
                                onFetchHistory = { subjectTitle ->
                                    viewModel.fetchNoteHistory(groupId, subjectTitle)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}