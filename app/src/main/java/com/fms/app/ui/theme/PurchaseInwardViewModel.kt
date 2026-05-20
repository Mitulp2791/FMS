package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class PurchaseInwardViewModel : ViewModel() {
    val items = mutableStateListOf<Map<String, String>>()
    val suppliers = mutableStateListOf<Map<String, String>>()

    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadData()
    }

    private fun loadData() {
        isLoading = true
        FirebaseRepository.getAllItems { fetchedItems ->
            items.clear()
            items.addAll(fetchedItems)

            FirebaseRepository.getAccountsByType("Supplier") { fetchedSuppliers ->
                suppliers.clear()
                suppliers.addAll(fetchedSuppliers)
                isLoading = false
            }
        }
    }

    fun processInward(
        supplierId: String,
        supplierName: String,
        referenceNo: String,
        itemId: String,
        qty: Double,
        baseTotal: Double,
        isGst: Boolean,
        gstPercent: Double,
        gstType: String,
        totalGst: Double
    ) {
        if (itemId.isEmpty() || supplierId.isEmpty()) {
            statusMessage = "Please select both Supplier and Item."
            return
        }

        isLoading = true
        FirebaseRepository.createInwardTransaction(
            supplierId = supplierId,
            supplierName = supplierName,
            referenceNo = referenceNo,
            itemId = itemId,
            qty = qty,
            baseTotal = baseTotal,
            isGstApplicable = isGst,
            gstPercent = gstPercent,
            gstType = gstType,
            totalGst = totalGst
        )

        statusMessage = "Stock Inward Processed Successfully."
        isLoading = false
    }

    fun clearStatus() {
        statusMessage = null
    }
}