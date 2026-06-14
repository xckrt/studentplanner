package com.xckrt.studentplanner.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xckrt.studentplanner.components.KofiMascot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    val flyX = remember { Animatable(220f) }
    val flyY = remember { Animatable(-280f) }
    val flyScale = remember { Animatable(0.2f) }
    val flyRotZ = remember { Animatable(-35f) }
    val flyRotY = remember { Animatable(60f) }
    val flyRotX = remember { Animatable(-20f) }

    val halo1Scale = remember { Animatable(0f) }
    val halo2Scale = remember { Animatable(0f) }
    val halo3Scale = remember { Animatable(0f) }
    val halo1Alpha = remember { Animatable(0f) }
    val halo2Alpha = remember { Animatable(0f) }
    val halo3Alpha = remember { Animatable(0f) }

    val burstProgress = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleLetterSpacing = remember { Animatable(20f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffset = remember { Animatable(20f) }

    val haptic = LocalHapticFeedback.current
    val idle = rememberInfiniteTransition(label = "idle")
    val idleY by idle.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idleY"
    )
    val idleWobbleY by idle.animateFloat(
        initialValue = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idleWobbleY"
    )
    val orbitAngle by idle.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "orbit"
    )

    LaunchedEffect(Unit) {
        launch { flyX.animateTo(0f, tween(900, easing = EaseOutExpo)) }
        launch { flyY.animateTo(0f, tween(900, easing = EaseOutExpo)) }
        launch { flyScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        launch { flyRotZ.animateTo(0f, tween(800, easing = EaseOutBack)) }
        launch { flyRotY.animateTo(0f, tween(900, easing = EaseOutCubic)) }
        launch { flyRotX.animateTo(0f, tween(900, easing = EaseOutCubic)) }
        launch {
            delay(200)
            launch { halo1Alpha.animateTo(1f, tween(120)) }
            launch { halo1Scale.animateTo(3.0f, tween(900, easing = FastOutSlowInEasing)) }
            launch { halo1Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
        }
        launch {
            delay(400)
            launch { halo2Alpha.animateTo(1f, tween(120)) }
            launch { halo2Scale.animateTo(3.4f, tween(900, easing = FastOutSlowInEasing)) }
            launch { halo2Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
        }
        launch {
            delay(600)
            launch { halo3Alpha.animateTo(1f, tween(120)) }
            launch { halo3Scale.animateTo(3.8f, tween(900, easing = FastOutSlowInEasing)) }
            launch { halo3Alpha.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
        }
        delay(800)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        burstAlpha.snapTo(1f)
        launch { burstProgress.animateTo(1f, tween(900, easing = EaseOutExpo)) }
        launch {
            delay(500)
            burstAlpha.animateTo(0f, tween(500))
        }
        launch {
            delay(950)
            titleAlpha.animateTo(1f, tween(500))
        }
        launch {
            delay(950)
            titleLetterSpacing.animateTo(6f, tween(800, easing = EaseOutExpo))
        }
        launch {
            delay(1300)
            subtitleAlpha.animateTo(1f, tween(500))
        }
        launch {
            delay(1300)
            subtitleOffset.animateTo(0f, tween(500, easing = EaseOutBack))
        }

        delay(2400)
        onAnimationFinished()
    }

    val bgBrush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.background
        ),
        radius = 1600f
    )

    val density = LocalDensity.current
    val camDistPx = with(density) { 12.dp.toPx() } * 8f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        OrbitingSparks(angle = orbitAngle)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                PulseHalo(scale = halo1Scale.value, alpha = halo1Alpha.value)
                PulseHalo(scale = halo2Scale.value, alpha = halo2Alpha.value)
                PulseHalo(scale = halo3Scale.value, alpha = halo3Alpha.value)
                ConfettiBurst(progress = burstProgress.value, alpha = burstAlpha.value)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            translationX = with(density) { flyX.value.dp.toPx() }
                            translationY = with(density) { (flyY.value + idleY).dp.toPx() }
                            scaleX = flyScale.value
                            scaleY = flyScale.value
                            rotationZ = flyRotZ.value
                            rotationY = flyRotY.value + idleWobbleY
                            rotationX = flyRotX.value
                            cameraDistance = camDistPx
                        }
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
                    KofiMascot(size = 118.dp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "STUDENT PLANNER",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = titleLetterSpacing.value.sp,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "со мной не забудешь",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .offset(y = subtitleOffset.value.dp)
            )
        }
    }
}

@Composable
private fun PulseHalo(scale: Float, alpha: Float) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.35f))
    )
}
@Composable
private fun OrbitingSparks(angle: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadiusPx = size.minDimension * 0.22f

        repeat(8) { i ->
            val phase = (angle + i * 45f) * (PI / 180f)
            val r = baseRadiusPx + (i % 3) * 18f
            val x = cx + (r * cos(phase)).toFloat()
            val y = cy + (r * sin(phase)).toFloat() * 0.6f  // эллиптическая орбита (псевдо-3D)
            val color = when (i % 3) {
                0 -> primary
                1 -> tertiary
                else -> secondary
            }
            // Размер пульсирует по фазе — задние искры мельче.
            val depth = (sin(phase) + 1.0).toFloat() / 2f
            val radius = 4f + depth * 6f
            drawCircle(
                color = color.copy(alpha = 0.4f + depth * 0.5f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
@Composable
private fun ConfettiBurst(progress: Float, alpha: Float) {
    if (alpha <= 0f) return
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .size(320.dp)
            .alpha(alpha)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.55f
        repeat(12) { i ->
            val angleRad = i * (PI * 2 / 12)
            val r = maxR * progress
            val x = cx + (r * cos(angleRad)).toFloat()
            val y = cy + (r * sin(angleRad)).toFloat()
            val color = when (i % 3) {
                0 -> primary
                1 -> tertiary
                else -> secondary
            }
            drawCircle(
                color = color,
                radius = 6f * (1f - progress * 0.5f),
                center = Offset(x, y)
            )
        }
    }
}
