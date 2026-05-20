package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BillingScreen(viewModel: BillingViewModel = viewModel()) {
    var customerName by remember { mutableStateOf("") }
    var customerMobile by remember { mutableStateOf("") }
    var gstEnabled by remember { mutableStateOf(false) }
    var gstValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Point of Sale", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = customerMobile,
            onValueChange = { customerMobile = it },
            label = { Text("Customer Mobile") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Enable GST")
            Switch(checked = gstEnabled, onCheckedChange = { gstEnabled = it })
        }

        if (gstEnabled) {
            OutlinedTextField(
                value = gstValue,
                onValueChange = { gstValue = it },
                label = { Text("GST Amount") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Items in Cart: ${viewModel.cart.size}", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.items) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item["name"] ?: "Item")
                        Button(onClick = { viewModel.addToCart(item, 1.0) }) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.processSale(
                    customerName,
                    customerMobile,
                    gstEnabled,
                    gstValue.toDoubleOrNull() ?: 0.0,
                    "IGST" // Defaulting for simple retail
                )
            },
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isLoading) "Processing..." else "Complete Sale")
        }
    }

    // Status handling
    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            // Toast logic would go here
            viewModel.clearStatus()
        }
    }
}