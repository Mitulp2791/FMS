package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StockLedgerDialog(itemId: String, itemName: String, onDismiss: () -> Unit, viewModel: StockLedgerViewModel = viewModel()) {
    LaunchedEffect(itemId) {
        viewModel.loadLedgerForItem(itemId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock History: $itemName") },
        text = {
            Box(modifier = Modifier.height(400.dp)) {
                if (viewModel.ledgerEntries.isEmpty()) {
                    Text("No transactions found for this item.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.ledgerEntries) { entry ->
                            val timestamp = entry["timestamp"] as? Long ?: 0L
                            val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
                            val change = (entry["change"] as? Number)?.toDouble() ?: 0.0
                            val type = entry["type"]?.toString() ?: "Unknown"
                            val userId = entry["userId"]?.toString() ?: "System"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (change > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = if (change > 0) "+$change" else "$change", fontWeight = FontWeight.Bold)
                                        Text(text = type, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(text = "By: $userId", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
