package com.dusk.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusk.app.ui.stories.StoriesViewerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onProfileClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showStories by remember { mutableStateOf(false) }
    var storyUserId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Dusk", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary)
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onChatClick) {
                        Icon(Icons.Filled.Send, contentDescription = "Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab Row: For You | Following | Live
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = uiState.selectedTab == 0, onClick = { viewModel.selectTab(0) }) {
                    Text("For You", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                }
                Tab(selected = uiState.selectedTab == 1, onClick = { viewModel.selectTab(1) }) {
                    Text("Following", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                }
                Tab(selected = uiState.selectedTab == 2, onClick = { viewModel.selectTab(2) }) {
                    Text("Live", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Story Bar at top
                    item {
                        StoryBar(
                            stories = uiState.stories,
                            myAvatar = uiState.currentUser?.avatar,
                            onMyStoryClick = {
                                uiState.currentUser?.let {
                                    storyUserId = it.uid
                                    showStories = true
                                }
                            },
                            onStoryClick = { story ->
                                storyUserId = story.userId
                                showStories = true
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Posts
                    items(uiState.posts) { post ->
                        PostCard(
                            post = post,
                            onUserClick = { onProfileClick(post.authorId) },
                            onLike = { viewModel.toggleLike(post) },
                            onComment = { onPostClick(post.id) },
                            onShare = { /* TODO share sheet */ },
                            onBookmark = { viewModel.toggleBookmark(post) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Stories overlay
        if (showStories) {
            StoriesViewerScreen(
                userId = storyUserId,
                onDismiss = { showStories = false }
            )
        }
    }
}
