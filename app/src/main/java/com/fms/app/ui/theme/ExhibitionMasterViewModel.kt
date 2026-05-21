package com.fms.app.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

/**
 * ExhibitionMasterViewModel: Manages trade shows and exhibitions for the tenant.
 * Ensures all entries are strictly isolated to the companyId in context.
 */
class ExhibitionMasterViewModel : ViewModel() {
    
    // List of exhibitions: Pair of <FirebaseKey, DataMap>
    val exhibitionList = mutableStateListOf<Pair<String, Map<String, Any>>>()

    var isEntryStarted = mutableStateOf(false)
    var isLoading = mutableStateOf(false)

    // Form State
    var id = mutableStateOf("0")
    var exhibitorName = mutableStateOf("")
    var location = mutableStateOf("")
    var fromDate = mutableStateOf("")
    var toDate = mutableStateOf("")

    var searchQuery = mutableStateOf("")

    /**
     * Filtered list based on search query (Name or Location).
     */
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

    /**
     * Loads exhibitions from the tenant-scoped repository.
     */
    fun loadExhibitions() {
        isLoading.value = true
        FirebaseRepository.getModuleDataWithKeys("Exhibitions") { data ->
            exhibitionList.clear()
            exhibitionList.addAll(data)
            isLoading.value = false
        }
    }

    fun startNewEntry() {
        resetFields()
        isEntryStarted.value = true
    }

    /**
     * Prepares the form for editing an existing exhibition.
     */
    fun selectExhibition(exId: String, data: Map<String, Any>) {
        id.value = exId
        exhibitorName.value = data["exhibitorName"]?.toString() ?: ""
        location.value = data["location"]?.toString() ?: ""
        fromDate.value = data["fromDate"]?.toString() ?: ""
        toDate.value = data["toDate"]?.toString() ?: ""
        isEntryStarted.value = true
    }

    /**
     * Persists the exhibition data to Firebase under the tenant path.
     */
    fun saveExhibition(onSuccess: () -> Unit) {
        if (exhibitorName.value.isEmpty()) return

        val data = mapOf(
            "exhibitorName" to exhibitorName.value,
            "location" to location.value,
            "fromDate" to fromDate.value,
            "toDate" to toDate.value
        )

        val targetId = if (id.value == "0") null else id.value
        
        FirebaseRepository.saveItem("Exhibitions", targetId, data) { success ->
            if (success) {
                loadExhibitions()
                resetFields()
                onSuccess()
            }
        }
    }

    fun resetFields() {
        id.value = "0"
        exhibitorName.value = ""
        location.value = ""
        fromDate.value = ""
        toDate.value = ""
        isEntryStarted.value = false
    }

    /**
     * Removes the exhibition from the tenant's record.
     */
    fun deleteExhibition(exId: String) {
        FirebaseRepository.deleteItem("Exhibitions", exId) { success ->
            if (success) {
                loadExhibitions()
            }
        }
    }
}