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
fun ItemMasterScreen(viewModel: ItemMasterViewModel = viewModel()) {
    // UI State for form fields
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var uom by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var hsn by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Item Master", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Input Form
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uom, onValueChange = { uom = it }, label = { Text("UOM (PCS/KG)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost Price") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Selling Price") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = hsn, onValueChange = { hsn = it }, label = { Text("HSN Code") }, modifier = Modifier.fillMaxWidth())

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val data = mapOf(
                        "name" to name,
                        "category" to category,
                        "uom" to uom,
                        "costPerUnit" to (cost.toDoubleOrNull() ?: 0.0),
                        "sellingPrice" to (price.toDoubleOrNull() ?: 0.0),
                        "sku" to sku,
                        "hsn" to hsn
                    )
                    viewModel.saveItem(null, data) { success ->
                        if (success) {
                            // Clear form
                            name = ""; category = ""; uom = ""; cost = ""; price = ""; sku = ""; hsn = ""
                        }
                    }
                }
            ) {
                Text("Save Item")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List Display
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.items) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Accessing data via Map key (Fixing the Unresolved Reference errors)
                            Text(text = item["name"] ?: "Unknown Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Category: ${item["category"] ?: "N/A"}")
                            Text(text = "Price: ${item["costPerUnit"] ?: "0.0"}")
                        }
                    }
                }
            }
        }
    }
}