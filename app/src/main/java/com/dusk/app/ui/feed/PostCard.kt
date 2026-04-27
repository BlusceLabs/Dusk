package com.dusk.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dusk.app.domain.model.Post
import com.dusk.app.ui.reels.formatCount

@Composable
fun PostCard(
    post: Post,
    onUserClick: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header: avatar + name + time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.author?.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author?.displayName ?: post.author?.username ?: "Unknown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.author?.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = null,
                                tint = Color(0xFF1DA1F2), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = post.author?.let { "@${it.username}" } ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = timeAgo(post.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Media
            if (post.images.isNotEmpty()) {
                AsyncImage(
                    model = post.images.first(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            // Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PostAction(
                    icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = formatCount(post.likes),
                    onClick = onLike,
                    tint = if (post.isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                PostAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = formatCount(post.comments),
                    onClick = onComment,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PostAction(
                    icon = Icons.Outlined.Repeat,
                    label = formatCount(post.reposts),
                    onClick = onShare,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PostAction(
                    icon = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    label = "",
                    onClick = onBookmark,
                    tint = if (post.isBookmarked) Color(0xFF1DA1F2) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PostAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 12.sp, color = tint)
        }
    }
}

fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 7 -> "${days / 7}w"
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "now"
    }
}
