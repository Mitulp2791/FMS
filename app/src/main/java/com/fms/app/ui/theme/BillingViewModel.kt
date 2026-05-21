package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

/**
 * BillingViewModel: Handles sales transactions with atomic inventory deductions.
 * Optimized for Multi-Tenant SaaS with strict data integrity.
 */
class BillingViewModel : ViewModel() {
    
    // UI State: Available items for sale
    val availableItems = mutableStateListOf<Map<String, Any>>()
    
    // UI State: Cart
    val cartItems = mutableStateListOf<Map<String, Any>>()
    var customerName = mutableStateOf("")
    var totalAmount = mutableStateOf(0.0)
    var isProcessing by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)

    init {
        loadAvailableItems()
    }

    /**
     * Fetches items from the tenant's master catalog.
     */
    fun loadAvailableItems() {
        FirebaseRepository.getAllItems { items ->
            availableItems.clear()
            availableItems.addAll(items)
        }
    }

    /**
     * Adds an item to the current billing session.
     */
    fun addToCart(item: Map<String, Any>, qty: Double) {
        val cartItem = item.toMutableMap()
        cartItem["qty"] = qty
        cartItems.add(cartItem)
        calculateTotal()
    }

    /**
     * Removes an item from the cart.
     */
    fun removeFromCart(index: Int) {
        if (index in cartItems.indices) {
            cartItems.removeAt(index)
            calculateTotal()
        }
    }

    private fun calculateTotal() {
        totalAmount.value = cartItems.sumOf { 
            val price = (it["price"] as? Number)?.toDouble() ?: (it["costPerUnit"] as? Number)?.toDouble() ?: 0.0
            val qty = (it["qty"] as? Number)?.toDouble() ?: 0.0
            price * qty 
        }
    }

    fun clearStatus() {
        statusMessage = null
    }

    /**
     * Finalizes the sale using a Firebase Transaction.
     * This ensures the Invoice is created and Stock is deducted atomically.
     */
    fun finalizeInvoice(onComplete: (Boolean) -> Unit) {
        if (cartItems.isEmpty()) {
            statusMessage = "Cart is empty"
            onComplete(false)
            return
        }
        
        isProcessing = true
        
        val invoiceData = mapOf(
            "customerName" to customerName.value,
            "totalAmount" to totalAmount.value,
            "itemCount" to cartItems.size,
            "status" to "COMPLETED"
        )

        // Convert cart items to a simplified list for the repository
        val itemsToDeduct = cartItems.map { 
            mapOf(
                "itemId" to (it["id"] ?: it["name"] ?: ""),
                "qty" to (it["qty"] ?: 0.0)
            )
        }

        FirebaseRepository.createInvoice(invoiceData, itemsToDeduct) { success ->
            isProcessing = false
            if (success) {
                cartItems.clear()
                customerName.value = ""
                totalAmount.value = 0.0
                statusMessage = "Sale completed successfully!"
            } else {
                statusMessage = "Error processing sale. Please check connectivity."
            }
            onComplete(success)
        }
    }
}