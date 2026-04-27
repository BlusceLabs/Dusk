package com.dusk.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dusk.app.domain.model.Notification
import com.dusk.app.domain.model.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("Mark all read")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(uiState.notifications, key = { it.id }) { notif ->
                    NotificationItem(
                        notification = notif,
                        onClick = { viewModel.markAsRead(notif.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val (icon, tint) = notificationIcon(notification.type)
    val bg = if (!notification.read) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = notification.fromUser?.avatar,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append("<b>${notification.fromUser?.displayName ?: "Someone"}</b> ")
                    append(notification.content)
                },
                fontSize = 14.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeAgo(notification.createdAt),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!notification.read) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
    Divider()
}

fun notificationIcon(type: NotificationType): Pair<ImageVector, Color> = when (type) {
    NotificationType.LIKE -> Icons.Filled.Favorite to Color(0xFFEF4444)
    NotificationType.COMMENT -> Icons.Filled.ChatBubble to MaterialTheme.colorScheme.primary
    NotificationType.FOLLOW -> Icons.Filled.PersonAdd to Color(0xFF22C55E)
    NotificationType.REPOST -> Icons.Filled.Replay to Color(0xFF3B82F6)
    NotificationType.MENTION -> Icons.Filled.AlternateEmail to Color(0xFFA855F7)
    NotificationType.STREAM_START -> Icons.Filled.Videocam to Color(0xFFEF4444)
    NotificationType.PREMIUM_GIFT -> Icons.Filled.Star to Color(0xFFEAB308)
}

fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> "${diff / 604800_000}w ago"
    }
}
