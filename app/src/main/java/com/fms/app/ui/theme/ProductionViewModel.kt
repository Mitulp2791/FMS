package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class ProductionViewModel : ViewModel() {
    val masterItems = mutableStateListOf<Pair<String, Map<String, Any?>>>()
    
    // Live stock tracker for Raw Materials
    val stockLevels = mutableStateMapOf<String, Double>()

    // Separation of RM and FG based on Category
    val finishedGoods get() = masterItems.filter { it.second["type"] == "FG" }
    val rawMaterials get() = masterItems.filter { it.second["type"] == "RM" }

    // Output Data
    var selectedFgId = mutableStateOf("")
    var selectedFgName = mutableStateOf("Select Finished Good")
    var yieldQty = mutableStateOf("")
    var batchNumber = mutableStateOf("")
    var laborCost = mutableStateOf("")

    // BOM Input Data
    var selectedRmId = mutableStateOf("")
    var selectedRmName = mutableStateOf("Select Material")
    var selectedRmCost = mutableStateOf(0.0)
    var rmQty = mutableStateOf("")

    val bomInputs = mutableStateListOf<Map<String, Any>>()

    init {
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.getModuleData("Masters") { data ->
            masterItems.clear()
            masterItems.addAll(data)
        }
        // Fetch inventory to calculate available RM stock
        FirebaseRepository.getModuleData("Inventory") { ledger ->
            stockLevels.clear()
            ledger.forEach { entry ->
                val itemId = entry.second["itemId"]?.toString() ?: ""
                val change = (entry.second["change"] as? Number)?.toDouble() ?: 0.0
                if (itemId.isNotEmpty()) {
                    stockLevels[itemId] = (stockLevels[itemId] ?: 0.0) + change
                }
            }
        }
    }

    fun getAvailableStock(itemId: String): Double {
        val currentStock = stockLevels[itemId] ?: 0.0
        val inBom = bomInputs.filter { it["itemId"] == itemId }.sumOf { it["qty"] as Double }
        return currentStock - inBom
    }

    fun addRmToBom(): String? {
        val q = rmQty.value.toDoubleOrNull() ?: 0.0
        val itemId = selectedRmId.value
        
        if (itemId.isEmpty()) return "Please select a material"
        if (q <= 0) return "Quantity must be greater than 0"
        
        // SAFETY CHECK: Ensure we have enough RM stock
        if (getAvailableStock(itemId) < q) {
            return "Insufficient stock for ${selectedRmName.value}"
        }

        bomInputs.add(mapOf(
            "itemId" to itemId,
            "name" to selectedRmName.value,
            "qty" to q,
            "unitCost" to selectedRmCost.value
        ))
        
        // Reset RM fields
        selectedRmId.value = ""
        selectedRmName.value = "Select Material"
        rmQty.value = ""
        return null // Success
    }

    fun completeProduction(onSuccess: () -> Unit) {
        val yQty = yieldQty.value.toDoubleOrNull() ?: 0.0
        val lCost = laborCost.value.toDoubleOrNull() ?: 0.0
        
        if (selectedFgId.value.isNotEmpty() && bomInputs.isNotEmpty() && yQty > 0) {
            FirebaseRepository.completeProduction(
                selectedFgId.value,
                selectedFgName.value,
                yQty,
                lCost,
                batchNumber.value,
                bomInputs.toList()
            )
            
            // Reset fields
            bomInputs.clear()
            selectedFgId.value = ""
            selectedFgName.value = "Select Finished Good"
            yieldQty.value = ""
            batchNumber.value = ""
            laborCost.value = ""
            onSuccess()
        }
    }
}
