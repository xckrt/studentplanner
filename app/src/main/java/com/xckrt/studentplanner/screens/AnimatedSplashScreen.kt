package com.xckrt.studentplanner.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    val logoScale = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(1f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(50f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(key1 = true) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            pulseScale.animateTo(
                targetValue = 3f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            pulseAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = EaseOutBack)
            )
        }
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        delay(800)
        onAnimationFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale.value)
                        .alpha(pulseAlpha.value)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = "Логотип",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale.value)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "STUDENT PLANNER",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            )
        }
    }
}