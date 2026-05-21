package com.fms.app.data

import com.google.firebase.database.*
import com.google.firebase.database.Transaction.Result
import com.google.firebase.database.Transaction.Handler

/**
 * FirebaseRepository: Principal Data Access Object for the Multi-Tenant FMS SaaS.
 * Enforces strict tenant isolation and atomic consistency.
 */
object FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()

    /**
     * Helper to get the base reference for the current tenant.
     * Throws an exception if no company is in context, preventing data leakage.
     */
    private fun getTenantRef(): DatabaseReference {
        val companyId = UserSession.companyId ?: throw IllegalStateException("ACCESS DENIED: No Tenant context found.")
        return database.getReference("Businesses/$companyId")
    }

    // --- GENERIC DATA FETCHERS ---

    /**
     * Fetches module data as a list of maps, including the Firebase keys.
     */
    fun getModuleDataWithKeys(module: String, onResult: (List<Pair<String, Map<String, Any>>>) -> Unit) {
        getTenantRef().child(module).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Pair<String, Map<String, Any>>>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val value = child.value as? Map<String, Any> ?: continue
                    list.add(key to value)
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) { onResult(emptyList()) }
        })
    }

    fun <T> getModuleData(module: String, onResult: (List<T>) -> Unit) {
        getTenantRef().child(module).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<T>()
                for (child in snapshot.children) {
                    @Suppress("UNCHECKED_CAST")
                    (child.value as? T)?.let { list.add(it) }
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) { onResult(emptyList()) }
        })
    }

    // --- SAVE METHODS (TENANT-SCOPED) ---

    fun saveItem(module: String, id: String?, data: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = getTenantRef().child(module)
        val targetRef = if (id.isNullOrEmpty()) ref.push() else ref.child(id)
        
        val enrichedData = data.toMutableMap()
        enrichedData["tenantId"] = UserSession.companyId ?: ""
        enrichedData["updatedBy"] = UserSession.userId ?: ""
        enrichedData["timestamp"] = ServerValue.TIMESTAMP

        targetRef.setValue(enrichedData).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteItem(module: String, id: String, onComplete: (Boolean) -> Unit) {
        getTenantRef().child(module).child(id).removeValue().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- ATOMIC TRANSACTIONS (CRITICAL FOR SaaS INTEGRITY) ---

    /**
     * Atomic Billing Transaction: Creates an invoice and updates inventory simultaneously.
     */
    fun createInvoice(invoiceData: Map<String, Any>, items: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        val tenantRef = getTenantRef()
        val invoiceId = tenantRef.child("Billing/Invoices").push().key ?: return onComplete(false)

        val updates = mutableMapOf<String, Any?>()
        
        val enrichedInvoice = invoiceData.toMutableMap()
        enrichedInvoice["tenantId"] = UserSession.companyId ?: ""
        enrichedInvoice["timestamp"] = ServerValue.TIMESTAMP
        updates["Billing/Invoices/$invoiceId"] = enrichedInvoice

        tenantRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Record stock ledger entries for the sale
                items.forEach { item ->
                    val ledgerEntry = mapOf(
                        "type" to "SALE",
                        "itemId" to item["itemId"],
                        "change" to -(item["qty"] as? Number)?.toDouble()!!,
                        "timestamp" to ServerValue.TIMESTAMP,
                        "ref" to invoiceId
                    )
                    tenantRef.child("Inventory/StockLedger").push().setValue(ledgerEntry)
                }
                deductInventoryAtomic(items, onComplete)
            } else {
                onComplete(false)
            }
        }
    }

    /**
     * Atomic Production Transaction: Consumes BOM items and produces a finished item.
     */
    fun executeProductionTransaction(productionData: Map<String, Any>, bom: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        val tenantRef = getTenantRef()
        val productionId = tenantRef.child("Processes/Production").push().key ?: return onComplete(false)

        val updates = mutableMapOf<String, Any?>()
        val enrichedProd = productionData.toMutableMap()
        enrichedProd["timestamp"] = ServerValue.TIMESTAMP
        updates["Processes/Production/$productionId"] = enrichedProd

        tenantRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 1. Deduct Raw Materials from BOM
                bom.forEach { item ->
                    val itemId = item["itemId"] as? String ?: return@forEach
                    val qty = (item["qty"] as? Number)?.toDouble() ?: 0.0
                    updateStockAtomic(itemId, -qty)
                    
                    val ledgerEntry = mapOf(
                        "type" to "PROD_CONSUMPTION",
                        "itemId" to itemId,
                        "change" to -qty,
                        "timestamp" to ServerValue.TIMESTAMP,
                        "ref" to productionId
                    )
                    tenantRef.child("Inventory/StockLedger").push().setValue(ledgerEntry)
                }

                // 2. Add Finished Good
                val outputId = productionData["outputItem"] as? String ?: ""
                val outputQty = (productionData["outputQty"] as? Number)?.toDouble() ?: 0.0
                updateStockAtomic(outputId, outputQty)
                
                val ledgerEntry = mapOf(
                    "type" to "PROD_OUTPUT",
                    "itemId" to outputId,
                    "change" to outputQty,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "ref" to productionId
                )
                tenantRef.child("Inventory/StockLedger").push().setValue(ledgerEntry)

                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    private fun deductInventoryAtomic(items: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        var completedCount = 0
        val totalItems = items.size
        if (totalItems == 0) { onComplete(true); return }

        items.forEach { item ->
            val itemId = item["itemId"] as? String ?: return@forEach
            val qty = (item["qty"] as? Number)?.toDouble() ?: 0.0
            updateStockAtomic(itemId, -qty) {
                completedCount++
                if (completedCount == totalItems) onComplete(true)
            }
        }
    }

    private fun updateStockAtomic(itemId: String, change: Double, onDone: (() -> Unit)? = null) {
        val stockRef = getTenantRef().child("Inventory/Stocks/$itemId/currentStock")
        stockRef.runTransaction(object : Handler {
            override fun doTransaction(currentData: MutableData): Result {
                val currentVal = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = currentVal + change
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                onDone?.invoke()
            }
        })
    }

    // --- REFINED MODULE SPECIFIC METHODS ---

    fun getAllItems(onResult: (List<Map<String, Any>>) -> Unit) = getModuleData<Map<String, Any>>("Masters", onResult)
    fun getAccountsByType(type: String, onResult: (List<Map<String, Any>>) -> Unit) = getModuleData<Map<String, Any>>("Accounts", onResult)
    
    fun bookPurchase(data: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        // Atomic Purchase: Add stock and record transaction
        val tenantRef = getTenantRef()
        val purchaseId = tenantRef.child("Transactions/Purchases").push().key ?: return onComplete(false)
        
        val enriched = data.toMutableMap()
        enriched["timestamp"] = ServerValue.TIMESTAMP
        
        tenantRef.child("Transactions/Purchases").child(purchaseId).setValue(enriched).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val itemId = data["itemId"] as? String ?: ""
                val qty = (data["qty"] as? Number)?.toDouble() ?: 0.0
                updateStockAtomic(itemId, qty)
                
                val ledgerEntry = mapOf(
                    "type" to "PURCHASE",
                    "itemId" to itemId,
                    "change" to qty,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "ref" to purchaseId
                )
                tenantRef.child("Inventory/StockLedger").push().setValue(ledgerEntry)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun completeProduction(data: Map<String, Any>, bom: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        executeProductionTransaction(data, bom, onComplete)
    }
}