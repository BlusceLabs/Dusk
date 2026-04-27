package com.dusk.app.ui.communities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dusk.app.data.repository.Community

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    viewModel: CommunitiesViewModel = hiltViewModel(),
    onCreateClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.Create, contentDescription = "Create Community")
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
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(uiState.communities, key = { it.id }) { community ->
                    CommunityCard(
                        community = community,
                        onToggleJoin = { viewModel.toggleJoin(community) }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityCard(community: Community, onToggleJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = community.avatar,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(community.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${community.memberCount} members", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (community.description.isNotBlank()) {
                    Text(community.description, fontSize = 13.sp, maxLines = 2)
                }
            }
            if (community.isJoined) {
                OutlinedButton(
                    onClick = onToggleJoin,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("Joined", fontSize = 12.sp) }
            } else {
                Button(
                    onClick = onToggleJoin,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) { Text("Join", fontSize = 12.sp) }
            }
        }
    }
}
