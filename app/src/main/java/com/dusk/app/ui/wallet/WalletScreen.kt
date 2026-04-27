package com.dusk.app.ui.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.dusk.app.data.repository.WalletTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Balance card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$${String.format("%.2f", uiState.balance.balance)}",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.depositPaypal(10.0) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Deposit")
                                }
                                Button(
                                    onClick = { /* Withdraw dialog */ },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Withdraw")
                                }
                            }
                        }
                    }
                }

                // Recent transactions header
                item {
                    Text(
                        "Recent Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TransactionItem(transaction = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: WalletTransaction) {
    val icon = when (transaction.type) {
        "deposit" -> Icons.Filled.ArrowDownward
        "withdrawal" -> Icons.Filled.ArrowUpward
        "tip" -> Icons.Filled.Favorite
        "subscription" -> Icons.Filled.Star
        else -> Icons.Filled.Receipt
    }
    val color = when (transaction.type) {
        "deposit" -> Color(0xFF22C55E)
        "withdrawal" -> MaterialTheme.colorScheme.error
        "tip" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description.ifBlank { transaction.type.replaceFirstChar { it.uppercase() } },
                fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(transaction.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (transaction.type == "deposit") "+$${String.format("%.2f", transaction.amount)}"
            else "-$${String.format("%.2f", transaction.amount)}",
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == "deposit") Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
            fontSize = 14.sp
        )
    }
    Divider(modifier = Modifier.padding(horizontal = 16.dp))
}
