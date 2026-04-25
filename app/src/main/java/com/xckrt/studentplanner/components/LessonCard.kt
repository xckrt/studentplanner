package com.xckrt.studentplanner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ScheduleItem
import com.xckrt.studentplanner.db.NoteDao
import com.xckrt.studentplanner.db.NoteEntity
import com.xckrt.studentplanner.dialogs.SharedNoteEditorDialog
import com.xckrt.studentplanner.viewmodels.ScheduleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LessonCard(
    lessons: List<ScheduleItem>,
    noteDao: NoteDao,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel,
    currentUserId: Int?,
    tokenManager: TokenManager,
    isOriginalMode: Boolean = false,
    onSaveSharedNote: (subject: String, content: String, deadlineType: String?, customDate: String?, isPrivate: Boolean) -> Unit,
    onFetchHistory: (String) -> Unit = {}
) {
    if (lessons.isEmpty()) return


    val firstLesson = lessons.find {
        !it.startTime.isNullOrBlank() && !it.startTime.startsWith("00")
    } ?: lessons.first()


    val isNow = remember(firstLesson) {
        try {
            val start = firstLesson.startTime ?: ""
            val end = firstLesson.endTime ?: ""
            if (start.isBlank() || end.isBlank()) return@remember false
            val calendar = Calendar.getInstance()
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            fun tToM(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val currentMins = tToM(currentTime)
            val dayDb = calendar.get(Calendar.DAY_OF_WEEK).let { if (it == 1) 7 else it - 1 }
            firstLesson.dayOfWeek == dayDb && currentMins in tToM(start)..tToM(end)
        } catch (e: Exception) { false }
    }


    val hasAnyChanges = lessons.any { it.isChange || it.isCancelled } && !isOriginalMode
    val cardBorder = if (hasAnyChanges) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = cardBorder
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {


            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = firstLesson.startTime?.take(5) ?: "--:--",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = firstLesson.endTime?.take(5) ?: "--:--",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }


            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // ПРАВЫЙ БЛОК: КОНТЕНТ ПАР (Сплит-дизайн)
            Column(modifier = Modifier.weight(1f)) {
                lessons.forEachIndexed { index, lesson ->
                    LessonContentRow(
                        lesson = lesson,
                        index = index,
                        totalLessons = lessons.size,
                        isOriginalMode = isOriginalMode,
                        noteDao = noteDao,
                        viewModel = viewModel,
                        tokenManager = tokenManager,
                        onSaveSharedNote = onSaveSharedNote,
                        onFetchHistory = onFetchHistory
                    )


                    if (index < lessons.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LessonContentRow(
    lesson: ScheduleItem,
    index: Int,
    totalLessons: Int,
    isOriginalMode: Boolean,
    noteDao: NoteDao,
    viewModel: ScheduleViewModel,
    tokenManager: TokenManager,
    onSaveSharedNote: (String, String, String?, String?, Boolean) -> Unit,
    onFetchHistory: (String) -> Unit
) {
    val displaySubject = if (isOriginalMode) lesson.originalSubject ?: lesson.subject?.title ?: "Нет предмета"
    else lesson.subject?.title ?: "Неизвестный предмет"

    val noteEntity by noteDao.getNoteBySubject(displaySubject).collectAsState(initial = null)
    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember(noteEntity) { mutableStateOf(noteEntity?.noteText ?: "") }
    val coroutineScope = rememberCoroutineScope()
    val localHasNote = !noteEntity?.noteText.isNullOrBlank()


    val subGroup = when {
        displaySubject.contains("1 п", ignoreCase = true) -> "П/Г 1"
        displaySubject.contains("2 п", ignoreCase = true) -> "П/Г 2"
        else -> null
    }


    val cleanOriginalSubj = lesson.originalSubject?.replace(Regex("(?i)[12]\\s*п/?г?"), "")?.trim()
    val cleanNewSubj = lesson.subject?.title?.replace(Regex("(?i)[12]\\s*п/?г?"), "")?.replace("ЗАМЕНА", "")?.trim()
    val isCancelled = lesson.isCancelled && !isOriginalMode

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .background(if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else Color.Transparent)
            .padding(12.dp)
    ) {
        Column {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (subGroup != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = subGroup,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isCancelled) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "ОТМЕНА",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    } else if (lesson.isChange && !isOriginalMode) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "ЗАМЕНА",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }


                DiffText(
                    originalText = lesson.originalAuditorium,
                    newText = lesson.auditorium,
                    isOriginalMode = isOriginalMode,
                    textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    newAccentColor = MaterialTheme.colorScheme.primary,
                    isCancelled = isCancelled
                )
            }

            Spacer(modifier = Modifier.height(6.dp))


            DiffText(
                originalText = cleanOriginalSubj,
                newText = cleanNewSubj,
                isOriginalMode = isOriginalMode,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                newAccentColor = MaterialTheme.colorScheme.primary,
                isCancelled = isCancelled
            )

            Spacer(modifier = Modifier.height(2.dp))


            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                DiffText(
                    originalText = lesson.originalTeacher,
                    newText = lesson.teacher?.fullName,
                    isOriginalMode = isOriginalMode,
                    textStyle = MaterialTheme.typography.bodySmall,
                    newAccentColor = MaterialTheme.colorScheme.primary,
                    isCancelled = isCancelled,
                    modifier = Modifier.weight(1f)
                )


                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lesson.hasCloudNote) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).clickable { onFetchHistory(displaySubject) },
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = if (localHasNote) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (localHasNote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showDialog) {
        SharedNoteEditorDialog(
            subjectTitle = displaySubject,
            viewModel = viewModel,
            initialContent = noteText,
            onDismiss = { showDialog = false },
            tokenManager = tokenManager,
            onSave = { newContent, deadlineType, customDate, isPrivate ->
                coroutineScope.launch {
                    noteText = newContent
                    if (newContent.isBlank()) noteDao.deleteNoteBySubject(displaySubject)
                    else noteDao.insertNote(NoteEntity(displaySubject, newContent, false))
                    onSaveSharedNote(displaySubject, newContent, deadlineType, customDate, isPrivate)
                    showDialog = false
                }
            }
        )
    }
}