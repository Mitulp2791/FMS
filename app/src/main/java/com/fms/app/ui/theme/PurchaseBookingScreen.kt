package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fms.app.data.FirebaseRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseBookingScreen() {
    val context = LocalContext.current

    // Master Data
    val masterItems = remember { mutableStateListOf<Pair<String, Map<String, Any?>>>() }
    val vendorItems = remember { mutableStateListOf<Pair<String, Map<String, Any?>>>() }

    // Selections
    var selectedItemId by remember { mutableStateOf("") }
    var selectedItemName by remember { mutableStateOf("Select Raw Material / Item") }
    var expandedItems by remember { mutableStateOf(false) }

    var selectedVendorId by remember { mutableStateOf("") }
    var selectedVendorName by remember { mutableStateOf("Select Supplier/Vendor") }
    var expandedVendors by remember { mutableStateOf(false) }

    // Financial Fields
    var qty by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var serviceCostPerUnit by remember { mutableStateOf("") }
    var referenceNo by remember { mutableStateOf("") }

    // GST Engine
    var isGstApplicable by remember { mutableStateOf(false) }
    var gstType by remember { mutableStateOf("CGST_SGST") } // CGST_SGST or IGST
    var gstPercent by remember { mutableStateOf("18") }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        FirebaseRepository.getModuleData("Masters") { data -> masterItems.clear(); masterItems.addAll(data) }
        FirebaseRepository.getModuleData("Accounts") { data -> 
            vendorItems.clear()
            // Filter only "Supplier" type accounts for GRN
            val suppliers = data.filter { it.second["accountType"] == "Supplier" }
            vendorItems.addAll(suppliers) 
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Goods Receipt Note (GRN)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // --- 1. VENDOR DETAILS (From Master) ---
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Supplier & Invoice Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ExposedDropdownMenuBox(expanded = expandedVendors, onExpandedChange = { expandedVendors = !expandedVendors }) {
                    OutlinedTextField(
                        value = selectedVendorName, onValueChange = {}, readOnly = true,
                        label = { Text("Select Vendor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVendors) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedVendors, onDismissRequest = { expandedVendors = false }) {
                        vendorItems.forEach { vendor ->
                            val name = vendor.second["name"]?.toString() ?: "Unknown"
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedVendorId = vendor.first; selectedVendorName = name; expandedVendors = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = referenceNo, onValueChange = { referenceNo = it },
                        label = { Text("Invoice / Ref No.") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = SimpleDateFormat("dd/MM", Locale.US).format(Date()), onValueChange = {},
                        label = { Text("Date") }, enabled = false, modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }

        // --- 2. ITEM SELECTION & PRICING ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("2. Product & Costing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ExposedDropdownMenuBox(expanded = expandedItems, onExpandedChange = { expandedItems = !expandedItems }) {
                    OutlinedTextField(
                        value = selectedItemName, onValueChange = {}, readOnly = true,
                        label = { Text("Select Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItems) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedItems, onDismissRequest = { expandedItems = false }) {
                        masterItems.forEach { item ->
                            val name = item.second["name"]?.toString() ?: "Unknown"
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedItemId = item.first; selectedItemName = name; expandedItems = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = pricePerUnit, onValueChange = { pricePerUnit = it }, label = { Text("Base Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = serviceCostPerUnit, onValueChange = { serviceCostPerUnit = it }, label = { Text("Freight") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        }


        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("3. Tax Details (GST)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Switch(checked = isGstApplicable, onCheckedChange = { isGstApplicable = it })
                }

                if (isGstApplicable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = gstPercent, onValueChange = { gstPercent = it },
                            label = { Text("Tax (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.4f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gstType == "CGST_SGST", onClick = { gstType = "CGST_SGST" })
                                Text("Local (CGST/SGST)", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gstType == "IGST", onClick = { gstType = "IGST" })
                                Text("Inter-State (IGST)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. FINANCIAL MATH ---
        val q = qty.toDoubleOrNull() ?: 0.0
        val p = pricePerUnit.toDoubleOrNull() ?: 0.0
        val s = serviceCostPerUnit.toDoubleOrNull() ?: 0.0
        val g = if (isGstApplicable) (gstPercent.toDoubleOrNull() ?: 0.0) else 0.0

        val baseTotal = (p + s) * q
        val totalGstAmount = baseTotal * (g / 100)
        val finalLandedTotal = baseTotal + totalGstAmount

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("GRN Summary Preview", style = MaterialTheme.typography.labelLarge)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Base Amount:"); Text("₹${String.format(Locale.US, "%.2f", baseTotal)}") }

                if (isGstApplicable) {
                    if (gstType == "CGST_SGST") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("CGST (${g/2}%):"); Text("₹${String.format(Locale.US, "%.2f", totalGstAmount/2)}") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SGST (${g/2}%):"); Text("₹${String.format(Locale.US, "%.2f", totalGstAmount/2)}") }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("IGST ($g%):"); Text("₹${String.format(Locale.US, "%.2f", totalGstAmount)}") }
                    }
                }
                HorizontalDivider()
                Text("Total Invoice Value: ₹${String.format(Locale.US, "%.2f", finalLandedTotal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = selectedItemId.isNotEmpty() && selectedVendorId.isNotEmpty() && qty.isNotEmpty() && pricePerUnit.isNotEmpty(),
            onClick = {
                FirebaseRepository.bookPurchase(selectedItemId, q, p, s, selectedVendorId, selectedVendorName, referenceNo, isGstApplicable, gstType, g)
                Toast.makeText(context, "GRN Saved & Sequence Generated", Toast.LENGTH_SHORT).show()

                selectedItemId = ""; selectedItemName = "Select Raw Material / Item"
                selectedVendorId = ""; selectedVendorName = "Select Supplier/Vendor"
                qty = ""; pricePerUnit = ""; serviceCostPerUnit = ""; referenceNo = ""
            }
        ) {
            Text("Generate GRN & Update Stock")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
