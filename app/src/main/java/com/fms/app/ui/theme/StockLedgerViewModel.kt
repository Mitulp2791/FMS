package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class StockLedgerViewModel : ViewModel() {
    val ledgerEntries = mutableStateListOf<Map<String, Any>>()

    fun loadLedgerForItem(itemId: String) {
        FirebaseRepository.getModuleData<Map<String, Any>>("Inventory/StockLedger") { data ->
            ledgerEntries.clear()
            // Filter entries for this specific item and sort by latest first
            val filtered = data.filter { it["itemId"] == itemId }
                .sortedByDescending { it["timestamp"] as? Long ?: 0L }
            ledgerEntries.addAll(filtered)
        }
    }
}
