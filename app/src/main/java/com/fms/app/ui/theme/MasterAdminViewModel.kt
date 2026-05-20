package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Tenant(val companyCode: String, val companyName: String)

class MasterAdminViewModel : ViewModel() {
    val tenants = mutableStateListOf<Tenant>()

    var isLoading by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        fetchAllTenants()
    }

    private fun fetchAllTenants() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Businesses")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Tenant>()
                for (child in snapshot.children) {
                    val code = child.key ?: continue
                    val name = child.child("BusinessProfile").child("companyName").value?.toString() ?: "Unknown Company"
                    list.add(Tenant(code, name))
                }
                tenants.clear()
                tenants.addAll(list)
            }

            override fun onCancelled(error: DatabaseError) {
                statusMessage = "Database Error: ${error.message}"
            }
        })
    }

    fun provisionNewBusiness(companyCode: String, companyName: String) {
        val cleanCode = companyCode.uppercase().trim()
        if (cleanCode.isBlank() || companyName.isBlank()) {
            statusMessage = "Both Company Code and Name are required."
            return
        }

        isLoading = true
        statusMessage = null

        val db = FirebaseDatabase.getInstance().getReference("Businesses/$cleanCode")

        val initialSetup = mapOf(
            "BusinessProfile" to mapOf(
                "companyName" to companyName,
                "status" to "Active"
            ),
            "System" to mapOf(
                "Settings" to mapOf(
                    "DocumentSequences" to mapOf(
                        "INWARD" to mapOf("prefix" to "GRN", "sequence" to 1),
                        "PRODUCTION" to mapOf("prefix" to "MFG", "sequence" to 1),
                        "BILLING" to mapOf("prefix" to "INV", "sequence" to 1)
                    )
                )
            )
        )

        db.setValue(initialSetup).addOnCompleteListener { task ->
            isLoading = false
            // Warning fixed by lifting the assignment out of the 'if' block
            statusMessage = if (task.isSuccessful) {
                "Tenant '$cleanCode' Provisioned Successfully!"
            } else {
                "Failed to provision tenant: ${task.exception?.localizedMessage}"
            }
        }
    }

    fun clearStatus() {
        statusMessage = null
    }
}