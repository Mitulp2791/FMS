package com.fms.app.data

object UserSession {

    // =========================
    // AUTH IDENTITY
    // =========================
    var userId: String? = null

    // =========================
    // TENANT (COMPANY)
    // =========================
    var companyId: String? = null

    // =========================
    // ROLE
    // =========================
    var role: String? = null

    // =========================
    // GLOBAL CONFIG CACHE
    // =========================
    var globalModules: Map<String, Boolean> = emptyMap()

    // =========================
    // COMPANY MODULE ACCESS
    // =========================
    var companyModules: Map<String, Boolean> = emptyMap()

    // =========================
    // ROLE PERMISSIONS
    // =========================
    var permissions: Map<String, Any> = emptyMap()

    // =========================
    // RESET SESSION
    // =========================
    fun clear() {
        userId = null
        companyId = null
        role = null
        globalModules = emptyMap()
        companyModules = emptyMap()
        permissions = emptyMap()
    }

    // =========================
    // ACCESS CHECK HELPERS
    // =========================

    fun canAccessModule(module: String): Boolean {
        return globalModules[module] == true &&
                companyModules[module] == true
    }

    fun canRead(module: String): Boolean {
        val modulePerm = (permissions[module] as? Map<*, *>) ?: return false
        return modulePerm["read"] == true
    }

    fun canWrite(module: String): Boolean {
        val modulePerm = (permissions[module] as? Map<*, *>) ?: return false
        return modulePerm["write"] == true
    }
}