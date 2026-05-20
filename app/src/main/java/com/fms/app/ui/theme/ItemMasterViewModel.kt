package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository

class ItemMasterViewModel : ViewModel() {

    // State collection to store the items belonging to the active tenant
    val items = mutableStateListOf<Map<String, String>>()

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadItems()
    }

    /**
     * Fetches items strictly scoped to the tenantId authenticated in UserSession.
     * The repository automatically handles the pathing.
     */
    fun loadItems() {
        isLoading = true
        FirebaseRepository.getAllItems { fetchedItems ->
            items.clear()
            items.addAll(fetchedItems)
            isLoading = false
        }
    }

    /**
     * Saves or updates an item record. The repository automatically injects the
     * tenantId and lastUpdated metadata into the payload.
     */
    fun saveItem(
        id: String?,
        data: Map<String, Any>,
        onResult: (Boolean) -> Unit
    ) {
        isLoading = true
        FirebaseRepository.saveDynamicRecord("Masters", id, data) { success ->
            isLoading = false
            if (success) {
                // Refresh local state after successful tenant-scoped persistence
                loadItems()
            }
            onResult(success)
        }
    }
}