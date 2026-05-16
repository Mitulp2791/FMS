package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {
    var totalStockValue = mutableStateOf(0.0)
    var salesToday = mutableStateOf(0.0)
    var activeOrders = mutableStateOf(0)
    var lowStockItems = mutableStateOf(0)

    init {
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // 1. Calculate Stock Value & Low Stock
        FirebaseRepository.getModuleData("Masters") { items ->
            FirebaseRepository.getModuleData("Inventory") { ledger ->
                var totalValue = 0.0
                var lowCount = 0
                
                items.forEach { item ->
                    val id = item.first
                    val cost = (item.second["costPerUnit"] as? Number)?.toDouble() ?: 0.0
                    val balance = ledger.filter { it.second["itemId"] == id }
                        .sumOf { (it.second["change"] as? Number)?.toDouble() ?: 0.0 }
                    
                    if (balance > 0) totalValue += (balance * cost)
                    if (balance < 10) lowCount++ // Threshold for low stock
                }
                totalStockValue.value = totalValue
                lowStockItems.value = lowCount
            }
        }

        // 2. Calculate Sales Today
        FirebaseRepository.getModuleData("Billing") { invoices ->
            val total = invoices.filter { it.second["date"] == today }
                .sumOf { (it.second["grandTotal"] as? Number)?.toDouble() ?: 0.0 }
            salesToday.value = total
            activeOrders.value = invoices.filter { it.second["date"] == today }.size
        }
    }
}
