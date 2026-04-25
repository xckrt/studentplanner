package com.xckrt.studentplanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ScheduleHeader(
    dayName: String,
    date: String?,
    hasChanges: Boolean,
    isShowingOriginal: Boolean,
    onToggleClick: () -> Unit
) {

    val formattedDate = try {
        if (!date.isNullOrBlank()) {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("d MMMM", Locale("ru"))
            parser.parse(date)?.let { formatter.format(it) } ?: ""
        } else ""
    } catch (e: Exception) {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )


            if (hasChanges && formattedDate.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "($formattedDate)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }


        if (hasChanges) {
            IconButton(onClick = onToggleClick) {
                Icon(
                    imageVector = if (isShowingOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Показать оригинал",
                    tint = if (isShowingOriginal) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}