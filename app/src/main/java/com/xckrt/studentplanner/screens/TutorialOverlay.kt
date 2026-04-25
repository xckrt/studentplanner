package com.xckrt.studentplanner.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlobalKofiTutorial(step: Int, onNext: () -> Unit, onFinish: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "kofi")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "y"
    )
    val isInteractive = step in 3..11

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isInteractive) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            if (step == 12) onFinish() else onNext()
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .offset(y = if (isInteractive) (-150).dp else 0.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp,
                    bottomStart = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .offset(y = offsetY.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Кофи",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = when (step) {
                                1 -> "Здарова! Я Кофи 🤖\nТвой бро в мире дедлайнов. Давай покажу, как тут не вылететь из колледжа. Тапни по мне!"
                                2 -> "Это твоё РАСПИСАНИЕ. Тут всегда актуальные пары твоей группы. Удобно, да?"
                                3 -> "🔥 ЗАДАНИЕ: Нажми на любую пару, чтобы добавить к ней заметку."
                                4 -> "🕒 А теперь нажми на иконку Истории в карточке пары. Там видно, кто из группы помогал с инфой!"
                                5 -> "С парами ясно. Теперь дуй в 'Задачи' в меню внизу. Жду тебя там!"
                                6 -> "Это твой центр управления. Нажми на Плюс, создадим твою первую задачу!"
                                7 -> "📅 ЗАДАНИЕ: Напиши название и обязательно выбери ДЕДЛАЙН в календаре диалога!"
                                8 -> "Теперь выбери ВЕС (приоритет). Чем важнее пара, тем выше ставь вес!"
                                9 -> ""
                                10 -> "А теперь магия: свайпни созданную задачу ВПРАВО, чтобы закрыть её!"
                                11 -> "Создай еще задачу и свайпни ее ВЛЕВО чтобы удалить её!"
                                12 -> "Ну ты машина! 😎 Теперь ты официально мастер планирования. Удачи, не подведи группу!"
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (step < 12) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onFinish,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Пропустить",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}