package com.fms.app.data

object ERPFieldSchema {

    /**
     * Provides the specific field structure for each module of the ERP.
     * This defines what inputs the user sees when adding or editing items.
     * Multi-tenant architecture delegates 'tenantId' injection strictly to the Repository layer.
     */
    fun getSchema(module: String, onResult: (Map<String, String>) -> Unit) {
        val schema = mutableMapOf<String, String>()

        when(module) {
            "Masters" -> {
                schema["name"] = "String"
                schema["costPerUnit"] = "Number"
                schema["uom"] = "String (ML/PCS/KG/LTR)"
                schema["category"] = "String"
                schema["type"] = "String (RM/FG/SV)"
                schema["hsn"] = "String"
                schema["sku"] = "String"
            }
            "Accounts" -> {
                schema["name"] = "String"
                schema["accountType"] = "String (Customer/Supplier)"
                schema["phone"] = "String"
                schema["email"] = "String"
                schema["city"] = "String"
                schema["state"] = "String"
                schema["country"] = "String"
                schema["gstRegistrationType"] = "String"
                schema["supplierFrequency"] = "String"
                schema["defaultCurrency"] = "String"
                schema["tdsTcsApplicable"] = "String"
            }
            "Inventory" -> {
                schema["itemId"] = "String"
                schema["change"] = "Number"
                schema["type"] = "String"
            }
            "Processes" -> {
                schema["processName"] = "String"
                schema["outputItem"] = "String"
                schema["outputQty"] = "Number"
                schema["status"] = "String"
            }
            "Reports" -> {
                schema["report_type"] = "String"
                schema["date_range"] = "String"
            }
            "Transactions" -> {
                schema["itemId"] = "String"
                schema["qty"] = "Number"
                schema["type"] = "String"
                schema["date"] = "String"
            }
        }
        onResult(schema)
    }
}