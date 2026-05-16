package com.fms.app.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class PurchaseInwardViewModel : ViewModel() {
    val masterItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    val vendorItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()

    var selectedVendorId = mutableStateOf("")
    var selectedVendorName = mutableStateOf("Select Supplier")
    var isSupplierSearchOpen = mutableStateOf(false)
    var supplierSearchQuery = mutableStateOf("")
    
    var selectedItemId = mutableStateOf("")
    var selectedItemName = mutableStateOf("Select Item")
    
    var qty = mutableStateOf("")
    var unitPrice = mutableStateOf("")
    var serviceCost = mutableStateOf("0")
    var referenceNo = mutableStateOf("")
    
    // Batch & Expiry for ERP Quality Control
    var batchNo = mutableStateOf("")
    var expiryDate = mutableStateOf("")

    var isGstApplicable = mutableStateOf(false)
    var gstType = mutableStateOf("CGST_SGST")
    var gstPercent = mutableStateOf("18")

    val filteredVendors = derivedStateOf {
        if (supplierSearchQuery.value.isEmpty()) {
            vendorItems
        } else {
            vendorItems.filter {
                val name = it.second["name"]?.toString() ?: ""
                val mobile = it.second["mobile"]?.toString() ?: ""
                val city = it.second["city"]?.toString() ?: ""
                name.contains(supplierSearchQuery.value, ignoreCase = true) ||
                        mobile.contains(supplierSearchQuery.value, ignoreCase = true) ||
                        city.contains(supplierSearchQuery.value, ignoreCase = true)
            }
        }
    }

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleData("Masters") { data ->
            masterItems.clear()
            masterItems.addAll(data)
        }
        FirebaseRepository.getModuleData("Accounts") { data ->
            vendorItems.clear()
            // Filter only "Supplier" type accounts for Inward
            val suppliers = data.filter { 
                val type = it.second["accountType"]?.toString() ?: ""
                type.contains("Supplier", ignoreCase = true) 
            }
            vendorItems.addAll(suppliers)
        }
    }

    fun onSupplierSelected(id: String, name: String) {
        selectedVendorId.value = id
        selectedVendorName.value = name
        isSupplierSearchOpen.value = false
        supplierSearchQuery.value = ""
    }

    fun submitInward(onSuccess: () -> Unit) {
        val q = qty.value.toDoubleOrNull() ?: 0.0
        val p = unitPrice.value.toDoubleOrNull() ?: 0.0
        val s = serviceCost.value.toDoubleOrNull() ?: 0.0
        val g = gstPercent.value.toDoubleOrNull() ?: 0.0

        FirebaseRepository.bookPurchase(
            itemId = selectedItemId.value,
            qty = q,
            unitPrice = p,
            serviceCost = s,
            supplierId = selectedVendorId.value,
            supplierName = selectedVendorName.value,
            referenceNo = referenceNo.value,
            isGstApplicable = isGstApplicable.value,
            gstType = gstType.value,
            gstPercent = g,
            batchNo = batchNo.value,
            expiryDate = expiryDate.value
        )

        // Reset fields
        qty.value = ""
        unitPrice.value = ""
        referenceNo.value = ""
        batchNo.value = ""
        expiryDate.value = ""
        onSuccess()
    }
}
