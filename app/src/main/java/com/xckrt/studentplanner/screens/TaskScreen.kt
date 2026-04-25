package com.xckrt.studentplanner.screens

import com.xckrt.studentplanner.dialogs.AddTaskDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.db.NoteEntity
import com.xckrt.studentplanner.db.TaskEntity
import com.xckrt.studentplanner.viewmodels.TaskDisplayItem
import com.xckrt.studentplanner.viewmodels.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel,tokenManager: TokenManager) {
    val displayItems by viewModel.combinedTasks.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val subjects = viewModel.subjectsList
    val selectedDate by viewModel.selectedDate.collectAsState()

    // СОСТОЯНИЯ
    var isCelebrationDismissed by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Логика обучения здесь больше НЕ НУЖНА (мы вынесем её выше)
    val tutorialStep by viewModel.tutorialStep.collectAsState(initial = 1)

    LaunchedEffect(displayItems.size) { isCelebrationDismissed = false }
    if (progress < 1f) { isCelebrationDismissed = false }

    val filteredItems = if (selectedSubject == null) displayItems
    else displayItems.filter {
        when(it) {
            is TaskDisplayItem.ManualTask -> it.task.subjectName == selectedSubject
            is TaskDisplayItem.NoteTask -> it.note.subjectTitle == selectedSubject
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Планировщик", fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "Осталось дел: ${displayItems.count { item ->
                                    when(item) {
                                        is TaskDisplayItem.ManualTask -> !item.task.isCompleted
                                        is TaskDisplayItem.NoteTask -> !item.note.isCompleted
                                    }
                                }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (tutorialStep == 6) viewModel.nextStep()
                        showAddDialog = true
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChip(selected = selectedSubject == null, onClick = { selectedSubject = null }, label = { Text("Все") }) }
                    items(subjects) { subject ->
                        FilterChip(selected = selectedSubject == subject, onClick = { selectedSubject = subject }, label = { Text(subject) })
                    }
                }
                DatePickerStrip(selectedDate = selectedDate, onDateSelected = { viewModel.onDateSelected(it) })

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Твой прогресс", style = MaterialTheme.typography.titleSmall)
                            Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = animateFloatAsState(targetValue = progress).value,
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { item ->
                        when(item) {
                            is TaskDisplayItem.ManualTask -> "task_${item.task.id}"
                            is TaskDisplayItem.NoteTask -> "note_${item.note.subjectTitle}"
                        }
                    }) { item ->
                        when (item) {
                            is TaskDisplayItem.ManualTask -> TaskCard(
                                task = item.task,
                                onToggle = {
                                    if (tutorialStep == 9) viewModel.nextStep()
                                    viewModel.toggleTaskStatus(item.task)
                                },
                                onDelete = { viewModel.deleteTask(item.task) }
                            )
                            is TaskDisplayItem.NoteTask -> NoteAsTaskCard(
                                note = item.note,
                                onToggle = { viewModel.toggleNoteStatus(item.note) }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = progress == 1f && displayItems.isNotEmpty() && !isCelebrationDismissed,
            enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()
        ) {
            CelebrationOverlay(onDismiss = { isCelebrationDismissed = true })
        }
    }
    if (showAddDialog) {
        AddTaskDialog(viewModel = viewModel, onDismiss = { showAddDialog = false }, tokenManager = tokenManager)
    }
}


@Composable
fun NoteAsTaskCard(
    note: NoteEntity,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Чекбокс для заметки-задачи
            Icon(
                imageVector = if (note.isCompleted) Icons.Default.CheckCircle
                else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onToggle() },
                tint = if (note.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = note.subjectTitle,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null,
                    color = if (note.isCompleted) Color.Gray else Color.Unspecified
                )
                Text(
                    text = note.noteText,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = "ИЗ УРОКА",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
@Composable
fun DatePickerStrip(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    // Запоминаем текущий день, чтобы подсветить его точкой
    val todayMillis = System.currentTimeMillis()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), // Чуть больше воздуха сверху и снизу
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(14) { i -> // Показываем 2 недели
            val date = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val isSelected = isSameDay(date.timeInMillis, selectedDate)
            val isToday = isSameDay(date.timeInMillis, todayMillis)

            // 1. Анимация размера (эффект «выпрыгивания»)
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            // 2. Плавная смена цвета фона
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                animationSpec = tween(300)
            )

            // 3. Плавная смена цвета текста
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(300)
            )

            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(84.dp) // Вытянутая форма капсулы
                    .scale(scale)  // Применяем пружинистую анимацию
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .clickable { onDateSelected(date.timeInMillis) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // День недели (ПН, ВТ)
                    Text(
                        text = SimpleDateFormat("E", Locale("ru")).format(date.time).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) contentColor.copy(alpha = 0.8f) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Число (12, 13)
                    Text(
                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Индикатор "Сегодня" (Точка)
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        // Пустышка, чтобы текст не прыгал по высоте в другие дни
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

// Вспомогательная функция для сравнения дат (можно положить в Utils)
fun isSameDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
@Composable
fun CelebrationOverlay(onDismiss: () -> Unit) {
    // Анимация пульсации для иконки 🎉
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                    )
                )
            )
            .clickable(enabled = false) { }, // Блокируем клики по нижним слоям
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Эмодзи с анимацией пульсации
            Text(
                text = "🎉",
                fontSize = 100.sp,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "ВСЁ ГОТОВО!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                "Ты закрыл все задачи и заметки. Время законного отдыха!",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // РАБОЧАЯ КНОПКА
            Button(
                onClick = onDismiss, // ВЫЗЫВАЕМ ЗАКРЫТИЕ
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text(
                    "КРУТО, ПОНЯЛ!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}


@Composable
fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 300f
    val swipeProgress = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    val fakeOffsetY = abs(offsetX.value) * 0.15f
    val rotationAngle = offsetX.value / 20f
    val bgColor = when {
        offsetX.value > 0 -> Color(0xFF81C784).copy(alpha = swipeProgress)
        offsetX.value < 0 -> Color(0xFFE57373).copy(alpha = swipeProgress)
        else -> Color.Transparent
    }
    val icon = when {
        offsetX.value > 20f -> Icons.Default.CheckCircle
        offsetX.value < -20f -> Icons.Default.Delete
        else -> null
    }
    val alignment = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp),
            contentAlignment = alignment
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .scale(0.5f + (swipeProgress * 0.5f))
                )
            }
        }
        val priorityColor = when (task.weight) {
            5 -> Color.Red
            4 -> Color(0xFFFF9800)
            3 -> Color(0xFFFFEB3B)
            else -> Color.Transparent
        }

        val backgroundColor = if (task.isCompleted)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), fakeOffsetY.roundToInt()) }
                .graphicsLayer { rotationZ = rotationAngle }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > swipeThreshold) {

                                    offsetX.animateTo(1000f, tween(300))
                                    onToggle()
                                    offsetX.snapTo(0f)
                                } else if (offsetX.value < -swipeThreshold) {
                                    offsetX.animateTo(-1000f, tween(300))
                                    onDelete()
                                    offsetX.snapTo(0f)
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = if (task.weight >= 4 && !task.isCompleted)
                BorderStroke(1.dp, priorityColor.copy(alpha = 0.5f))
            else null
        ) {

            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.weight > 1 && !task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(priorityColor)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onToggle() },
                    tint = if (task.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f, fill = false),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) Color.Gray else Color.Unspecified
                        )

                        task.deadlineTimestamp?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (task.weight >= 4) priorityColor else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (task.subjectName != null) {
                        Text(
                            text = task.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = if (task.isCompleted) Color.Gray else Color.Unspecified
                        )
                    }

                    Row(Modifier.padding(top = 4.dp)) {
                        if (task.isFromNote) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "ИЗ УРОКА",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        if (task.weight >= 5 && !task.isCompleted) {
                            Text(
                                text = "ВАЖНО",
                                color = Color.Red,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}