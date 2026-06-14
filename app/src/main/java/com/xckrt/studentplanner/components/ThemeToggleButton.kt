package com.xckrt.studentplanner.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunColor = Color(0xFFFFB300)
private val MoonColor = Color(0xFFD7DEEA)

@Composable
fun ThemeToggleButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "themeMorph"
    )
    val container = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(container)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.26f

            rotate(degrees = 65f * p, pivot = c) {
                val rayAlpha = (1f - p * 1.8f).coerceIn(0f, 1f)
                if (rayAlpha > 0.001f) {
                    val inner = r * 1.5f
                    val outer = inner + r * 0.85f * (1f - p)
                    for (i in 0 until 8) {
                        val ang = (i * PI / 4).toFloat()
                        val dx = cos(ang)
                        val dy = sin(ang)
                        drawLine(
                            color = SunColor.copy(alpha = rayAlpha),
                            start = Offset(c.x + dx * inner, c.y + dy * inner),
                            end = Offset(c.x + dx * outer, c.y + dy * outer),
                            strokeWidth = r * 0.34f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                drawCircle(color = lerp(SunColor, MoonColor, p), radius = r, center = c)

                if (p > 0.01f) {
                    val d = r * (2.1f - 1.48f * p)
                    val ang = (-PI / 4).toFloat()
                    val maskCenter = Offset(c.x + cos(ang) * d, c.y + sin(ang) * d)
                    drawCircle(color = container, radius = r * 0.98f, center = maskCenter)
                }
            }
        }
    }
}
