package com.dusk.app.ui.stories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dusk.app.domain.model.Story

@Composable
fun StoriesViewerScreen(
    uiState: StoriesUiState,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val groupKeys = uiState.groupedStories.keys.toList()
    val currentGroup = groupKeys.getOrNull(uiState.currentGroupIndex) ?: return
    val groupStories = uiState.groupedStories[currentGroup] ?: return
    val currentStory = groupStories.getOrNull(uiState.currentStoryIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onHorizontalDrag = { _, _ -> }
                )
            }
    ) {
        // Story media
        AsyncImage(
            model = currentStory.mediaUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Progress bars at top
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupStories.forEachIndexed { index, story ->
                    val progress = if (index < uiState.currentStoryIndex) 1f
                    else if (index == uiState.currentStoryIndex) uiState.viewerProgress
                    else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
            // Header with user info and close
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentStory.user?.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentStory.user?.displayName ?: "User",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        // Tap areas for prev/next
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clickable { onPrevious() })
            Box(modifier = Modifier.weight(1f).fillMaxSize().clickable { onNext() })
        }
    }
}
