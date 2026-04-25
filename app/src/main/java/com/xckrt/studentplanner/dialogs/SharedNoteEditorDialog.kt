package com.xckrt.studentplanner.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.AnimatedSegmentedButton
import com.xckrt.studentplanner.components.KofiHint
import com.xckrt.studentplanner.viewmodels.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedNoteEditorDialog(
    subjectTitle: String,
    viewModel: ScheduleViewModel,
    initialContent: String = "",
    onDismiss: () -> Unit,
    tokenManager: TokenManager,
    onSave: (content: String, deadlineType: String?, customDate: String?, isPrivate: Boolean) -> Unit
) {
    val tutorialStep by viewModel.tutorialStep.collectAsState()
    var content by remember { mutableStateOf(initialContent) }
    var expanded by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val deadlineOptions = mapOf(
        "none" to "Без дедлайна",
        "next_lesson" to "К следующей паре",
        "custom" to "Выбрать дату..."
    )

    var selectedOptionKey by remember { mutableStateOf("next_lesson") }

    val displayDateText = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
        }
    }

    val serverDateText = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                if (datePickerState.selectedDateMillis == null) {
                    selectedOptionKey = "next_lesson"
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    if (datePickerState.selectedDateMillis == null) {
                        selectedOptionKey = "next_lesson"
                    }
                }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Задание: $subjectTitle", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                if (tutorialStep == 4) {
                    KofiHint(text = "Напиши тут что-нибудь важное и нажми Сохранить!", tokenManager = tokenManager)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                AnimatedSegmentedButton(
                    isPrivate = isPrivate,
                    onSelectionChange = { isPrivate = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (isPrivate) "Личное напоминание..." else "Домашнее задание для всех...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val textToShow = if (selectedOptionKey == "custom" && displayDateText != null) {
                        "До: $displayDateText"
                    } else {
                        deadlineOptions[selectedOptionKey] ?: ""
                    }

                    OutlinedTextField(
                        value = textToShow,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дедлайн") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        deadlineOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedOptionKey = key
                                    expanded = false
                                    if (key == "custom") {
                                        showDatePicker = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val typeToSend = if (selectedOptionKey == "none") null else selectedOptionKey
                    val dateToSend = if (selectedOptionKey == "custom") serverDateText else null
                    onSave(content, typeToSend, dateToSend, isPrivate)
                },
                enabled = content.isNotBlank() && (selectedOptionKey != "custom" || datePickerState.selectedDateMillis != null)
            ) {
                Text("СОХРАНИТЬ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.Gray)
            }
        }
    )
}