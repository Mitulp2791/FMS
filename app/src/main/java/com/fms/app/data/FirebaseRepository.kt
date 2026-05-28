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
        return database.getReference("companies/$companyId")
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
                    list.add(Pair(key, value))
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    fun saveItem(module: String, id: String?, data: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = if (id == null || id == "0") {
            getTenantRef().child(module).push()
        } else {
            getTenantRef().child(module).child(id)
        }
        ref.setValue(data).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteItem(module: String, id: String, onComplete: (Boolean) -> Unit) {
        getTenantRef().child(module).child(id).removeValue().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getAllItems(onResult: (List<Map<String, Any>>) -> Unit) {
        getModuleData("Masters/Items", onResult)
    }

    fun createInvoice(invoiceData: Map<String, Any>, itemsToDeduct: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        val tenantRef = getTenantRef()
        val invoiceId = tenantRef.child("Billing/Invoices").push().key ?: return onComplete(false)
        
        val updates = mutableMapOf<String, Any?>()
        updates["Billing/Invoices/$invoiceId"] = invoiceData
        
        tenantRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                itemsToDeduct.forEach { item ->
                    val itemId = item["itemId"] as? String ?: ""
                    val qty = (item["qty"] as? Number)?.toDouble() ?: 0.0
                    updateStockAtomic(itemId, -qty)
                }
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun saveInvoice(invoiceData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = getTenantRef().child("Billing").child("Invoices").push()
        ref.setValue(invoiceData).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getInvoices(onResult: (List<Map<String, Any>>) -> Unit) {
        getTenantRef().child("Billing").child("Invoices").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, Any>>()
                for (child in snapshot.children) {
                    @Suppress("UNCHECKED_CAST")
                    val item = child.value as? Map<String, Any> ?: continue
                    list.add(item)
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    fun saveStockMovement(movement: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = getTenantRef().child("Inventory").child("StockLedger").push()
        ref.setValue(movement).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getStockLedger(onResult: (List<Map<String, Any>>) -> Unit) {
        getTenantRef().child("Inventory").child("StockLedger").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, Any>>()
                for (child in snapshot.children) {
                    @Suppress("UNCHECKED_CAST")
                    val item = child.value as? Map<String, Any> ?: continue
                    list.add(item)
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    fun saveMasterItem(item: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val ref = getTenantRef().child("Masters").child("Items").push()
        ref.setValue(item).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getMasterItems(onResult: (List<Map<String, Any>>) -> Unit) = getModuleData("Masters/Items", onResult)
    fun getVendors(onResult: (List<Map<String, Any>>) -> Unit) = getModuleData("Masters/Vendors", onResult)
    fun getAccounts(onResult: (List<Map<String, Any>>) -> Unit) = getModuleData("Masters/Accounts", onResult)

    fun <T> getModuleData(path: String, onResult: (List<T>) -> Unit) {
        getTenantRef().child(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<T>()
                for (child in snapshot.children) {
                    @Suppress("UNCHECKED_CAST")
                    val item = child.value as? T ?: continue
                    list.add(item)
                }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

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

    private fun updateStockAtomic(itemId: String, changeQty: Double) {
        val stockRef = getTenantRef().child("Inventory").child("CurrentStock").child(itemId)
        stockRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentStock = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = currentStock + changeQty
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }

    private fun executeProductionTransaction(prodData: Map<String, Any>, bom: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        val tenantRef = getTenantRef()
        val processId = tenantRef.child("Processes").push().key ?: return onComplete(false)

        val enrichedProd = prodData.toMutableMap()
        enrichedProd["timestamp"] = ServerValue.TIMESTAMP

        tenantRef.child("Processes").child(processId).setValue(enrichedProd).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val outputItemId = prodData["outputItemId"] as? String ?: ""
                val outputQty = (prodData["outputQty"] as? Number)?.toDouble() ?: 0.0
                updateStockAtomic(outputItemId, outputQty)

                bom.forEach { material ->
                    val rmId = material["itemId"] as? String ?: ""
                    val rmQty = (material["qty"] as? Number)?.toDouble() ?: 0.0
                    updateStockAtomic(rmId, -rmQty)
                }
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }
}
