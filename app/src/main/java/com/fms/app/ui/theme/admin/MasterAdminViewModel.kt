package com.fms.app.ui.theme.admin

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.fms.app.data.UserSession

data class TenantCompany(
    val id: String = "",
    val name: String = "",
    val tier: String = "Free",
    val maxUsers: Int = 5,
    val isActive: Boolean = true
)

class MasterAdminViewModel : ViewModel() {
    val companies = mutableStateListOf<TenantCompany>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchAllCompanies()
    }

    fun fetchAllCompanies() {
        isLoading = true
        FirebaseDatabase.getInstance().getReference("SYSTEM").child("companyRegistry")
            .get().addOnSuccessListener { snapshot ->
                val list = mutableListOf<TenantCompany>()
                snapshot.children.forEach { snap ->
                    val idKey = snap.key ?: ""
                    val name = snap.child("name").value?.toString() ?: ""
                    val tier = snap.child("tier").value?.toString() ?: "Free"
                    val maxUsers = snap.child("maxUsers").getValue(Int::class.java) ?: 5
                    val isActive = snap.child("isActive").value as? Boolean ?: true

                    list.add(TenantCompany(idKey, name, tier, maxUsers, isActive))
                }
                companies.clear()
                companies.addAll(list)
                isLoading = false
            }.addOnFailureListener {
                errorMessage = "Directory download broken: ${it.message}"
                isLoading = false
            }
    }

    fun createCompany(id: String, name: String, tier: String, maxUsers: Int) {
        if (id.isBlank() || name.isBlank()) {
            errorMessage = "Please fill out all parameter definitions."
            return
        }

        val companyData = mapOf(
            "name" to name,
            "tier" to tier,
            "maxUsers" to maxUsers,
            "isActive" to true,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("SYSTEM").child("companyRegistry").child(id)
            .setValue(companyData)
            .addOnSuccessListener {
                val baseStructure = mapOf(
                    "System/Settings/companyName" to name,
                    "System/Settings/isActive" to true
                )
                FirebaseDatabase.getInstance().getReference("companies/$id").updateChildren(baseStructure)
                fetchAllCompanies()
            }
            .addOnFailureListener {
                errorMessage = "Failed to board client company: ${it.message}"
            }
    }

    fun updateCompanyDetails(id: String, name: String, tier: String, maxUsers: Int) {
        if (id.isBlank() || name.isBlank()) {
            errorMessage = "Company Name cannot be blank."
            return
        }

        val updates = mapOf(
            "name" to name,
            "tier" to tier,
            "maxUsers" to maxUsers
        )

        FirebaseDatabase.getInstance().getReference("SYSTEM").child("companyRegistry").child(id)
            .updateChildren(updates)
            .addOnSuccessListener {
                FirebaseDatabase.getInstance().getReference("companies/$id/System/Settings/companyName")
                    .setValue(name)
                fetchAllCompanies()
            }
            .addOnFailureListener {
                errorMessage = "Failed to update company parameters: ${it.message}"
            }
    }

    fun toggleCompanyStatus(companyId: String, currentStatus: Boolean) {
        FirebaseDatabase.getInstance().getReference("SYSTEM").child("companyRegistry").child(companyId)
            .child("isActive").setValue(!currentStatus)
            .addOnSuccessListener {
                fetchAllCompanies()
            }
    }

    fun loginAsTenant(companyId: String, onComplete: () -> Unit) {
        UserSession.startImpersonation(companyId)
        onComplete()
    }
}