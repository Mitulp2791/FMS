package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

/**
 * PurchaseInwardViewModel: Manages Goods Received Notes (GRN).
 * Ensures that stock increases and financial records are created atomically for the tenant.
 */
class PurchaseInwardViewModel : ViewModel() {
    
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Processes a purchase inward transaction.
     * Triggers the atomic stock update and transaction logging in the Repository.
     */
    fun processInward(
        supplierId: String,
        supplierName: String,
        referenceNo: String,
        itemId: String,
        qty: Double,
        baseTotal: Double,
        gstEnabled: Boolean,
        gstPercent: Double,
        gstType: String,
        totalGst: Double,
        onComplete: (Boolean) -> Unit
    ) {
        if (itemId.isBlank() || qty <= 0 || supplierName.isBlank()) {
            onComplete(false)
            return
        }

        isLoading = true
        
        val purchaseData = mapOf(
            "supplierId" to supplierId,
            "supplierName" to supplierName,
            "referenceNo" to referenceNo,
            "itemId" to itemId,
            "qty" to qty,
            "baseTotal" to baseTotal,
            "gstEnabled" to gstEnabled,
            "gstPercent" to gstPercent,
            "gstType" to gstType,
            "totalGst" to totalGst,
            "grandTotal" to (baseTotal + totalGst),
            "status" to "POSTED"
        )

        // FirebaseRepository.bookPurchase handles atomic stock increment and ledger logging
        FirebaseRepository.bookPurchase(purchaseData) { success ->
            isLoading = false
            onComplete(success)
        }
    }
}