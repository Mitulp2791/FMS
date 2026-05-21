package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession

/**
 * BillingScreen: A multi-tenant POS (Point of Sale) interface.
 * Uses atomic transactions for data integrity across inventory and sales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: BillingViewModel = viewModel()) {
    val context = LocalContext.current
    var showItemPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing Console - ${UserSession.companyName}") },
                actions = {
                    IconButton(onClick = { showItemPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Customer Info
            OutlinedTextField(
                value = viewModel.customerName.value,
                onValueChange = { viewModel.customerName.value = it },
                label = { Text("Customer Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Cart Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            // Cart List
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(viewModel.cartItems) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item["name"]?.toString() ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text(text = "Qty: ${item["qty"]} | Rate: ${item["price"] ?: item["costPerUnit"]}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.removeFromCart(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // Summary Section
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Amount:", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("₹${viewModel.totalAmount.value}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkout Button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    viewModel.finalizeInvoice { success ->
                        if (success) {
                            Toast.makeText(context, "Invoice Generated & Inventory Updated", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Transaction Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = viewModel.cartItems.isNotEmpty() && !viewModel.isProcessing,
                shape = MaterialTheme.shapes.medium
            ) {
                if (viewModel.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Transaction (Atomic)")
                }
            }
        }
    }

    // Modal to pick items from the Master Catalog
    if (showItemPicker) {
        AlertDialog(
            onDismissRequest = { showItemPicker = false },
            title = { Text("Select Product") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(viewModel.availableItems) { item ->
                        ListItem(
                            headlineContent = { Text(item["name"]?.toString() ?: "Unnamed") },
                            supportingContent = { Text("Price: ${item["costPerUnit"]}") },
                            modifier = Modifier.clickable {
                                viewModel.addToCart(item, 1.0)
                                showItemPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showItemPicker = false }) { Text("Close") }
            }
        )
    }
}
