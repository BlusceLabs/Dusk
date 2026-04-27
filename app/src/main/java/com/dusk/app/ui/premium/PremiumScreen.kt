package com.dusk.app.ui.premium

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusk.app.data.repository.PremiumTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Premium header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                ))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFEAB308),
                                modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Go Premium", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Unlock all exclusive features", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    if (uiState.currentSubscription != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("You're a Premium member!", fontWeight = FontWeight.Bold)
                                    Text("Enjoy all exclusive features", fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.cancelSubscription() }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                items(uiState.tiers, key = { it.id }) { tier ->
                    PremiumTierCard(
                        tier = tier,
                        onSelect = { viewModel.subscribe(tier.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PremiumTierCard(tier: PremiumTier, onSelect: () -> Unit) {
    val borderColor = if (tier.popular) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (tier.popular) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (tier.popular) 8.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (tier.popular) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("POPULAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text(tier.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(tier.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$${String.format("%.2f", tier.price)}", fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Text("/month", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            tier.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Subscribe - $${String.format("%.2f", tier.price)}/mo")
            }
        }
    }
}
