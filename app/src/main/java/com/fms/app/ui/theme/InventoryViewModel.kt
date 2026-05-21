package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

/**
 * InventoryViewModel: Manages the stock ledger and real-time inventory levels.
 * Strictly isolated by Tenant ID.
 */
class InventoryViewModel : ViewModel() {
    
    // UI State: List of ledger entries
    val stockItems = mutableStateListOf<Map<String, Any>>()
    
    // UI State: Current stock levels by item
    val currentStocks = mutableStateListOf<Pair<String, Double>>()

    var isLoading by mutableStateOf(false)
        private set

    init {
        refreshInventory()
    }

    /**
     * Pulls the latest ledger and stock snapshots for the current tenant.
     */
    fun refreshInventory() {
        isLoading = true
        
        // 1. Fetch Ledger Entries
        FirebaseRepository.getModuleData<Map<String, Any>>("Inventory/StockLedger") { ledger ->
            stockItems.clear()
            stockItems.addAll(ledger.reversed()) // Show latest first
            
            // 2. Fetch Current Stock Levels
            FirebaseRepository.getModuleDataWithKeys("Inventory/Stocks") { stocks ->
                val snapshot = stocks.map { (id, data) ->
                    id to ((data["currentStock"] as? Number)?.toDouble() ?: 0.0)
                }
                currentStocks.clear()
                currentStocks.addAll(snapshot)
                isLoading = false
            }
        }
    }
}