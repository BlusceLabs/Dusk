package com.dusk.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Account", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                SettingsRow(title = "Edit Profile", onClick = { })
                SettingsRow(title = "Change Password", onClick = { })
                SettingsRow(title = "Privacy", onClick = { })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Preferences", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                SettingsRow(title = "Notifications", onClick = { })
                SettingsRow(title = "Dark Mode", onClick = { })
                SettingsRow(title = "Language", onClick = { })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Other", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                SettingsRow(title = "About", onClick = { })
                SettingsRow(title = "Log Out", onClick = { viewModel.logout() })
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
