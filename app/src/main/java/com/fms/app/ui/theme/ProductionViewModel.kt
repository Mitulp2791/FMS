package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class ProductionViewModel : ViewModel() {
    
    var selectedFgId = mutableStateOf("")
    var selectedFgName = mutableStateOf("")
    var batchNumber = mutableStateOf("")
    var yieldQty = mutableStateOf("")
    var laborCost = mutableStateOf("")

    var selectedRmId = mutableStateOf("")
    var selectedRmName = mutableStateOf("")
    var selectedRmCost = mutableStateOf(0.0)
    var rmQty = mutableStateOf("")

    val finishedGoods = mutableStateListOf<Pair<String, Map<String, Any>>>()
    val rawMaterials = mutableStateListOf<Pair<String, Map<String, Any>>>()
    val bomInputs = mutableStateListOf<Map<String, Any>>()
    
    private val stocks = mutableMapOf<String, Double>()

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleDataWithKeys("Masters") { items ->
            finishedGoods.clear()
            rawMaterials.clear()
            items.forEach { (id, data) ->
                val category = data["category"]?.toString()?.uppercase() ?: ""
                if (category == "FINISHED GOOD" || category == "FG") {
                    finishedGoods.add(id to data)
                } else {
                    rawMaterials.add(id to data)
                }
            }
        }
        
        FirebaseRepository.getModuleDataWithKeys("Inventory/Stocks") { data ->
            stocks.clear()
            data.forEach { (id, value) ->
                stocks[id] = (value["currentStock"] as? Number)?.toDouble() ?: 0.0
            }
        }
    }

    fun getAvailableStock(id: String): Double {
        return stocks[id] ?: 0.0
    }

    fun addRmToBom(): String? {
        val id = selectedRmId.value
        val name = selectedRmName.value
        val qty = rmQty.value.toDoubleOrNull() ?: 0.0
        val cost = selectedRmCost.value

        if (id.isEmpty()) return "Select a material"
        if (qty <= 0) return "Enter valid quantity"
        
        bomInputs.add(mapOf(
            "itemId" to id,
            "name" to name,
            "qty" to qty,
            "unitCost" to cost
        ))
        
        // Reset RM selection
        selectedRmId.value = ""
        selectedRmName.value = ""
        selectedRmCost.value = 0.0
        rmQty.value = ""
        
        return null
    }

    fun completeProduction(onComplete: () -> Unit) {
        val productionData = mapOf(
            "outputItem" to selectedFgId.value,
            "outputQty" to (yieldQty.value.toDoubleOrNull() ?: 0.0),
            "laborCost" to (laborCost.value.toDoubleOrNull() ?: 0.0),
            "batch" to batchNumber.value
        )

        FirebaseRepository.completeProduction(productionData, bomInputs.toList()) { success ->
            if (success) {
                // Clear state
                bomInputs.clear()
                batchNumber.value = ""
                yieldQty.value = ""
                laborCost.value = ""
                selectedFgId.value = ""
                selectedFgName.value = ""
                onComplete()
                loadData() // Refresh stocks
            }
        }
    }
}
