package com.fms.app.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object FirebaseBootstrap {
    private val db = FirebaseDatabase.getInstance()
    private const val TAG = "FMS_BOOTSTRAP"

    /**
     * Initializes the application session context safely across multi-tenant boundaries.
     * Uses a single-parameter Boolean callback to match the original architecture design perfectly.
     */
    fun initialize(uid: String, companyCode: String, onDone: (Boolean) -> Unit) {
        Log.d(TAG, "Starting secure session initialization loop for UID: $uid within Workspace: $companyCode")

        val operationalModules = listOf(
            "Billing", "Inventory", "Purchases", "ItemMaster",
            "AccountMaster", "Reports", "Settings", "AdminPanel",
            "Dashboard", "Exhibitions", "Masters", "Processes", "Transactions"
        )

        // Scenario A: Identity validation tracking for the platform Super Owner
        if (companyCode == "SYSTEM") {
            db.getReference("SYSTEM").child("users").child(uid).get()
                .addOnSuccessListener { masterSnap ->
                    if (masterSnap.exists() && masterSnap.child("role").value?.toString() == "MasterAdmin") {
                        UserSession.userId = uid
                        UserSession.role = "MasterAdmin"
                        UserSession.companyId = "SYSTEM"
                        UserSession.companyName = "Global Control Panel"
                        UserSession.subscriptionTier = "Enterprise"
                        UserSession.maxUsersAllowed = 999
                        UserSession.isAccountActive = true

                        val masterPerms = mutableMapOf<String, Map<String, Boolean>>()
                        operationalModules.forEach { module ->
                            masterPerms[module] = mapOf("read" to true, "write" to true, "view" to true)
                        }
                        UserSession.permissions = masterPerms
                        Log.d(TAG, "SUCCESS: Master Admin authorized under global SYSTEM tier space.")
                        onDone(true)
                    } else {
                        Log.e(TAG, "FAILED: Not a Master Admin or doesn't exist in SYSTEM")
                        onDone(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED: SYSTEM initialization exception error: ${e.message}")
                    onDone(false)
                }
            return
        }

        // Scenario B: Standard corporate tenant or Master Admin impersonation crossover
        // Query the isolated local company directory branch first to satisfy regular admin security rules
        db.getReference("companies").child(companyCode).child("users").child(uid).get()
            .addOnSuccessListener { userSnap ->
                if (userSnap.exists()) {
                    val role = userSnap.child("role").value?.toString() ?: "User"
                    val assignedCompanyId = userSnap.child("companyId").value?.toString() ?: companyCode

                    UserSession.userId = uid
                    UserSession.role = role
                    UserSession.companyId = assignedCompanyId
                    UserSession.companyName = "Active Tenant Workspace"
                    UserSession.subscriptionTier = "Pro"
                    UserSession.maxUsersAllowed = 5
                    UserSession.isAccountActive = true

                    val tenantPerms = mutableMapOf<String, Map<String, Boolean>>()
                    operationalModules.forEach { module ->
                        tenantPerms[module] = mapOf("read" to true, "write" to true, "view" to true)
                    }
                    UserSession.permissions = tenantPerms

                    Log.d(TAG, "SUCCESS: Standard company session initialized for tenant admin: $companyCode")
                    onDone(true)
                } else {
                    // Fallback: Check if a Master Admin is trying to cross-login directly into this company code
                    db.getReference("SYSTEM").child("users").child(uid).get()
                        .addOnSuccessListener { masterSnap ->
                            if (masterSnap.exists() && masterSnap.child("role").value?.toString() == "MasterAdmin") {
                                UserSession.userId = uid
                                UserSession.role = "MasterAdmin"
                                UserSession.companyId = companyCode
                                UserSession.companyName = "Impersonation Target Mode"
                                UserSession.subscriptionTier = "Enterprise"
                                UserSession.maxUsersAllowed = 999
                                UserSession.isAccountActive = true

                                val masterPerms = mutableMapOf<String, Map<String, Boolean>>()
                                operationalModules.forEach { module ->
                                    masterPerms[module] = mapOf("read" to true, "write" to true, "view" to true)
                                }
                                UserSession.permissions = masterPerms
                                Log.d(TAG, "SUCCESS: Master Admin cross-login authorized for target context: $companyCode")
                                onDone(true)
                            } else {
                                Log.e(TAG, "FAILED: User profile entry not found in company or SYSTEM nodes.")
                                onDone(false)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "FAILED: User not found in company, and SYSTEM read failed: ${e.message}")
                            onDone(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FAILED: Tenant verification lookup completely blocked: ${e.message}")
                onDone(false)
            }
    }
}