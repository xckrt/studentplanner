package com.xckrt.studentplanner.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xckrt.studentplanner.components.KofiMascot
import com.xckrt.studentplanner.ui.theme.KofiBubbleShape

private data class KofiStep(val title: String, val body: String, val emoji: String = "")

private fun stepContent(step: Int): KofiStep = when (step) {
    1 -> KofiStep(
        "Здарова! Я Кофи",
        "Твой бро в мире дедлайнов. Покажу за минуту, как не утонуть в учёбе. Тапни по экрану ↓",
        "👋"
    )
    2 -> KofiStep(
        "Это РАСПИСАНИЕ",
        "Здесь всегда актуальные пары твоей группы. Замены подсвечены, отмены — зачёркнуты.",
        "📅"
    )
    3 -> KofiStep(
        "Заметки к парам",
        "Нажми на любую пару, чтобы добавить заметку — её увидит вся группа.",
        "📝"
    )
    4 -> KofiStep(
        "Сохрани заметку",
        "Напиши пару слов о домашке и тапни «СОХРАНИТЬ». Я подожду.",
        "📝"
    )
    5 -> KofiStep(
        "Окей, с парами разобрались",
        "Жми «Задачи» в меню снизу — там центр управления дедлайнами.",
        "✅"
    )
    6 -> KofiStep(
        "Создай первую задачу",
        "Тапни на круглую кнопку с плюсом справа внизу.",
        "➕"
    )
    7 -> KofiStep(
        "Дедлайн",
        "Напиши, что сделать, и обязательно выбери дату — иначе я не смогу напомнить.",
        "⏰"
    )
    8 -> KofiStep(
        "Приоритет",
        "Чем важнее задача — тем выше вес. Они отсортируются автоматически.",
        "🔥"
    )
    9 -> KofiStep("", "")
    10 -> KofiStep(
        "Магия свайпов",
        "Свайпни задачу ВПРАВО — она закроется как выполненная.",
        "👉"
    )
    11 -> KofiStep(
        "Удаление",
        "А свайп ВЛЕВО удалит задачу. Создай ещё одну и попробуй.",
        "👈"
    )
    12 -> KofiStep(
        "Ну ты машина!",
        "Теперь ты официально мастер планирования. Не подведи группу!",
        "😎"
    )
    else -> KofiStep("", "")
}

@Composable
fun GlobalKofiTutorial(step: Int, onNext: () -> Unit, onFinish: () -> Unit) {
    val isInteractive = step in 3..11
    val content = remember(step) { stepContent(step) }
    if (content.title.isBlank() && content.body.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "kofi-float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
        label = "y"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isInteractive) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (step == 12) onFinish() else onNext()
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .offset(y = if (isInteractive) (-150).dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .offset(y = offsetY.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    KofiMascot(size = 80.dp)
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = KofiBubbleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier.widthIn(max = 340.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = content.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (content.emoji.isNotBlank()) {
                                Text(text = content.emoji, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = content.body,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        if (!isInteractive) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (step < 12) {
                                    TextButton(
                                        onClick = onFinish,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            "Пропустить",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(0.dp))
                                }
                                Button(
                                    onClick = { if (step == 12) onFinish() else onNext() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = if (step == 12) "Готово" else "Дальше",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
