package com.fms.app.data

object ERPFieldSchema {

    /**
     * Provides the specific field structure for each module of the ERP.
     * This defines what inputs the user sees when adding or editing items.
     */
    fun getSchema(module: String, onResult: (Map<String, String>) -> Unit) {
        val schema = mutableMapOf<String, String>()

        when(module) {
            "Masters" -> {
                schema["name"] = "String"
                schema["costPerUnit"] = "Number"
                schema["uom"] = "String (ML/PCS/KG/GM)"
                schema["category"] = "String (Oil/Chemical/Bottle/Box)"
                schema["type"] = "String (RAW/PACKAGING/FG/SERVICE)"
            }
            "Processes" -> {
                schema["processName"] = "Process Name (e.g., Mixing Rose Batch)"
                schema["outputItem"] = "Finished Product ID"
                schema["outputQty"] = "Quantity Produced"
                schema["status"] = "Status (Draft/Completed)"
            }
            "Billing" -> {
                schema["customerName"] = "String"
                schema["amount"] = "Number"
                schema["status"] = "String (Paid/Pending)"
            }
            "Exhibitions" -> {
                schema["exhibitorName"] = "Exhibitor Name"
                schema["location"] = "Location"
                schema["fromDate"] = "Date (yyyy-MM-dd)"
                schema["toDate"] = "Date (yyyy-MM-dd)"
            }
        }
        onResult(schema)
    }
}