package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class BillingViewModel : ViewModel() {
    val items = mutableStateListOf<Map<String, String>>()
    val cart = mutableStateListOf<Map<String, Any>>()

    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadInventory()
    }

    private fun loadInventory() {
        isLoading = true
        FirebaseRepository.getAllItems { fetchedItems ->
            items.clear()
            items.addAll(fetchedItems)
            isLoading = false
        }
    }

    fun addToCart(item: Map<String, String>, qty: Double) {
        val price = item["costPerUnit"]?.toDoubleOrNull() ?: 0.0
        val cartItem = mapOf(
            "itemId" to (item["id"] ?: ""),
            "name" to (item["name"] ?: "Unknown"),
            "qty" to qty,
            "price" to price,
            "lineTotal" to (qty * price)
        )
        cart.add(cartItem)
    }

    fun processSale(
        customerName: String,
        customerMobile: String,
        isGst: Boolean,
        gstValue: Double,
        gstType: String
    ) {
        if (cart.isEmpty()) {
            statusMessage = "Cart is empty."
            return
        }

        isLoading = true
        val baseAmount = cart.sumOf { it["lineTotal"]?.toString()?.toDoubleOrNull() ?: 0.0 }
        val grandTotal = baseAmount + gstValue

        FirebaseRepository.createInvoice(
            customerId = "WALKIN",
            customerName = customerName,
            customerMobile = customerMobile,
            items = cart.toList(),
            baseAmount = baseAmount,
            isGstApplicable = isGst,
            gstType = gstType,
            totalGst = gstValue,
            grandTotal = grandTotal,
            saleType = "RETAIL"
        )

        isLoading = false
        statusMessage = "Invoice Generated Successfully."
        cart.clear()
    }

    fun clearStatus() {
        statusMessage = null
    }
}