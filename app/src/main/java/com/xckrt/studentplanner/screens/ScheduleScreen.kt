package com.xckrt.studentplanner.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.HistoryBottomSheet
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
        8 -> "ПОНЕДЕЛЬНИК (След. неделя)" // <-- Магия здесь
        9 -> "ВТОРНИК (След. неделя)"
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
    val groupname = viewModel.getGroupName(groupId)
    val currentUserId by tokenManager.userId.collectAsState(initial = 0)
    val calendar = Calendar.getInstance()
    val currentDayInCalendar = calendar.get(Calendar.DAY_OF_WEEK)
    val changedModeDays = remember { mutableStateListOf<Int>() }
    val groupedSchedule = remember(lessons) {
        lessons
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
            .groupBy { it.dayOfWeek }
    }
    val scrollIndex = remember(groupedSchedule) {
        var index = 0
        val todayIndex = if (currentDayInCalendar == Calendar.SUNDAY) 1 else currentDayInCalendar - 1
        for (day in listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)) {
            if (day == todayIndex) break
            groupedSchedule[day]?.let {
                index += 1
                index += it.groupBy { l -> l.lessonNumber }.size
            }
        }
        index
    }

    LaunchedEffect(lessons, viewModel.weekOffset) {
        if (lessons.isNotEmpty()) {
            if (viewModel.weekOffset == 0 && scrollIndex > 0) {
                listState.animateScrollToItem(scrollIndex)
            } else {
                listState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchSchedule(groupId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Расписание",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                navigationIcon = {
                    val parityText = viewModel.textWeekParity
                    if (parityText.isNotBlank()) {
                        Box(modifier = Modifier.padding(start = 16.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = parityText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    Text(
                        text = viewModel.groupName,
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            WeekNavBar(
                rangeLabel = viewModel.weekRangeLabel,
                isCurrentWeek = viewModel.weekOffset == 0,
                onPrev = { viewModel.changeWeek(groupId, -1) },
                onNext = { viewModel.changeWeek(groupId, +1) },
                onReset = { viewModel.resetWeek(groupId) }
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                        val isShowingOriginal = !changedModeDays.contains(dayOfWeek)

                        item(key = "header_$dayOfWeek") {
                            ScheduleHeader(
                                dayName = getDayName(dayOfWeek),
                                date = scheduleDate,
                                hasChanges = dayHasChanges,
                                isShowingOriginal = isShowingOriginal,
                                onToggleClick = {
                                    if (changedModeDays.contains(dayOfWeek)) {
                                        changedModeDays.remove(dayOfWeek)
                                    } else {
                                        changedModeDays.add(dayOfWeek)
                                    }
                                }
                            )
                        }

                        val lessonsByNumber = dayLessons
                            .groupBy { it.lessonNumber }
                            .toSortedMap()


                        items(lessonsByNumber.keys.toList()) { lessonNum ->
                            val subLessons = lessonsByNumber[lessonNum] ?: emptyList()
                            val displayLessons = if (isShowingOriginal) {
                                subLessons.mapNotNull { lesson ->
                                    if (lesson.isChange && lesson.originalSubject == null) {
                                        null
                                    } else {
                                        lesson.copy(
                                            subject = lesson.subject?.copy(title = lesson.originalSubject ?: lesson.subject.title),
                                            auditorium = lesson.originalAuditorium ?: lesson.auditorium,
                                            teacher = lesson.teacher?.copy(fullName = lesson.originalTeacher ?: lesson.teacher.fullName),
                                            isChange = false,
                                            isCancelled = false
                                        )
                                    }
                                }
                            } else {
                                val parity = viewModel.currentWeekParity
                                val filtered = subLessons.filter { it.weekParity == 0 || it.weekParity == parity }
                                if (filtered.isNotEmpty()) filtered else subLessons
                            }
                            if (displayLessons.isNotEmpty()) {
                                LessonCard(
                                    lessons = displayLessons,
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
            if (viewModel.noteHistoryList.isNotEmpty()) {
                HistoryBottomSheet(
                    historyList = viewModel.noteHistoryList,
                    currentUserId,
                    onDismiss = { viewModel.clearNoteHistory() }
                )
            }
            }
        }
    }
}

@Composable
private fun WeekNavBar(
    rangeLabel: String,
    isCurrentWeek: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Предыдущая неделя",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !isCurrentWeek, onClick = onReset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isCurrentWeek) "Текущая неделя" else "Нажмите, чтобы вернуться к текущей",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Следующая неделя",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}