package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMasterScreen(viewModel: ItemMasterViewModel = viewModel()) {
    val context = LocalContext.current
    
    // SECURITY ENFORCEMENT
    val canWrite = remember { UserSession.canWrite("Masters") }
    val isAdminOrManager = remember { UserSession.role == "ADMIN" || UserSession.role == "MANAGER" }

    var expandedCat by remember { mutableStateOf(false) }
    var expandedUom by remember { mutableStateOf(false) }
    
    // State for showing Stock Ledger Audit Trail
    var selectedItemForAudit by remember { mutableStateOf<Pair<String, Map<String, Any?>>?>(null) }

    val categories = listOf("Raw Material", "Finished Good", "Packaging Material")
    val uoms = listOf("Kg", "Gm", "Ltr", "Ml", "Pcs")

    // Auto Prefix Logic
    val skuPrefix = when(viewModel.category.value) {
        "Finished Good" -> "FG"
        "Raw Material" -> "RM"
        else -> "PM"
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        
        // 1. DATA ENTRY SECTION (Role Protected)
        if (canWrite) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Create Enterprise Item", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = viewModel.name.value,
                        onValueChange = { viewModel.name.value = it },
                        label = { Text("Product/Material Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = !expandedCat },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = viewModel.category.value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            viewModel.category.value = cat
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = viewModel.rawSkuInput.value,
                            onValueChange = { viewModel.rawSkuInput.value = it.uppercase() },
                            label = { Text("SKU Code") },
                            prefix = { Text(skuPrefix, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedUom,
                            onExpandedChange = { expandedUom = !expandedUom },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = viewModel.uom.value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("UOM") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUom) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(expanded = expandedUom, onDismissRequest = { expandedUom = false }) {
                                uoms.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit) },
                                        onClick = {
                                            viewModel.uom.value = unit
                                            expandedUom = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = viewModel.hsnCode.value,
                            onValueChange = { viewModel.hsnCode.value = it },
                            label = { Text("HSN Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // FINANCIAL PRIVACY: Only Admin/Manager can set/see cost
                        if (isAdminOrManager) {
                            OutlinedTextField(
                                value = viewModel.openingCost.value,
                                onValueChange = { viewModel.openingCost.value = it },
                                label = { Text("Initial Cost (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (viewModel.category.value == "Finished Good") {
                            OutlinedTextField(
                                value = viewModel.sellingPrice.value,
                                onValueChange = { viewModel.sellingPrice.value = it },
                                label = { Text("Selling Price (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = viewModel.name.value.isNotEmpty() && viewModel.rawSkuInput.value.isNotEmpty(),
                        onClick = {
                            viewModel.saveItem {
                                Toast.makeText(context, "Item Saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Save Item to Master") }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. ITEM DIRECTORY SECTION
        Text("Item Directory (Tap for History)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(viewModel.itemsList) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedItemForAudit = item }
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(item.second["name"].toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("SKU: ${item.second["sku"]} | HSN: ${item.second["hsn"]}", style = MaterialTheme.typography.bodyMedium)
                            if(item.second["type"] == "FG") {
                                Text("Selling Price: ₹${item.second["sellingPrice"]}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Badge(containerColor = if (item.second["type"] == "FG") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) {
                            Text(item.second["category"].toString())
                        }
                    }
                }
            }
        }
    }

    // 3. AUDIT TRAIL DIALOG
    selectedItemForAudit?.let { item ->
        StockLedgerDialog(
            itemId = item.first,
            itemName = item.second["name"]?.toString() ?: "Unknown",
            onDismiss = { selectedItemForAudit = null }
        )
    }
}
