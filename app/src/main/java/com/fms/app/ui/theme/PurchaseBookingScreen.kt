package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fms.app.data.FirebaseRepository

@Composable
fun PurchaseBookingScreen() {
    val context = LocalContext.current

    // UI State for transaction fields
    var selectedItemId by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var selectedVendorId by remember { mutableStateOf("") }
    var referenceNo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // ... (Existing UI Fields) ...

        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick = {
                // Construct the transaction payload
                val transactionData = mapOf(
                    "itemId" to selectedItemId,
                    "vendorId" to selectedVendorId,
                    "qty" to (qty.toDoubleOrNull() ?: 0.0),
                    "price" to (pricePerUnit.toDoubleOrNull() ?: 0.0),
                    "referenceNo" to referenceNo,
                    "timestamp" to System.currentTimeMillis()
                )

                // The repository now handles Tenant Isolation automatically
                FirebaseRepository.saveItem("Transactions", null, transactionData) { success ->
                    if (success) {
                        Toast.makeText(context, "Transaction Stored in Tenant Cloud", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: Access Denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text("Submit Transaction")
        }
    }
}