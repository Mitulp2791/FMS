package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseInwardScreen(viewModel: PurchaseInwardViewModel = viewModel()) {
    val context = LocalContext.current

    var expandedItems by remember { mutableStateOf(false) }

    if (viewModel.isSupplierSearchOpen.value) {
        SupplierSelectionDialog(
            onDismiss = { viewModel.isSupplierSearchOpen.value = false },
            onSupplierSelected = { id, name ->
                viewModel.onSupplierSelected(id, name)
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Material Inward (GRN)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. VENDOR SELECTION
                Box(modifier = Modifier.fillMaxWidth().clickable { viewModel.isSupplierSearchOpen.value = true }) {
                    OutlinedTextField(
                        value = viewModel.selectedVendorName.value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier / Vendor") },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Supplier")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }

                OutlinedTextField(
                    value = viewModel.referenceNo.value,
                    onValueChange = { viewModel.referenceNo.value = it },
                    label = { Text("Supplier Invoice / Ref No.") },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // 2. ITEM SELECTION
                ExposedDropdownMenuBox(expanded = expandedItems, onExpandedChange = { expandedItems = !expandedItems }) {
                    OutlinedTextField(
                        value = viewModel.selectedItemName.value, onValueChange = {}, readOnly = true,
                        label = { Text("Select Material/Item") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItems) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedItems, onDismissRequest = { expandedItems = false }) {
                        viewModel.masterItems.forEach { item ->
                            val name = item.second["name"]?.toString() ?: "Unknown"
                            DropdownMenuItem(text = { Text(name) }, onClick = { 
                                viewModel.selectedItemId.value = item.first
                                viewModel.selectedItemName.value = name
                                expandedItems = false 
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.qty.value, onValueChange = { viewModel.qty.value = it },
                        label = { Text("Qty Inward") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.unitPrice.value, onValueChange = { viewModel.unitPrice.value = it },
                        label = { Text("Unit Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Batch & Expiry Tracking
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.batchNo.value, onValueChange = { viewModel.batchNo.value = it },
                        label = { Text("Batch / Lot No.") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.expiryDate.value, onValueChange = { viewModel.expiryDate.value = it },
                        label = { Text("Expiry (YYYY-MM)") }, modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = viewModel.serviceCost.value, onValueChange = { viewModel.serviceCost.value = it },
                    label = { Text("Other Charges (Freight/Service)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. TAXATION
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Input GST Applicable")
                            Switch(checked = viewModel.isGstApplicable.value, onCheckedChange = { viewModel.isGstApplicable.value = it })
                        }
                        if (viewModel.isGstApplicable.value) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = viewModel.gstPercent.value, onValueChange = { viewModel.gstPercent.value = it }, label = { Text("Tax %") }, modifier = Modifier.weight(1f))
                                Column(modifier = Modifier.weight(2f)) {
                                    Row { RadioButton(selected = viewModel.gstType.value == "CGST_SGST", onClick = { viewModel.gstType.value = "CGST_SGST" }); Text("Local") }
                                    Row { RadioButton(selected = viewModel.gstType.value == "IGST", onClick = { viewModel.gstType.value = "IGST" }); Text("Inter-state") }
                                }
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = viewModel.selectedItemId.value.isNotEmpty() && viewModel.selectedVendorId.value.isNotEmpty() && viewModel.qty.value.isNotEmpty(),
                    onClick = {
                        viewModel.submitInward {
                            Toast.makeText(context, "Purchase Recorded & Stock Updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Confirm Material Inward") }
            }
        }
    }
}

@Composable
fun SupplierSelectionDialog(
    onDismiss: () -> Unit,
    onSupplierSelected: (String, String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Supplier Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Embed the full Account Master Screen with selection logic
                AccountMasterScreen(
                    onAccountSelected = { id, name ->
                        onSupplierSelected(id, name)
                    }
                )
            }
        }
    }
}
