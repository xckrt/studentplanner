package com.xckrt.studentplanner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckrt.studentplanner.viewmodels.TeacherSearchViewModel
import com.xckrt.studentplanner.viewmodels.TeacherStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSearchScreen(
    viewModel: TeacherSearchViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val query by viewModel.searchQuery
    val liveStatus by viewModel.liveStatus
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage
    val filteredTeachers by viewModel.filteredTeachers

    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Поиск преподавателя",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        viewModel.filterList(it)
                        expanded = true
                    },
                    placeholder = { Text("Кого ищем? (например, Ватолина)") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.clearQuery()
                                expanded = false
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            expanded = false
                            focusManager.clearFocus()
                            viewModel.searchTeacher()
                        }
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                if (filteredTeachers.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredTeachers.forEach { teacherName ->
                            DropdownMenuItem(
                                text = { Text(teacherName) },
                                onClick = {
                                    viewModel.filterList(teacherName)
                                    expanded = false
                                    focusManager.clearFocus()
                                    viewModel.searchTeacher()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    error != null -> Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    liveStatus != null -> TeacherLiveCard(status = liveStatus!!)
                    else -> Text(
                        text = "Выберите преподавателя,\nчтобы узнать, где он сейчас",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
@Composable
fun TeacherLiveCard(status: TeacherStatus) {
    val statusColor = when (status) {
        is TeacherStatus.OnLesson -> MaterialTheme.colorScheme.primary
        is TeacherStatus.Absent -> MaterialTheme.colorScheme.error
        is TeacherStatus.Lunch -> Color(0xFFFFA000)
        is TeacherStatus.InBreak -> MaterialTheme.colorScheme.secondary
        is TeacherStatus.Free, is TeacherStatus.DayOff -> MaterialTheme.colorScheme.outline
    }

    val headerText = when (status) {
        is TeacherStatus.OnLesson -> "На занятии (Пара ${status.lessonNumber})"
        is TeacherStatus.Absent -> "ОТСУТСТВУЕТ ПО ЗАМЕНАМ"
        is TeacherStatus.Lunch -> "ОБЕДЕННЫЙ ПЕРЕРЫВ"
        is TeacherStatus.InBreak -> "ПЕРЕМЕНА"
        is TeacherStatus.Free -> "СВОБОДЕН"
        is TeacherStatus.DayOff -> "ВЫХОДНОЙ"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = headerText,
                    color = statusColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (status) {
                is TeacherStatus.OnLesson -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.subject,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Группа: ${status.groupName}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (status.isChange) {
                                Text(
                                    text = "Ведет по замене",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        Text(
                            text = status.room,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                is TeacherStatus.Lunch -> {
                    Text(
                        text = "У преподавателя законный перерыв до 12:30. Приятного аппетита! 🍕",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (status.nextSubject != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Следующая пара: ${status.nextSubject}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!status.nextTime.isNullOrBlank() || !status.nextRoom.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    if (!status.nextTime.isNullOrBlank()) append("Начало в ${status.nextTime}")
                                    if (!status.nextRoom.isNullOrBlank()) {
                                        if (isNotEmpty()) append(" ")
                                        append("(ауд. ${status.nextRoom})")
                                    }
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                is TeacherStatus.Absent -> {
                    Text(
                        text = "Данного преподавателя сегодня нет в колледже (согласно листу замен). Пары отменены или их ведет другой преподаватель.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                is TeacherStatus.InBreak -> {
                    Text(
                        text = "Следующая пара: ${status.nextSubject}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Начало в ${status.nextTime} (ауд. ${status.nextRoom})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is TeacherStatus.Free -> {
                    Text(
                        text = status.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                is TeacherStatus.DayOff -> {
                    Text(
                        text = "Сегодня воскресенье — выходной день! Преподаватели отдыхают ☕",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
