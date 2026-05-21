package com.fms.app.data

/**
 * UserSession: The central authority for the current user's identity and permissions.
 * Designed for Multi-Tenant SaaS (Principal Architect Level).
 */
object UserSession {
    // Basic Identity
    var userId: String? = null
    var role: String? = null
    
    // Multi-Tenancy
    var companyId: String? = null // The current active tenant ID
    var companyName: String? = null
    var originalCompanyId: String? = null // For Master Admin impersonation tracking
    
    // RBAC and SaaS Governance
    var isImpersonating: Boolean = false
    var permissions: Map<String, Map<String, Boolean>>? = null
    var subscriptionTier: String = "Free" // Free, Pro, Enterprise
    var maxUsersAllowed: Int = 5
    var isAccountActive: Boolean = true

    /**
     * Master Admin (God Tier) Check
     */
    fun isMasterAdmin(): Boolean = role == "MasterAdmin"

    /**
     * RBAC Check: Validates if the user can perform a specific action in a module.
     * Master Admins bypass all checks.
     */
    fun hasPermission(module: String, action: String): Boolean {
        if (isMasterAdmin()) return true
        if (!isAccountActive) return false
        return permissions?.get(module)?.get(action) ?: false
    }

    /**
     * Multi-Tenant Impersonation: Allows Master Admin to jump into any company context.
     */
    fun startImpersonation(targetTenantId: String) {
        if (!isMasterAdmin()) return
        if (originalCompanyId == null) {
            originalCompanyId = companyId
        }
        companyId = targetTenantId
        isImpersonating = true
    }

    fun stopImpersonation() {
        if (isImpersonating) {
            companyId = originalCompanyId
            originalCompanyId = null
            isImpersonating = false
        }
    }

    /**
     * Session Cleanup on Logout
     */
    fun clear() {
        userId = null
        companyId = null
        companyName = null
        role = null
        originalCompanyId = null
        isImpersonating = false
        permissions = null
        subscriptionTier = "Free"
        maxUsersAllowed = 5
        isAccountActive = true
    }
}