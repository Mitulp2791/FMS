package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.fms.app.data.UserSession

class InventoryViewModel : ViewModel() {
    val stockItems = mutableStateListOf<Map<String, String>>()
    var isLoading by mutableStateOf(false)
        private set

    init {
        fetchInventoryState()
    }

    private fun fetchInventoryState() {
        isLoading = true
        // Scoped to the current tenant path defined in Repository
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("Businesses/${UserSession.companyId ?: "UNAUTHORIZED_TENANT"}/Inventory/StockLedger")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ledger = mutableListOf<Map<String, String>>()
                for (child in snapshot.children) {
                    val entry = mutableMapOf<String, String>()
                    entry["id"] = child.key ?: ""
                    for (prop in child.children) {
                        entry[prop.key ?: ""] = prop.value?.toString() ?: ""
                    }
                    ledger.add(entry)
                }
                stockItems.clear()
                stockItems.addAll(ledger)
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }
}