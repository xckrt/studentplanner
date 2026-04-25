package com.xckrt.studentplanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSegmentedButton(
    isPrivate: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 2


        val indicatorOffset by animateDpAsState(
            targetValue = if (isPrivate) tabWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "indicatorOffset"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )


        Row(modifier = Modifier.fillMaxSize()) {

            TabItem(
                text = "🌐 Общее",
                isSelected = !isPrivate,
                modifier = Modifier.weight(1f),
                onClick = { onSelectionChange(false) }
            )


            TabItem(
                text = "🔒 Личное",
                isSelected = isPrivate,
                modifier = Modifier.weight(1f),
                onClick = { onSelectionChange(true) }
            )
        }
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}