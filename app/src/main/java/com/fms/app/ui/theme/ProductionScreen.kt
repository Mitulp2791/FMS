package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(viewModel: ProductionViewModel = viewModel()) {
    val context = LocalContext.current

    var expandedFg by remember { mutableStateOf(false) }
    var expandedRm by remember { mutableStateOf(false) }
    
    val availableRmStock = if (viewModel.selectedRmId.value.isNotEmpty()) {
        viewModel.getAvailableStock(viewModel.selectedRmId.value)
    } else 0.0

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Manufacturing & BOM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. FINISHED GOOD SETUP
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Output Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ExposedDropdownMenuBox(expanded = expandedFg, onExpandedChange = { expandedFg = !expandedFg }) {
                    OutlinedTextField(
                        value = viewModel.selectedFgName.value, onValueChange = {}, readOnly = true, label = { Text("Target Product") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFg) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedFg, onDismissRequest = { expandedFg = false }) {
                        viewModel.finishedGoods.forEach { fg ->
                            val name = fg.second["name"]?.toString() ?: "Unknown"
                            DropdownMenuItem(text = { Text(name) }, onClick = { 
                                viewModel.selectedFgId.value = fg.first
                                viewModel.selectedFgName.value = name
                                expandedFg = false 
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = viewModel.batchNumber.value, onValueChange = { viewModel.batchNumber.value = it }, label = { Text("Lot/Batch No.") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = viewModel.yieldQty.value, onValueChange = { viewModel.yieldQty.value = it }, label = { Text("Expected Yield") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = viewModel.laborCost.value, onValueChange = { viewModel.laborCost.value = it }, label = { Text("Labor (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. ADD RAW MATERIALS TO BOM
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Bill of Materials (Inputs)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ExposedDropdownMenuBox(expanded = expandedRm, onExpandedChange = { expandedRm = !expandedRm }) {
                    OutlinedTextField(
                        value = viewModel.selectedRmName.value, onValueChange = {}, readOnly = true, label = { Text("Select Material") },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedRm, onDismissRequest = { expandedRm = false }) {
                        viewModel.rawMaterials.forEach { rm ->
                            val name = rm.second["name"]?.toString() ?: "Unknown"
                            val cost = rm.second["costPerUnit"]?.toString()?.toDoubleOrNull() ?: 0.0
                            DropdownMenuItem(text = { Text(name) }, onClick = { 
                                viewModel.selectedRmId.value = rm.first
                                viewModel.selectedRmName.value = name
                                viewModel.selectedRmCost.value = cost
                                expandedRm = false 
                            })
                        }
                    }
                }

                if (viewModel.selectedRmId.value.isNotEmpty()) {
                    Text(
                        "Available Stock: $availableRmStock",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (availableRmStock <= 0) Color.Red else Color.DarkGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = viewModel.rmQty.value, 
                        onValueChange = { viewModel.rmQty.value = it }, 
                        label = { Text("Qty") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        modifier = Modifier.weight(0.5f),
                        enabled = viewModel.selectedRmId.value.isNotEmpty() && (viewModel.rmQty.value.toDoubleOrNull() ?: 0.0) <= availableRmStock,
                        onClick = { 
                            val error = viewModel.addRmToBom()
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Add") }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. BOM DISPLAY & COSTING MATH
        val totalMaterialCost = viewModel.bomInputs.sumOf { (it["qty"] as Double) * (it["unitCost"] as Double) }
        val lCost = viewModel.laborCost.value.toDoubleOrNull() ?: 0.0
        val totalBatchCost = totalMaterialCost + lCost
        val yQty = viewModel.yieldQty.value.toDoubleOrNull() ?: 0.0
        val costPerUnit = if (yQty > 0) totalBatchCost / yQty else 0.0

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.bomInputs) { item ->
                val lineCost = (item["qty"] as Double) * (item["unitCost"] as Double)
                Card {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(item["name"].toString(), fontWeight = FontWeight.Bold)
                            Text("${item["qty"]} units @ ₹${String.format(Locale.US, "%.2f", item["unitCost"])} WAC", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("₹${String.format(Locale.US, "%.2f", lineCost)}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (viewModel.bomInputs.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("True Batch Costing", style = MaterialTheme.typography.labelLarge)
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Raw Material Cost:"); Text("₹${String.format(Locale.US, "%.2f", totalMaterialCost)}") }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Labor / Overhead:"); Text("₹${String.format(Locale.US, "%.2f", lCost)}") }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New FG WAC (Per Unit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                Text("₹${String.format(Locale.US, "%.2f", costPerUnit)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = viewModel.selectedFgId.value.isNotEmpty() && viewModel.bomInputs.isNotEmpty() && yQty > 0,
            onClick = {
                viewModel.completeProduction {
                    Toast.makeText(context, "Production Complete. Stock Updated.", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Process Manufacturing Batch")
        }
    }
}
