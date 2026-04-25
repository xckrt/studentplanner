package com.xckrt.studentplanner.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.viewmodels.TaskViewModel
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.KofiHint

@Composable
fun AddTaskDialog(viewModel: TaskViewModel, onDismiss: () -> Unit,tokenManager: TokenManager) {
    val tutorialStep by viewModel.tutorialStep.collectAsState()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tutorialStep in 7..9) {
                    KofiHint(
                        text = when (tutorialStep) {
                            7 -> "Жми на календарь и выбери дату!"
                            8 -> "А теперь выбери сложность (вес) задачи!"
                            9 -> "Красава! Теперь жми «СОЗДАТЬ», и перейдем к магии свайпов."
                            else -> ""
                        },
                        tokenManager = tokenManager
                    )
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = viewModel.newTaskTitle,
                    onValueChange = { viewModel.newTaskTitle = it },
                    label = { Text("Что нужно сделать?") },
                    modifier = Modifier.fillMaxWidth()
                )
                PrioritySelector(
                    currentWeight = viewModel.newTaskWeight,
                    onWeightChange = {
                        viewModel.newTaskWeight = it
                        if (tutorialStep == 8) viewModel.nextStep()
                    }
                )
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            TimePickerDialog(context, { _, hh, mm ->
                                val picked = Calendar.getInstance()
                                picked.set(y, m, d, hh, mm)
                                viewModel.newTaskDeadline = picked.timeInMillis
                                if (tutorialStep == 7) viewModel.nextStep()

                            }, 12, 0, true).show()
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(if (viewModel.newTaskDeadline == null) "Установить дедлайн" else "Срок: ${
                        SimpleDateFormat(
                            "dd.MM HH:mm",
                            Locale.getDefault()
                        ).format(Date(viewModel.newTaskDeadline!!))}")
                }

                Text("Предмет:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.subjectsList) { subject ->
                        val isSelected = viewModel.newTaskSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.newTaskSubject = if (isSelected) null else subject },
                            label = { Text(subject) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.addTask()
                if(tutorialStep == 9) viewModel.nextStep()
                onDismiss()
            }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}


data class PriorityItem(val weight: Int, val label: String, val color: Color)

@Composable
fun PrioritySelector(
    currentWeight: Int,
    onWeightChange: (Int) -> Unit
) {
    val priorities = listOf(
        PriorityItem(1, "Можно забить", Color(0xFF81C784)),
        PriorityItem(2, "Обычная", Color(0xFFAED581)),
        PriorityItem(3, "Средняя", Color(0xFFFFD54F)),
        PriorityItem(4, "Важная", Color(0xFFFFB74D)),
        PriorityItem(5, "КРОВЬ И ПОТ", Color(0xFFE57373))
    )

    val selectedItem = priorities.find { it.weight == currentWeight } ?: priorities[0]

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Приоритет задания", style = MaterialTheme.typography.labelMedium)
            Text(
                text = selectedItem.label,
                fontWeight = FontWeight.ExtraBold,
                color = selectedItem.color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            priorities.forEach { item ->
                val isSelected = currentWeight == item.weight
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) item.color else Color.Transparent,
                    animationSpec = tween(300)
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    animationSpec = tween(300)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(backgroundColor)
                        .clickable { onWeightChange(item.weight) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.weight.toString(),
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}