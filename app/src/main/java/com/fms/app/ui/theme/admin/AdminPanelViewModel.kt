package com.fms.app.ui.theme.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminPanelViewModel : ViewModel() {
    val tenants = mutableStateListOf<Map<String, Any>>()
    var isLoading by mutableStateOf(false)
        private set

    init {
        loadTenants()
    }

    private fun loadTenants() {
        isLoading = true
        // Updated to read from the new SYSTEM tier registry
        val dbRef = FirebaseDatabase.getInstance().getReference("SYSTEM/companyRegistry")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tenantList = mutableListOf<Map<String, Any>>()
                for (child in snapshot.children) {
                    val data = mutableMapOf<String, Any>()
                    data["companyId"] = child.key ?: ""
                    data["isActive"] = child.child("isActive").value ?: true
                    data["name"] = child.child("name").value?.toString() ?: "Unnamed Tenant"
                    tenantList.add(data)
                }
                tenants.clear()
                tenants.addAll(tenantList)
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    fun toggleTenantStatus(companyId: String, currentStatus: Boolean) {
        val dbRef = FirebaseDatabase.getInstance().getReference("SYSTEM/companyRegistry/$companyId/isActive")
        dbRef.setValue(!currentStatus).addOnSuccessListener {
            loadTenants()
        }
    }
}