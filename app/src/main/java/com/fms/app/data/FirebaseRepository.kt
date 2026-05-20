package com.fms.app.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FirebaseRepository {

    private val database = FirebaseDatabase.getInstance()

    // STRICT TENANT ISOLATION: The entire database path is gated by the current UserSession's companyId
    private val bizPath get() = database.getReference("Businesses/${UserSession.companyId ?: "UNAUTHORIZED_TENANT"}")

    private fun getPathForModule(module: String) = when(module) {
        "Masters" -> bizPath.child("Masters/Items")
        "Accounts" -> bizPath.child("Masters/Accounts")
        "Vendors" -> bizPath.child("Masters/Accounts")
        "Customers" -> bizPath.child("Masters/Accounts")
        "Inventory" -> bizPath.child("Inventory/StockLedger")
        "Processes" -> bizPath.child("Processes")
        "Billing" -> bizPath.child("Billing/Invoices")
        "Transactions" -> bizPath.child("Transactions")
        else -> bizPath.child(module)
    }

    private fun generateDynamicDocNumber(sequenceType: String, onGenerated: (String) -> Unit) {
        val configRef = bizPath.child("System/Settings/DocumentSequences").child(sequenceType)
        configRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val prefix = snapshot.child("prefix").value?.toString() ?: "DOC"
                val seq = snapshot.child("sequence").value?.toString()?.toIntOrNull() ?: 1
                val useMonth = snapshot.child("useMonth").value as? Boolean ?: true
                val useYear = snapshot.child("useYear").value as? Boolean ?: true

                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR).toString().takeLast(2)
                val month = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)

                var docNum = prefix
                if (useYear) docNum += "/$year"
                if (useMonth) docNum += "/$month"
                docNum += "/${String.format(Locale.US, "%04d", seq)}"

                configRef.child("sequence").setValue(seq + 1)
                onGenerated(docNum)
            }

            override fun onCancelled(error: DatabaseError) {
                onGenerated("ERR-${System.currentTimeMillis()}")
            }
        })
    }

    // --- ATOMIC TRANSACTION ENGINE ---
    fun createInvoice(
        customerId: String, customerName: String, customerMobile: String, items: List<Map<String, Any>>,
        baseAmount: Double, isGstApplicable: Boolean, gstType: String, totalGst: Double, grandTotal: Double,
        saleType: String = ""
    ) {
        generateDynamicDocNumber("BILLING") { documentNumber ->
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

            val updates = mutableMapOf<String, Any?>()
            val invoiceId = getPathForModule("Billing").push().key ?: return@generateDynamicDocNumber

            var cgstAmount = 0.0; var sgstAmount = 0.0; var igstAmount = 0.0
            if (isGstApplicable) {
                if (gstType == "IGST") igstAmount = totalGst else { cgstAmount = totalGst / 2; sgstAmount = totalGst / 2 }
            }

            val invoiceData = mapOf(
                "tenantId" to (UserSession.companyId ?: "UNAUTHORIZED_TENANT"),
                "invoiceNumber" to documentNumber,
                "customerId" to customerId,
                "customerName" to customerName,
                "customerMobile" to customerMobile,
                "saleType" to saleType,
                "date" to dateStr,
                "timestamp" to timestamp,
                "items" to items,
                "baseAmount" to baseAmount,
                "isGstApplicable" to isGstApplicable,
                "cgstAmount" to cgstAmount,
                "sgstAmount" to sgstAmount,
                "igstAmount" to igstAmount,
                "grandTotal" to grandTotal,
                "type" to "SALE_OUTWARD",
                "createdBy" to (UserSession.userId ?: "SYSTEM")
            )

            // Map the invoice creation update
            updates["Billing/Invoices/$invoiceId"] = invoiceData

            // Map the inventory deduction updates (Atomically processed alongside the invoice)
            items.forEach { item ->
                val itemId = item["itemId"]?.toString() ?: ""
                val qty = item["qty"]?.toString()?.toDoubleOrNull() ?: 0.0
                if (itemId.isNotEmpty()) {
                    val ledgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@forEach
                    updates["Inventory/StockLedger/$ledgerKey"] = mapOf(
                        "tenantId" to (UserSession.companyId ?: "UNAUTHORIZED_TENANT"),
                        "itemId" to itemId,
                        "change" to -qty,
                        "type" to "SALE ($documentNumber)",
                        "timestamp" to timestamp
                    )
                }
            }

            // Commit atomic transaction
            bizPath.updateChildren(updates)
        }
    }

    fun createInwardTransaction(
        supplierId: String, supplierName: String, referenceNo: String, itemId: String, qty: Double,
        baseTotal: Double, isGstApplicable: Boolean, gstPercent: Double, gstType: String, totalGst: Double
    ) {
        generateDynamicDocNumber("INWARD") { documentNumber ->
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

            val landedPerUnit = if (qty > 0) (baseTotal + totalGst) / qty else 0.0
            val updates = mutableMapOf<String, Any?>()

            val txnId = getPathForModule("Transactions").push().key ?: return@generateDynamicDocNumber

            var cgstAmount = 0.0; var sgstAmount = 0.0; var igstAmount = 0.0
            if (isGstApplicable) {
                if (gstType == "IGST") igstAmount = totalGst else { cgstAmount = totalGst / 2; sgstAmount = totalGst / 2 }
            }

            updates["Transactions/$txnId"] = mapOf(
                "tenantId" to (UserSession.companyId ?: "UNAUTHORIZED_TENANT"),
                "documentNumber" to documentNumber,
                "itemId" to itemId,
                "qty" to qty,
                "landedCost" to landedPerUnit,
                "supplierId" to supplierId,
                "supplierName" to supplierName,
                "referenceNo" to referenceNo,
                "type" to "GRN_INWARD",
                "date" to dateStr,
                "timestamp" to timestamp,
                "userId" to (UserSession.userId ?: "SYSTEM"),
                "isGstApplicable" to isGstApplicable,
                "gstPercent" to gstPercent,
                "cgstAmount" to cgstAmount,
                "sgstAmount" to sgstAmount,
                "igstAmount" to igstAmount
            )

            val ledgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@generateDynamicDocNumber
            updates["Inventory/StockLedger/$ledgerKey"] = mapOf(
                "tenantId" to (UserSession.companyId ?: "UNAUTHORIZED_TENANT"),
                "itemId" to itemId,
                "change" to qty,
                "type" to "INWARD ($documentNumber)",
                "timestamp" to timestamp,
                "unitCost" to landedPerUnit,
                "totalValue" to (qty * landedPerUnit)
            )

            bizPath.updateChildren(updates)
        }
    }

    fun getAllItems(onResult: (List<Map<String, String>>) -> Unit) {
        getPathForModule("Masters").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Map<String, String>>()
                for (child in snapshot.children) {
                    val map = mutableMapOf<String, String>()
                    map["id"] = child.key ?: ""
                    for (prop in child.children) {
                        map[prop.key ?: ""] = prop.value?.toString() ?: ""
                    }
                    items.add(map)
                }
                onResult(items)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    fun getAccountsByType(type: String, onResult: (List<Map<String, String>>) -> Unit) {
        getPathForModule("Accounts").orderByChild("accountType").equalTo(type)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val accounts = mutableListOf<Map<String, String>>()
                    for (child in snapshot.children) {
                        val map = mutableMapOf<String, String>()
                        map["id"] = child.key ?: ""
                        for (prop in child.children) {
                            map[prop.key ?: ""] = prop.value?.toString() ?: ""
                        }
                        accounts.add(map)
                    }
                    onResult(accounts)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    fun saveDynamicRecord(module: String, id: String?, data: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = getPathForModule(module)
        val targetRef = if (id.isNullOrEmpty()) ref.push() else ref.child(id)

        val enrichedData = data.toMutableMap()
        // Force the tenant ID mapping on every direct save
        enrichedData["tenantId"] = UserSession.companyId ?: "UNAUTHORIZED_TENANT"
        enrichedData["lastUpdated"] = System.currentTimeMillis()
        enrichedData["updatedBy"] = UserSession.userId ?: "SYSTEM"

        targetRef.setValue(enrichedData).addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }
}