package com.fms.app.data

object ERPFieldSchema {
    // Defines the structure for dynamic modules, now strictly enforcing tenant-wide consistency
    private val schemaDefinitions = mapOf(
        "Masters" to mapOf(
            "name" to "String",
            "alias" to "String",
            "costPerUnit" to "Double",
            "type" to "String" // RM or FG
        ),
        "Accounts" to mapOf(
            "name" to "String",
            "accountType" to "String",
            "gstin" to "String",
            "mobile" to "String"
        )
    )

    fun getSchema(moduleName: String, onResult: (Map<String, String>) -> Unit) {
        // In a SaaS model, schemas could eventually be tenant-customizable.
        // For now, we return the base definition.
        onResult(schemaDefinitions[moduleName] ?: emptyMap())
    }
}