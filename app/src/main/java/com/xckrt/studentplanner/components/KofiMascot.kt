package com.xckrt.studentplanner.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
@Composable
fun KofiMascot(
    size: Dp = 64.dp,
    bodyColor: Color = Color.White,
    accentColor: Color = Color(0xFF2B2B7C),
    cheekColor: Color = Color(0xFF22D3A6),
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "kofi")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, delayMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "breathe"
    )

    Canvas(modifier = modifier.size(size)) {
        val scale = if (animated) breathe else 1f
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val s = minOf(w, h) * scale

        val antennaTopY = (h / 2f) - s * 0.45f
        drawCircle(
            color = bodyColor,
            radius = s * 0.04f,
            center = Offset(cx, antennaTopY)
        )
        drawLine(
            color = bodyColor,
            start = Offset(cx, antennaTopY + s * 0.04f),
            end = Offset(cx, antennaTopY + s * 0.12f),
            strokeWidth = s * 0.04f,
            cap = StrokeCap.Round
        )
        val headSize = s * 0.7f
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(cx - headSize / 2f, h / 2f - headSize * 0.35f),
            size = Size(headSize, headSize * 0.78f),
            cornerRadius = CornerRadius(headSize * 0.34f, headSize * 0.34f)
        )

        val eyeY = h / 2f + headSize * 0.05f
        val eyeOffsetX = headSize * 0.18f
        val eyeBaseR = headSize * 0.10f
        val eyeHeight = eyeBaseR * 2f * blink
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(cx - eyeOffsetX - eyeBaseR, eyeY - eyeHeight / 2f),
            size = Size(eyeBaseR * 2f, eyeHeight),
            cornerRadius = CornerRadius(eyeBaseR, eyeBaseR)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(cx + eyeOffsetX - eyeBaseR, eyeY - eyeHeight / 2f),
            size = Size(eyeBaseR * 2f, eyeHeight),
            cornerRadius = CornerRadius(eyeBaseR, eyeBaseR)
        )

        if (blink > 0.4f) {
            val gleamR = eyeBaseR * 0.32f
            drawCircle(
                color = Color.White,
                radius = gleamR,
                center = Offset(cx - eyeOffsetX - eyeBaseR * 0.35f, eyeY - eyeBaseR * 0.3f)
            )
            drawCircle(
                color = Color.White,
                radius = gleamR,
                center = Offset(cx + eyeOffsetX - eyeBaseR * 0.35f, eyeY - eyeBaseR * 0.3f)
            )
        }

        val smileTop = eyeY + headSize * 0.18f
        val smilePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - headSize * 0.18f, smileTop)
            quadraticTo(cx, smileTop + headSize * 0.14f, cx + headSize * 0.18f, smileTop)
        }
        drawPath(
            path = smilePath,
            color = accentColor,
            style = Stroke(width = s * 0.04f, cap = StrokeCap.Round)
        )

        drawCircle(
            color = cheekColor,
            radius = headSize * 0.12f,
            center = Offset(cx + headSize * 0.42f, h / 2f - headSize * 0.15f)
        )
    }
}
