package com.fms.app.data

import android.content.Context
import android.content.Intent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FirebaseRepository {

    private val database = FirebaseDatabase.getInstance("https://fragrance-management-system-default-rtdb.firebaseio.com")
    
    // DYNAMIC PATH: Switches based on logged-in company
    private val bizPath get() = database.getReference("Businesses/${UserSession.companyId ?: "biz_demo"}")

    private fun getPathForModule(module: String) = when(module) {
        "Masters" -> bizPath.child("Masters/Items")
        "Accounts" -> bizPath.child("Masters/Accounts")
        "Vendors" -> bizPath.child("Masters/Accounts") // Redirected
        "Customers" -> bizPath.child("Masters/Accounts") // Redirected
        "Inventory" -> bizPath.child("Inventory/StockLedger")
        "Processes" -> bizPath.child("Processes")
        "Billing" -> bizPath.child("Billing/Invoices")
        "Transactions" -> bizPath.child("Transactions")
        else -> bizPath.child(module)
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
                "invoiceNumber" to documentNumber, "customerId" to customerId, "customerName" to customerName,
                "customerMobile" to customerMobile, "saleType" to saleType,
                "date" to dateStr, "timestamp" to timestamp, "items" to items, "baseAmount" to baseAmount,
                "isGstApplicable" to isGstApplicable, "cgstAmount" to cgstAmount, "sgstAmount" to sgstAmount, "igstAmount" to igstAmount,
                "grandTotal" to grandTotal, "type" to "SALE_OUTWARD", "createdBy" to UserSession.userId
            )
            updates["Billing/Invoices/$invoiceId"] = invoiceData

            items.forEach { item ->
                val itemId = item["itemId"]?.toString() ?: ""
                val qty = item["qty"]?.toString()?.toDoubleOrNull() ?: 0.0
                if (itemId.isNotEmpty()) {
                    val ledgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@forEach
                    updates["Inventory/StockLedger/$ledgerKey"] = mapOf(
                        "itemId" to itemId, "change" to -qty, "type" to "SALE ($documentNumber)", 
                        "timestamp" to timestamp, "userId" to UserSession.userId
                    )
                }
            }
            bizPath.updateChildren(updates)
        }
    }

    fun bookPurchase(
        itemId: String, qty: Double, unitPrice: Double, serviceCost: Double,
        supplierId: String, supplierName: String, referenceNo: String,
        isGstApplicable: Boolean, gstType: String, gstPercent: Double,
        batchNo: String = "", expiryDate: String = "" // Added Batch & Expiry
    ) {
        generateDynamicDocNumber("INWARD") { documentNumber ->
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
            val baseCost = unitPrice + serviceCost
            val baseTotal = baseCost * qty
            
            var totalGst = 0.0
            if (isGstApplicable) totalGst = baseTotal * (gstPercent / 100)
            
            val landedPerUnit = if (qty > 0) (baseTotal + totalGst) / qty else 0.0

            val updates = mutableMapOf<String, Any?>()
            val txnId = getPathForModule("Transactions").push().key ?: return@generateDynamicDocNumber
            
            updates["Transactions/$txnId"] = mapOf(
                "documentNumber" to documentNumber, "itemId" to itemId, "qty" to qty, "landedCost" to landedPerUnit,
                "supplierName" to supplierName, "type" to "GRN_INWARD", "date" to dateStr, "timestamp" to timestamp, 
                "userId" to UserSession.userId, "batchNo" to batchNo, "expiryDate" to expiryDate
            )

            val ledgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@generateDynamicDocNumber
            updates["Inventory/StockLedger/$ledgerKey"] = mapOf(
                "itemId" to itemId, "change" to qty, "type" to "INWARD ($documentNumber)", 
                "unitCost" to landedPerUnit, "timestamp" to timestamp, "userId" to UserSession.userId,
                "batchNo" to batchNo, "expiryDate" to expiryDate
            )

            updates["Masters/Items/$itemId/costPerUnit"] = landedPerUnit
            bizPath.updateChildren(updates)
        }
    }

    fun completeProduction(
        finishedGoodId: String, finishedGoodName: String,
        yieldQty: Double, laborCost: Double, batchNumber: String,
        bomInputs: List<Map<String, Any>>,
        expiryDate: String = "" // Added Expiry for FG
    ) {
        generateDynamicDocNumber("PRODUCTION") { documentNumber ->
            val timestamp = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any?>()
            var totalMaterialCost = 0.0

            bomInputs.forEach { input ->
                val itemId = input["itemId"]?.toString() ?: ""
                val qty = input["qty"]?.toString()?.toDoubleOrNull() ?: 0.0
                val unitCost = input["unitCost"]?.toString()?.toDoubleOrNull() ?: 0.0
                totalMaterialCost += (qty * unitCost)
                
                val ledgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@forEach
                updates["Inventory/StockLedger/$ledgerKey"] = mapOf(
                    "itemId" to itemId, "change" to -qty, "type" to "MFG_CONSUMPTION ($documentNumber)", 
                    "timestamp" to timestamp, "userId" to UserSession.userId
                )
            }

            val fgUnitCost = if (yieldQty > 0) (totalMaterialCost + laborCost) / yieldQty else 0.0
            val fgLedgerKey = bizPath.child("Inventory/StockLedger").push().key ?: return@generateDynamicDocNumber
            updates["Inventory/StockLedger/$fgLedgerKey"] = mapOf(
                "itemId" to finishedGoodId, "change" to yieldQty, "type" to "MFG_YIELD ($documentNumber)", 
                "unitCost" to fgUnitCost, "timestamp" to timestamp, "userId" to UserSession.userId,
                "batchNo" to batchNumber, "expiryDate" to expiryDate
            )
            
            updates["Masters/Items/$finishedGoodId/costPerUnit"] = fgUnitCost
            
            val procId = getPathForModule("Processes").push().key ?: return@generateDynamicDocNumber
            updates["Processes/$procId"] = mapOf(
                "documentNumber" to documentNumber, "batchNumber" to batchNumber, 
                "fgUnitCost" to fgUnitCost, "timestamp" to timestamp, "userId" to UserSession.userId,
                "expiryDate" to expiryDate
            )

            bizPath.updateChildren(updates)
        }
    }

    // --- STOCK UTILS ---
    fun getItemStock(itemId: String, onResult: (Double) -> Unit) {
        getPathForModule("Inventory").orderByChild("itemId").equalTo(itemId).get().addOnSuccessListener { snapshot ->
            var balance = 0.0
            snapshot.children.forEach { child ->
                balance += (child.child("change").value?.toString()?.toDoubleOrNull() ?: 0.0)
            }
            onResult(balance)
        }
    }

    // --- GENERIC UTILS ---
    private fun generateDynamicDocNumber(docType: String, onGenerated: (String) -> Unit) {
        val configRef = bizPath.child("System/Settings/DocumentSequences/$docType")
        configRef.get().addOnSuccessListener { snap ->
            val prefix = snap.child("prefix").value?.toString() ?: docType.take(3).uppercase()
            val seq = snap.child("sequence").value?.toString()?.toLongOrNull() ?: 1L
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR) % 100
            
            val docNum = "$prefix/$year/${String.format(Locale.US, "%04d", seq)}"
            configRef.child("sequence").setValue(seq + 1)
            onGenerated(docNum)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getModuleData(module: String, onResult: (List<Pair<String, Map<String, Any?>>>) -> Unit) {
        getPathForModule(module).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val rawData = child.value as? Map<String, Any?>
                    if (rawData != null) child.key!! to rawData else null
                }
                onResult(items)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun saveItem(module: String, id: String?, data: Map<String, Any?>): String {
        val ref = getPathForModule(module)
        val key = id ?: ref.push().key ?: ""
        if (key.isEmpty()) return ""
        val finalData = data.toMutableMap()
        finalData["lastUpdatedBy"] = UserSession.userId
        finalData["lastUpdated"] = System.currentTimeMillis()
        ref.child(key).setValue(finalData)
        return key
    }

    fun updateStock(itemId: String, qtyChange: Double, type: String = "MANUAL_ADJUSTMENT") {
        val ledgerRef = bizPath.child("Inventory/StockLedger").push()
        ledgerRef.setValue(mapOf(
            "itemId" to itemId, "change" to qtyChange, "type" to type, 
            "timestamp" to System.currentTimeMillis(), "userId" to UserSession.userId
        ))
    }

    fun deleteItem(module: String, id: String) { getPathForModule(module).child(id).removeValue() }
    
    fun exportModuleToCSV(context: Context, module: String, data: List<Pair<String, Map<String, Any?>>>) {
        if (data.isEmpty()) return
        val headers = data.first().second.keys.joinToString(",")
        val rows = data.joinToString("\n") { item -> item.second.values.joinToString(",") { it?.toString() ?: "" } }
        val csvString = "$headers\n$rows"
        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, csvString); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, "Share $module Report"))
    }

    @Suppress("UNCHECKED_CAST")
    fun getDocumentConfig(docType: String, onResult: (Map<String, Any>) -> Unit) {
        bizPath.child("System/Settings/DocumentSequences/$docType").get().addOnSuccessListener { snap ->
            onResult(snap.value as? Map<String, Any> ?: emptyMap())
        }
    }

    fun saveDocumentConfig(docType: String, prefix: String, useYear: Boolean, useMonth: Boolean, sequence: Long) {
        bizPath.child("System/Settings/DocumentSequences/$docType").setValue(mapOf("prefix" to prefix, "useYear" to useYear, "useMonth" to useMonth, "sequence" to sequence))
    }
}
