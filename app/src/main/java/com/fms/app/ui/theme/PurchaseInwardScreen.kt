package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseInwardScreen(viewModel: PurchaseInwardViewModel = viewModel()) {
    val context = LocalContext.current
    var supplierId by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var itemId by remember { mutableStateOf("") }
    var referenceNo by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var baseTotal by remember { mutableStateOf("") }
    var gstEnabled by remember { mutableStateOf(false) }
    var gstPercent by remember { mutableStateOf("") }
    var totalGst by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Purchase Inward (GRN)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                OutlinedTextField(value = supplierName, onValueChange = { supplierName = it }, label = { Text("Supplier Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = itemId, onValueChange = { itemId = it }, label = { Text("Item ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = referenceNo, onValueChange = { referenceNo = it }, label = { Text("Reference Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = baseTotal, onValueChange = { baseTotal = it }, label = { Text("Base Total Amount") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("GST Applicable")
                    Switch(checked = gstEnabled, onCheckedChange = { gstEnabled = it })
                }

                if (gstEnabled) {
                    OutlinedTextField(value = gstPercent, onValueChange = { gstPercent = it }, label = { Text("GST %") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = totalGst, onValueChange = { totalGst = it }, label = { Text("Total GST Amount") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.processInward(
                    supplierId, supplierName, referenceNo, itemId,
                    qty.toDoubleOrNull() ?: 0.0, baseTotal.toDoubleOrNull() ?: 0.0,
                    gstEnabled, gstPercent.toDoubleOrNull() ?: 0.0, "IGST", totalGst.toDoubleOrNull() ?: 0.0
                ) { success ->
                    if (success) {
                        Toast.makeText(context, "Purchase Recorded", Toast.LENGTH_SHORT).show()
                        // Reset fields
                        supplierName = ""; itemId = ""; referenceNo = ""; qty = ""; baseTotal = ""; totalGst = ""
                    } else {
                        Toast.makeText(context, "Error saving purchase", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isLoading) "Processing..." else "Post Inward Transaction")
        }
    }
}
