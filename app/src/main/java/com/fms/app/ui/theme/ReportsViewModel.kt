package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import kotlin.math.abs

/**
 * ReportsViewModel: Aggregates financial and stock metrics for a specific tenant.
 * Uses derived states to provide real-time updates based on ledger data.
 */
class ReportsViewModel : ViewModel() {
    
    // Raw Data Streams
    val invoices = mutableStateListOf<Pair<String, Map<String, Any>>>()
    val stockLedger = mutableStateListOf<Map<String, Any>>()

    /**
     * Total Revenue: Sum of all completed invoice grand totals.
     */
    val totalRevenue = derivedStateOf {
        invoices.sumOf { (_, data) ->
            (data["totalAmount"] as? Number)?.toDouble() ?: 0.0
        }
    }

    /**
     * Total COGS (Cost of Goods Sold): Calculates the cost of items sold via stock ledger changes.
     */
    val totalCogs = derivedStateOf {
        stockLedger
            .filter { it["type"]?.toString() == "SALE" }
            .sumOf { 
                val change = abs((it["change"] as? Number)?.toDouble() ?: 0.0)
                val unitCost = (it["unitCost"] as? Number)?.toDouble() ?: 0.0
                change * unitCost
            }
    }

    /**
     * Gross Profit: Revenue - COGS
     */
    val grossProfit = derivedStateOf {
        totalRevenue.value - totalCogs.value
    }

    /**
     * Profit Margin Percentage
     */
    val profitMargin = derivedStateOf {
        if (totalRevenue.value > 0) (grossProfit.value / totalRevenue.value) * 100 else 0.0
    }

    init {
        loadReportData()
    }

    /**
     * Fetches tenant-scoped data from the repository for analysis.
     */
    fun loadReportData() {
        // Fetch Invoices
        FirebaseRepository.getModuleDataWithKeys("Billing/Invoices") { data ->
            invoices.clear()
            invoices.addAll(data)
        }
        
        // Fetch Stock Ledger for COGS calculation
        FirebaseRepository.getModuleData<Map<String, Any>>("Inventory/StockLedger") { ledger ->
            stockLedger.clear()
            stockLedger.addAll(ledger)
        }
    }
}