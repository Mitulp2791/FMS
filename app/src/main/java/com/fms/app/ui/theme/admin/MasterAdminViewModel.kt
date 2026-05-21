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

/**
 * MasterAdminViewModel: The top-tier controller for the SaaS owner.
 * Manages company onboarding, subscription states, and tenant impersonation.
 */
class MasterAdminViewModel : ViewModel() {
    val companies = mutableStateListOf<TenantCompany>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchAllCompanies()
    }

    /**
     * Fetches all registered companies from the global /Companies/ node.
     */
    fun fetchAllCompanies() {
        isLoading = true
        FirebaseDatabase.getInstance().getReference("Companies")
            .get().addOnSuccessListener { snapshot ->
                val list = mutableListOf<TenantCompany>()
                snapshot.children.forEach { snap ->
                    val company = snap.getValue(TenantCompany::class.java)
                    if (company != null) {
                        list.add(company.copy(id = snap.key ?: ""))
                    }
                }
                companies.clear()
                companies.addAll(list)
                isLoading = false
            }.addOnFailureListener {
                errorMessage = "Failed to load companies: ${it.message}"
                isLoading = false
            }
    }

    /**
     * Onboard a new business into the SaaS platform.
     * Creates entries in both the /Companies global index and the /Businesses tenant container.
     */
    fun createCompany(id: String, name: String, tier: String, maxUsers: Int) {
        if (id.isBlank() || name.isBlank()) {
            errorMessage = "Company ID and Name are required"
            return
        }
        
        val companyData = mapOf(
            "name" to name,
            "tier" to tier,
            "maxUsers" to maxUsers,
            "isActive" to true,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("Companies").child(id)
            .setValue(companyData)
            .addOnSuccessListener {
                // Initialize the tenant-specific configuration with default Admin permissions
                val defaultConfig = mapOf(
                    "config/roles/Admin/Dashboard/view" to true,
                    "config/roles/Admin/Inventory/view" to true,
                    "config/roles/Admin/Billing/create" to true
                )
                FirebaseDatabase.getInstance().getReference("Businesses/$id").updateChildren(defaultConfig)
                fetchAllCompanies()
            }
            .addOnFailureListener {
                errorMessage = "Failed to create company: ${it.message}"
            }
    }

    /**
     * Suspends or activates a tenant account.
     */
    fun toggleCompanyStatus(companyId: String, currentStatus: Boolean) {
        FirebaseDatabase.getInstance().getReference("Companies").child(companyId)
            .child("isActive").setValue(!currentStatus)
            .addOnSuccessListener { 
                fetchAllCompanies() 
            }
    }

    /**
     * Allows the Master Admin to jump into a specific tenant's context.
     */
    fun loginAsTenant(companyId: String, onComplete: () -> Unit) {
        UserSession.startImpersonation(companyId)
        onComplete()
    }
}