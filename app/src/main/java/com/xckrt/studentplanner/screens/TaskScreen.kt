package com.xckrt.studentplanner.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.components.KofiMascot
import com.xckrt.studentplanner.db.NoteEntity
import com.xckrt.studentplanner.db.TaskEntity
import com.xckrt.studentplanner.dialogs.AddTaskDialog
import com.xckrt.studentplanner.ui.theme.PriorityBlood
import com.xckrt.studentplanner.ui.theme.PriorityChill
import com.xckrt.studentplanner.ui.theme.PriorityHigh
import com.xckrt.studentplanner.ui.theme.PriorityMid
import com.xckrt.studentplanner.ui.theme.PriorityNormal
import com.xckrt.studentplanner.viewmodels.TaskDateFilter
import com.xckrt.studentplanner.viewmodels.TaskDisplayItem
import com.xckrt.studentplanner.viewmodels.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel, tokenManager: TokenManager) {
    val displayItems by viewModel.combinedTasks.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val subjects = viewModel.subjectsList
    val filter by viewModel.filter.collectAsState()

    var isCelebrationDismissed by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val tutorialStep by viewModel.tutorialStep.collectAsState(initial = 1)
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { msg -> snackbarHost.showSnackbar(msg) }
    }

    LaunchedEffect(displayItems.size) { isCelebrationDismissed = false }
    if (progress < 1f) isCelebrationDismissed = false

    val filteredItems = if (selectedSubject == null) displayItems
    else displayItems.filter {
        when (it) {
            is TaskDisplayItem.ManualTask -> it.task.subjectName == selectedSubject
            is TaskDisplayItem.NoteTask -> it.note.subjectTitle == selectedSubject
        }
    }

    val remaining = displayItems.count { item ->
        when (item) {
            is TaskDisplayItem.ManualTask -> !item.task.isCompleted
            is TaskDisplayItem.NoteTask -> !item.note.isCompleted
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Задачи",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (remaining == 0) "Свободен. Заслужил отдых 🎉"
                                else "Осталось: $remaining",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (tutorialStep == 6) viewModel.nextStep()
                        showAddDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Новая задача", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubject == null,
                            onClick = { selectedSubject = null },
                            label = { Text("Все") },
                            leadingIcon = if (selectedSubject == null) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(subjects) { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = { selectedSubject = subject },
                            label = { Text(subject) }
                        )
                    }
                }

                DateStrip(
                    filter = filter,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onShowAll = { viewModel.showAll() }
                )
                ProgressCard(progress = progress, total = filteredItems.size)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        item { EmptyTasksState() }
                    }
                    items(filteredItems, key = { item ->
                        when (item) {
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
private fun ProgressCard(progress: Float, total: Int) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Прогресс дня",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        when {
                            total == 0 -> "Пусто. Создай задачу 👇"
                            progress >= 1f -> "Всё закрыто. Красавчик."
                            progress >= 0.5f -> "Больше половины — финишная прямая!"
                            else -> "Ещё чуть-чуть и втянешься"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun DateStrip(
    filter: TaskDateFilter,
    onDateSelected: (Long) -> Unit,
    onShowAll: () -> Unit
) {
    val todayMillis = System.currentTimeMillis()
    val baseCal = remember {
        Calendar.getInstance().apply {
            timeInMillis = todayMillis
            add(Calendar.DAY_OF_YEAR, -3)
        }
    }
    val selectedMillis = (filter as? TaskDateFilter.Date)?.millis

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val isSelected = filter is TaskDateFilter.All
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300),
                label = "bg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300),
                label = "fg"
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(88.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(bg)
                    .clickable { onShowAll() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AllInclusive, null, tint = fg, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Все", color = fg, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        items(18) { i ->
            val date = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val millis = date.timeInMillis
            val isSelected = selectedMillis != null && isSameDay(millis, selectedMillis)
            val isToday = isSameDay(millis, todayMillis)
            val isPast = millis < todayMillis && !isToday

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.06f else 1f,
                animationSpec = tween(250),
                label = "scale"
            )
            val bg by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(300),
                label = "bg"
            )
            val fg by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(300),
                label = "fg"
            )

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(88.dp)
                    .scale(scale)
                    .clip(MaterialTheme.shapes.large)
                    .background(bg)
                    .clickable { onDateSelected(millis) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = SimpleDateFormat("E", Locale("ru")).format(date.time).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isToday) fg else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            KofiMascot(size = 68.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Здесь пусто",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Жми «+» и закидывай задачи. Я напомню за 30 минут до дедлайна.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun NoteAsTaskCard(
    note: NoteEntity,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (note.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onToggle() },
                tint = if (note.isCompleted) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = note.subjectTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = note.noteText,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = "ИЗ УРОКА",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

private fun isSameDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
private data class Confetti(
    val xFrac: Float,
    val startYFrac: Float,
    val swayFrac: Float,
    val fallFrac: Float,
    val gravity: Float,
    val sizePx: Float,
    val widthRatio: Float,
    val rotStart: Float,
    val rotSpeed: Float,
    val color: Color,
    val circle: Boolean
)

private val confettiPalette = listOf(
    Color(0xFFFFC107), Color(0xFFFF5252), Color(0xFF40C4FF),
    Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFFFFFFFF)
)

@Composable
fun CelebrationOverlay(onDismiss: () -> Unit) {
    val particles = remember {
        List(110) {
            Confetti(
                xFrac = Random.nextFloat(),
                startYFrac = Random.nextFloat() * -0.4f - 0.05f,
                swayFrac = (Random.nextFloat() - 0.5f) * 0.18f,
                fallFrac = 0.7f + Random.nextFloat() * 0.5f,
                gravity = 0.55f + Random.nextFloat() * 0.5f,
                sizePx = 14f + Random.nextFloat() * 18f,
                widthRatio = 0.4f + Random.nextFloat() * 0.5f,
                rotStart = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 1400f,
                color = confettiPalette[Random.nextInt(confettiPalette.size)],
                circle = Random.nextFloat() < 0.25f
            )
        }
    }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fall.animateTo(1f, tween(4200, easing = LinearEasing)) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }
    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val glow by idle.animateFloat(
        initialValue = 0.92f, targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(600, delayMillis = 280)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = fall.value
            particles.forEach { p ->
                val y = (p.startYFrac + p.fallFrac * t + p.gravity * t * t) * size.height
                if (y < -40f || y > size.height + 40f) return@forEach
                val x = (p.xFrac + p.swayFrac * sin(t * PI * 3 + p.rotStart).toFloat()) * size.width
                val a = if (t > 0.8f) ((1f - t) / 0.2f).coerceIn(0f, 1f) else 1f
                rotate(degrees = p.rotStart + p.rotSpeed * t, pivot = Offset(x, y)) {
                    if (p.circle) {
                        drawCircle(color = p.color.copy(alpha = a), radius = p.sizePx / 2f, center = Offset(x, y))
                    } else {
                        val w = p.sizePx * p.widthRatio
                        drawRect(
                            color = p.color.copy(alpha = a),
                            topLeft = Offset(x - w / 2f, y - p.sizePx / 2f),
                            size = Size(w, p.sizePx)
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(208.dp)
                        .scale(glow * pop.value)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            translationY = bob
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    KofiMascot(
                        size = 110.dp,
                        bodyColor = Color.White,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 48f
                }
            ) {
                Text(
                    "ВСЁ ГОТОВО!",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Закрыл все задачи. Время законного отдыха.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Text("КРУТО, ПОНЯЛ!", fontWeight = FontWeight.ExtraBold)
                }
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
    val swipeThreshold = 280f
    val swipeProgress = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    val rotationAngle = offsetX.value / 28f

    val priorityColor = when (task.weight) {
        5 -> PriorityBlood
        4 -> PriorityHigh
        3 -> PriorityMid
        2 -> PriorityNormal
        else -> PriorityChill
    }

    val bgColor = when {
        offsetX.value > 0 -> MaterialTheme.colorScheme.secondary.copy(alpha = swipeProgress)
        offsetX.value < 0 -> MaterialTheme.colorScheme.error.copy(alpha = swipeProgress)
        else -> Color.Transparent
    }

    val now = System.currentTimeMillis()
    val isOverdue = task.deadlineTimestamp != null && task.deadlineTimestamp < now && !task.isCompleted

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor, MaterialTheme.shapes.large)
                .padding(horizontal = 24.dp),
            contentAlignment = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            if (abs(offsetX.value) > 20f) {
                Icon(
                    imageVector = if (offsetX.value > 0) Icons.Default.CheckCircle else Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .scale(0.5f + (swipeProgress * 0.5f))
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface,
            tonalElevation = if (task.isCompleted) 0.dp else 2.dp,
            border = when {
                isOverdue -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                task.weight >= 4 && !task.isCompleted -> BorderStroke(1.dp, priorityColor.copy(alpha = 0.6f))
                else -> null
            },
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer { rotationZ = rotationAngle }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > swipeThreshold -> {
                                        offsetX.animateTo(1200f, tween(300))
                                        onToggle()
                                        offsetX.snapTo(0f)
                                    }
                                    offsetX.value < -swipeThreshold -> {
                                        offsetX.animateTo(-1200f, tween(300))
                                        onDelete()
                                        offsetX.snapTo(0f)
                                    }
                                    else -> offsetX.animateTo(0f, tween(220))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(priorityColor)
                    )
                }

                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp, horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                        task.deadlineTimestamp?.let { ts ->
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error else priorityColor
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error else priorityColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!task.subjectName.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = task.subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOverdue) {
                            Pill(text = "ПРОСРОЧЕНО", color = MaterialTheme.colorScheme.error,
                                onColor = MaterialTheme.colorScheme.onError)
                        }
                        if (task.weight >= 5 && !task.isCompleted) {
                            Pill(text = "ВАЖНО", color = MaterialTheme.colorScheme.tertiary,
                                onColor = MaterialTheme.colorScheme.onTertiary)
                        }
                        if (task.isFromNote) {
                            Pill(text = "ИЗ УРОКА", color = MaterialTheme.colorScheme.secondary,
                                onColor = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color, onColor: Color) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = onColor,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
