package com.fms.app.ui.theme

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class ItemMasterViewModel : ViewModel() {
    val itemsList = mutableStateListOf<Pair<String, Map<String, Any?>>>()

    var name = mutableStateOf("")
    var rawSkuInput = mutableStateOf("")
    var hsnCode = mutableStateOf("")
    var category = mutableStateOf("Raw Material")
    var uom = mutableStateOf("Kg")
    var openingCost = mutableStateOf("")
    var sellingPrice = mutableStateOf("")

    init {
        loadItems()
    }

    private fun loadItems() {
        FirebaseRepository.getModuleData("Masters") { data ->
            itemsList.clear()
            itemsList.addAll(data)
        }
    }

    fun saveItem(onSuccess: () -> Unit) {
        val skuPrefix = when (category.value) {
            "Finished Good" -> "FG"
            "Raw Material" -> "RM"
            else -> "PM"
        }
        val finalSku = "$skuPrefix${rawSkuInput.value}"
        val cost = openingCost.value.toDoubleOrNull() ?: 0.0
        val sell = sellingPrice.value.toDoubleOrNull() ?: 0.0
        val typeCode = if (category.value == "Finished Good") "FG" else "RM"

        val data = mapOf(
            "name" to name.value,
            "sku" to finalSku,
            "hsn" to hsnCode.value,
            "category" to category.value,
            "uom" to uom.value,
            "costPerUnit" to cost,
            "sellingPrice" to sell,
            "type" to typeCode
        )

        FirebaseRepository.saveItem("Masters", null, data)
        
        // Reset fields
        name.value = ""
        rawSkuInput.value = ""
        hsnCode.value = ""
        openingCost.value = ""
        sellingPrice.value = ""
        
        onSuccess()
    }
}
