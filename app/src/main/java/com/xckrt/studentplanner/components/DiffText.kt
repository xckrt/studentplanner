package com.xckrt.studentplanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DiffText(
    originalText: String?,
    newText: String?,
    isOriginalMode: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    originalColor: Color = Color.Gray,
    newAccentColor: Color = MaterialTheme.colorScheme.primary,
    isCancelled: Boolean = false
) {
    val orig = originalText?.trim()
    val new = newText?.trim()


    if (isOriginalMode) {
        Text(
            text = orig ?: new ?: "-",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }


    if (isCancelled) {
        Text(
            text = orig ?: new ?: "Отменено",
            style = textStyle.copy(textDecoration = TextDecoration.LineThrough),
            color = Color.Red,
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }


    if (!orig.isNullOrBlank() && orig != new && !new.isNullOrBlank()) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = orig,
                style = textStyle.copy(textDecoration = TextDecoration.LineThrough),
                color = originalColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "изменилось на",
                modifier = Modifier.size(textStyle.fontSize.value.dp * 0.9f),
                tint = originalColor
            )

            Text(
                text = new,
                style = textStyle.copy(fontWeight = FontWeight.ExtraBold),
                color = newAccentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }

    else {
        Text(
            text = new ?: orig ?: "-",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}