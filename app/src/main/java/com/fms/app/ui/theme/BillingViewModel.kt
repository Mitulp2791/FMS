package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import java.text.SimpleDateFormat
import java.util.*

class CartItem(
    itemId: String = "",
    name: String = "",
    initialQty: String = "",
    initialPrice: String = ""
) {
    var itemId by mutableStateOf(itemId)
    var name by mutableStateOf(name)
    var qty by mutableStateOf(initialQty)
    var price by mutableStateOf(initialPrice)
    
    val qtyDouble: Double get() = qty.toDoubleOrNull() ?: 0.0
    val priceDouble: Double get() = price.toDoubleOrNull() ?: 0.0
    val lineTotal: Double get() = qtyDouble * priceDouble
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "itemId" to itemId,
            "name" to name,
            "qty" to qtyDouble,
            "price" to priceDouble,
            "lineTotal" to lineTotal
        )
    }
}

class BillingViewModel : ViewModel() {
    val masterItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    val customerItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    val exhibitionItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    val cart = mutableStateListOf<CartItem>()
    
    // Live stock tracker to prevent overselling
    val stockLevels = mutableStateMapOf<String, Double>()

    var selectedCustomerId = mutableStateOf("")
    var selectedCustomerName = mutableStateOf("")
    var saleTypeDisplay = mutableStateOf("")
    var customerMobile = mutableStateOf("")
    
    var isGstApplicable = mutableStateOf(false)
    var gstType = mutableStateOf("CGST_SGST")
    var gstPercent = mutableStateOf("18")

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        FirebaseRepository.getModuleData("Masters") { data ->
            masterItems.clear()
            masterItems.addAll(data)
        }
        FirebaseRepository.getModuleData("Accounts") { data ->
            customerItems.clear()
            // Filter only "Customer" type accounts for billing
            val customers = data.filter { it.second["accountType"] == "Customer" }
            customerItems.addAll(customers)
        }
        FirebaseRepository.getModuleData("Exhibitions") { data ->
            exhibitionItems.clear()
            exhibitionItems.addAll(data)
            
            // Default to active exhibition if any
            val active = getActiveExhibitions()
            if (active.isNotEmpty() && selectedCustomerId.value.isEmpty()) {
                selectedCustomerId.value = "EXHIBITION"
                saleTypeDisplay.value = active.first()
                selectedCustomerName.value = "" // Clear Billing Name to allow user to type
            }
        }
        // Fetch inventory to calculate available stock
        FirebaseRepository.getModuleData("Inventory") { ledger ->
            stockLevels.clear()
            ledger.forEach { entry ->
                val itemId = entry.second["itemId"]?.toString() ?: ""
                val change = (entry.second["change"] as? Number)?.toDouble() ?: 0.0
                if (itemId.isNotEmpty()) {
                    stockLevels[itemId] = (stockLevels[itemId] ?: 0.0) + change
                }
            }
        }
    }

    fun getActiveExhibitions(): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val today = sdf.parse(todayStr) ?: return emptyList()

        return exhibitionItems.mapNotNull { ex ->
            val data = ex.second
            val fromDateStr = data["fromDate"]?.toString() ?: ""
            val toDateStr = data["toDate"]?.toString() ?: ""
            val name = data["exhibitorName"]?.toString() ?: ""
            val location = data["location"]?.toString() ?: ""

            try {
                val fromDate = sdf.parse(fromDateStr)
                val toDate = sdf.parse(toDateStr)

                if (fromDate != null && toDate != null && !today.before(fromDate) && !today.after(toDate)) {
                    // Format: Exhibition Name-Location-11 to 12 May 2026
                    val fromCal = Calendar.getInstance().apply { time = fromDate }
                    val toCal = Calendar.getInstance().apply { time = toDate }
                    
                    val dayMonthFormat = SimpleDateFormat("d MMMM yyyy", Locale.US)
                    val dayFormat = SimpleDateFormat("d", Locale.US)
                    
                    val dateRange = if (fromCal.get(Calendar.MONTH) == toCal.get(Calendar.MONTH) && 
                        fromCal.get(Calendar.YEAR) == toCal.get(Calendar.YEAR)) {
                        "${dayFormat.format(fromDate)} to ${dayMonthFormat.format(toDate)}"
                    } else {
                        "${dayMonthFormat.format(fromDate)} to ${dayMonthFormat.format(toDate)}"
                    }
                    
                    "$name-$location-$dateRange"
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getAvailableStock(itemId: String): Double {
        if (itemId.isEmpty()) return 0.0
        val currentStock = stockLevels[itemId] ?: 0.0
        val inCart = cart.filter { it.itemId == itemId }.sumOf { it.qtyDouble }
        return currentStock - inCart
    }

    fun addBlankLine() {
        cart.add(CartItem())
    }

    fun removeFromCart(index: Int) {
        if (index >= 0 && index < cart.size) {
            cart.removeAt(index)
        }
    }

    fun generateInvoice(onSuccess: () -> Unit) {
        // Filter out incomplete lines
        val validItems = cart.filter { it.itemId.isNotEmpty() && it.qtyDouble > 0 }
        if (validItems.isEmpty()) return

        val baseTotal = validItems.sumOf { it.lineTotal }
        val g = if (isGstApplicable.value) (gstPercent.value.toDoubleOrNull() ?: 0.0) else 0.0
        val totalGst = baseTotal * (g / 100)
        val grandTotal = baseTotal + totalGst

        FirebaseRepository.createInvoice(
            selectedCustomerId.value,
            selectedCustomerName.value,
            customerMobile.value,
            validItems.map { it.toMap() },
            baseTotal,
            isGstApplicable.value,
            gstType.value,
            totalGst,
            grandTotal,
            saleTypeDisplay.value
        )
        
        cart.clear()
        selectedCustomerId.value = ""
        selectedCustomerName.value = ""
        saleTypeDisplay.value = ""
        customerMobile.value = ""
        
        // Re-check for exhibition default after clear
        val active = getActiveExhibitions()
        if (active.isNotEmpty()) {
            selectedCustomerId.value = "EXHIBITION"
            saleTypeDisplay.value = active.first()
            selectedCustomerName.value = ""
        }

        onSuccess()
    }
}
