package com.xckrt.studentplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xckrt.studentplanner.data.NoteHistoryDTO
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    historyList: List<NoteHistoryDTO>,
    currentUserId:Int?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "История изменений",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (historyList.isEmpty()) {
                Text(
                    text = "Изменений пока не было.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList) { historyItem ->
                        HistoryItemCard(historyItem, currentUserId!!)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: NoteHistoryDTO, currentUserId: Int) {
    val rawTimestamp = item.changeTimestamp ?: ""
    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("dd MMM в HH:mm", Locale("ru"))
        val date = parser.parse(rawTimestamp)
        date?.let { formatter.format(it) } ?: rawTimestamp
    } catch (e: Exception) {
        rawTimestamp
    }


    val displayName = listOfNotNull(item.firstName, item.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifEmpty { "Анонимный студент" }

    val BASE_URL = "http://5.42.114.104:5262"
    val isMe = item.authorId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!item.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = "$BASE_URL${item.avatarUrl}",
                                contentDescription = "Аватар автора",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(Вы)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate.ifBlank { "Неизвестное время" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Surface(
                    color = if (item.isPrivate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (item.isPrivate) "🔒 Личное" else "🌐 Общее",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (item.isPrivate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }


                if (item.hasTask) {
                    Surface(
                        color = if (item.isTaskCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (item.isTaskCompleted) "✅ Выполнено" else "📋 В задачах",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (item.isTaskCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }


            val beforeText = item.contentBefore ?: ""
            val afterText = item.contentAfter ?: ""

            if (beforeText.isNotBlank() && beforeText != afterText) {
                Text(
                    text = beforeText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = afterText.ifBlank { "🗑 Заметка удалена" },
                fontSize = 14.sp,
                color = if (afterText.isBlank()) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}