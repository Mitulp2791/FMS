package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class InventoryViewModel : ViewModel() {
    private val stockLedger = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    private val itemsList = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    
    var searchQuery = mutableStateOf("")

    val inventoryData = derivedStateOf {
        itemsList.map { item ->
            val itemId = item.first
            val name = item.second["name"]?.toString() ?: "Unknown"
            val uom = item.second["uom"]?.toString() ?: ""
            val type = item.second["type"]?.toString() ?: "RM"
            val costPerUnit = (item.second["costPerUnit"] as? Number)?.toDouble() ?: 0.0

            val balance = stockLedger
                .filter { it.second["itemId"] == itemId }
                .sumOf { (it.second["change"] as? Number)?.toDouble() ?: 0.0 }

            val valuation = if (balance > 0) balance * costPerUnit else 0.0

            mapOf(
                "id" to itemId,
                "name" to name,
                "uom" to uom,
                "type" to type,
                "balance" to balance,
                "cost" to costPerUnit,
                "valuation" to valuation
            )
        }.sortedBy { it["name"].toString() }
    }

    val filteredInventory = derivedStateOf {
        inventoryData.value.filter {
            it["name"].toString().contains(searchQuery.value, ignoreCase = true) ||
                    it["id"].toString().contains(searchQuery.value, ignoreCase = true)
        }
    }

    val totalValuation = derivedStateOf {
        inventoryData.value.sumOf { it["valuation"] as Double }
    }

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleData("Masters") { itemsList.clear(); itemsList.addAll(it) }
        FirebaseRepository.getModuleData("Inventory") { stockLedger.clear(); stockLedger.addAll(it) }
    }

    fun exportLedger(context: android.content.Context) {
        FirebaseRepository.exportModuleToCSV(context, "Inventory", stockLedger.toList())
    }
}
