package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class DashboardViewModel : ViewModel() {
    var totalStockValue = mutableStateOf(0.0)
    var lowStockItems = mutableStateOf(0)
    var salesToday = mutableStateOf(0.0)
    var activeOrders = mutableStateOf(0)

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleData<Map<String, Any>>("Masters") { items ->
            FirebaseRepository.getModuleData<Map<String, Any>>("Inventory") { ledger ->
                var totalValue = 0.0
                var lowCount = 0
                items.forEach { item ->
                    val id = item["id"] as? String ?: ""
                    val cost = (item["costPerUnit"] as? Number)?.toDouble() ?: 0.0
                    val balance = ledger.filter { it["itemId"] == id }
                        .sumOf { (it["change"] as? Number)?.toDouble() ?: 0.0 }

                    if (balance > 0) totalValue += (balance * cost)
                    if (balance < 10) lowCount++
                }
                totalStockValue.value = totalValue
                lowStockItems.value = lowCount
            }
        }
        
        // Mocking or fetching sales data if needed
        salesToday.value = 0.0
        activeOrders.value = 0
    }
}
