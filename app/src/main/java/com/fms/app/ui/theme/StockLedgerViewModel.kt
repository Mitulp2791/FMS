package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class StockLedgerViewModel : ViewModel() {
    val ledgerEntries = mutableStateListOf<Map<String, Any?>>()

    fun loadLedgerForItem(itemId: String) {
        FirebaseRepository.getModuleData("Inventory") { data ->
            ledgerEntries.clear()
            // Filter entries for this specific item and sort by latest first
            val filtered = data.filter { it.second["itemId"] == itemId }
                .map { it.second }
                .sortedByDescending { it["timestamp"] as? Long ?: 0L }
            ledgerEntries.addAll(filtered)
        }
    }
}
