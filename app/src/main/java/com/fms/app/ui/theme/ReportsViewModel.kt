package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import kotlin.math.abs

class ReportsViewModel : ViewModel() {
    val invoices = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    val stockLedger = mutableStateListOf<Pair<String, Map<String, Any?>>>()

    val totalRevenue = derivedStateOf {
        invoices.sumOf {
            // Note: Updated to 'grandTotal' to match professional billing structure
            (it.second["grandTotal"] as? Number)?.toDouble() ?: 0.0
        }
    }

    val totalCogs = derivedStateOf {
        stockLedger
            .filter { it.second["type"]?.toString()?.startsWith("SALE") == true }
            .sumOf { abs((it.second["unitCost"] as? Number)?.toDouble() ?: 0.0 * ((it.second["change"] as? Number)?.toDouble() ?: 0.0)) }
            // Note: COGS should be absolute value of (qty_change * unit_cost) for sales
    }

    val grossProfit = derivedStateOf {
        totalRevenue.value - totalCogs.value
    }

    val profitMargin = derivedStateOf {
        if (totalRevenue.value > 0) (grossProfit.value / totalRevenue.value) * 100 else 0.0
    }

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleData("Billing") { data ->
            invoices.clear()
            invoices.addAll(data)
        }
        FirebaseRepository.getModuleData("Inventory") { data ->
            stockLedger.clear()
            stockLedger.addAll(data)
        }
    }
}
