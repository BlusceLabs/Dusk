package com.dusk.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dusk.app.domain.model.Post
import com.dusk.app.ui.reels.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId, currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.user?.username ?: "Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isCurrentUser) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Profile header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar & stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uiState.user?.avatar,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            ProfileStat("Posts", formatCount(uiState.user?.posts ?: 0))
                            ProfileStat("Followers", formatCount(uiState.user?.followers?.size ?: 0))
                            ProfileStat("Following", formatCount(uiState.user?.following?.size ?: 0))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name & bio
                        Text(
                            text = uiState.user?.displayName ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.user?.let { user ->
                            if (user.bio.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = user.bio, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons
                        if (uiState.isCurrentUser) {
                            OutlinedButton(
                                onClick = { /* TODO Edit Profile */ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit Profile")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = if (uiState.isFollowing) ButtonDefaults.outlinedButtonColors()
                                    else ButtonDefaults.buttonColors()
                                ) {
                                    Text(if (uiState.isFollowing) "Following" else "Follow")
                                }
                                OutlinedButton(
                                    onClick = { /* TODO Message */ },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Message")
                                }
                            }
                        }
                    }
                }

                // Tab Row
                item {
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(selected = uiState.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) }) {
                            Icon(Icons.Filled.GridOn, contentDescription = "Posts",
                                modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) }) {
                            Icon(Icons.Filled.BookmarkBorder, contentDescription = "Saved",
                                modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = uiState.selectedTab == 2,
                            onClick = { viewModel.selectTab(2) }) {
                            Icon(Icons.Filled.Tag, contentDescription = "Tagged",
                                modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                // Content grid
                items(uiState.posts.chunked(3)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        row.forEach { post ->
                            ProfilePostItem(
                                post = post,
                                onClick = { onPostClick(post.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfilePostItem(post: Post, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (post.images.isNotEmpty()) {
            AsyncImage(
                model = post.images.first(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(post.content.take(20), fontSize = 10.sp,
                    textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
