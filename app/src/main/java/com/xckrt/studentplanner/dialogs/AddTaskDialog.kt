package com.xckrt.studentplanner.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.KofiHint
import com.xckrt.studentplanner.ui.theme.PriorityBlood
import com.xckrt.studentplanner.ui.theme.PriorityChill
import com.xckrt.studentplanner.ui.theme.PriorityHigh
import com.xckrt.studentplanner.ui.theme.PriorityMid
import com.xckrt.studentplanner.ui.theme.PriorityNormal
import com.xckrt.studentplanner.viewmodels.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(viewModel: TaskViewModel, onDismiss: () -> Unit, tokenManager: TokenManager) {
    val tutorialStep by viewModel.tutorialStep.collectAsState()
    val context = LocalContext.current

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Новая задача",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (tutorialStep in 7..9) {
                    KofiHint(
                        text = when (tutorialStep) {
                            7 -> "Жми на «Дедлайн» и выбери дату/время"
                            8 -> "Теперь выбери приоритет — чем выше вес, тем выше задача в списке"
                            9 -> "Готов? Жми «Создать»."
                            else -> ""
                        },
                        tokenManager = tokenManager
                    )
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = viewModel.newTaskTitle,
                    onValueChange = { viewModel.newTaskTitle = it },
                    label = { Text("Что нужно сделать?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.newTaskDescription,
                    onValueChange = { viewModel.newTaskDescription = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Приоритет",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                PrioritySelector(
                    currentWeight = viewModel.newTaskWeight,
                    onWeightChange = {
                        viewModel.newTaskWeight = it
                        if (tutorialStep == 8) viewModel.nextStep()
                    }
                )

                Spacer(Modifier.height(16.dp))

                DeadlineRow(
                    deadline = viewModel.newTaskDeadline,
                    onPickDeadline = { picked ->
                        viewModel.newTaskDeadline = picked
                        if (tutorialStep == 7) viewModel.nextStep()
                    },
                    onClear = { viewModel.newTaskDeadline = null },
                    onPickClicked = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            TimePickerDialog(context, { _, hh, mm ->
                                val picked = Calendar.getInstance()
                                picked.set(y, m, d, hh, mm)
                                picked.set(Calendar.SECOND, 0)
                                viewModel.newTaskDeadline = picked.timeInMillis
                                if (tutorialStep == 7) viewModel.nextStep()
                            }, 12, 0, true).show()
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )

                Spacer(Modifier.height(16.dp))

                if (viewModel.subjectsList.isNotEmpty()) {
                    Text(
                        "Предмет",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.subjectsList) { subject ->
                            val isSelected = viewModel.newTaskSubject == subject
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.newTaskSubject = if (isSelected) null else subject
                                },
                                label = { Text(subject) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Отмена", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            viewModel.addTask()
                            if (tutorialStep == 9) viewModel.nextStep()
                            onDismiss()
                        },
                        enabled = viewModel.newTaskTitle.isNotBlank(),
                        modifier = Modifier.weight(1.4f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Создать", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineRow(
    deadline: Long?,
    onPickDeadline: (Long) -> Unit,
    onClear: () -> Unit,
    onPickClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPickClicked() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (deadline == null) "Установить дедлайн" else "Срок",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (deadline == null) "Не задан"
                    else SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(deadline)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (deadline != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        }
    }
}

private data class PriorityItem(val weight: Int, val label: String, val color: Color)

@Composable
private fun PrioritySelector(
    currentWeight: Int,
    onWeightChange: (Int) -> Unit
) {
    val priorities = listOf(
        PriorityItem(1, "Чилл", PriorityChill),
        PriorityItem(2, "Обычная", PriorityNormal),
        PriorityItem(3, "Средняя", PriorityMid),
        PriorityItem(4, "Важная", PriorityHigh),
        PriorityItem(5, "КРОВЬ", PriorityBlood)
    )

    val selectedItem = priorities.find { it.weight == currentWeight } ?: priorities[0]

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Вес ${selectedItem.weight}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = selectedItem.label,
                fontWeight = FontWeight.ExtraBold,
                color = selectedItem.color,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            priorities.forEach { item ->
                val isSelected = currentWeight == item.weight
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) item.color else Color.Transparent,
                    animationSpec = tween(280),
                    label = "bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    animationSpec = tween(280),
                    label = "fg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable { onWeightChange(item.weight) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.weight.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
