package com.fms.app.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class ExhibitionMasterViewModel : ViewModel() {
    val exhibitionList = mutableStateListOf<Pair<String, Map<String, Any?>>>()

    var isEntryStarted = mutableStateOf(false)

    var id = mutableStateOf("0")
    var exhibitorName = mutableStateOf("")
    var location = mutableStateOf("")
    var fromDate = mutableStateOf("")
    var toDate = mutableStateOf("")

    var searchQuery = mutableStateOf("")

    val filteredExhibitionList = derivedStateOf {
        if (searchQuery.value.isEmpty()) {
            exhibitionList
        } else {
            exhibitionList.filter { ex ->
                val name = ex.second["exhibitorName"]?.toString() ?: ""
                val loc = ex.second["location"]?.toString() ?: ""
                name.contains(searchQuery.value, ignoreCase = true) ||
                        loc.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    init {
        loadExhibitions()
    }

    private fun loadExhibitions() {
        FirebaseRepository.getModuleData("Exhibitions") { data ->
            exhibitionList.clear()
            exhibitionList.addAll(data)
        }
    }

    fun startNewEntry() {
        resetFields()
        isEntryStarted.value = true
    }

    fun selectExhibition(exId: String, data: Map<String, Any?>) {
        id.value = exId
        exhibitorName.value = data["exhibitorName"]?.toString() ?: ""
        location.value = data["location"]?.toString() ?: ""
        fromDate.value = data["fromDate"]?.toString() ?: ""
        toDate.value = data["toDate"]?.toString() ?: ""
        isEntryStarted.value = true
    }

    fun saveExhibition(onSuccess: () -> Unit) {
        if (exhibitorName.value.isEmpty()) return

        val data = mapOf(
            "exhibitorName" to exhibitorName.value,
            "location" to location.value,
            "fromDate" to fromDate.value,
            "toDate" to toDate.value
        )

        FirebaseRepository.saveItem("Exhibitions", if (id.value == "0") null else id.value, data)
        resetFields()
        onSuccess()
    }

    fun resetFields() {
        id.value = "0"
        exhibitorName.value = ""
        location.value = ""
        fromDate.value = ""
        toDate.value = ""
        isEntryStarted.value = false
    }

    fun deleteExhibition(id: String) {
        FirebaseRepository.deleteItem("Exhibitions", id)
    }
}
