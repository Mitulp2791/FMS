package com.fms.app.data

object UserSession {
    var userId: String? = null
    var companyId: String? = null
    var role: String? = null // Supported roles: MasterAdmin, Admin, Cashier, InventoryManager

    /**
     * Checks if the currently authenticated session possesses Master Administrator clearance.
     */
    fun isMasterAdmin(): Boolean {
        return role == "MasterAdmin"
    }

    /**
     * Enforces operational capabilities across tenant boundaries.
     * Maps authorization rules cleanly to prevent non-privileged personnel from accessing restricted operations.
     */
    fun hasAccessToModule(moduleName: String): Boolean {
        if (role == "MasterAdmin") return true

        return when (role) {
            "Admin" -> {
                // Admins have unfettered access to all local business operational nodes
                true
            }
            "Cashier" -> {
                // Cashiers are restricted exclusively to sales environments, invoice generation, and customer indexing
                moduleName == "Billing" || moduleName == "Accounts" || moduleName == "Dashboard"
            }
            "InventoryManager" -> {
                // Inventory Managers are restricted to materials cataloging, inward goods, and consumption logs
                moduleName == "Inventory" || moduleName == "Masters" || moduleName == "Processes" || moduleName == "Transactions" || moduleName == "Dashboard"
            }
            else -> false
        }
    }

    /**
     * Purges authorization cache context safely during termination sequences.
     */
    fun clear() {
        userId = null
        companyId = null
        role = null
    }
}