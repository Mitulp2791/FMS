package com.fms.app.data

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot

/**
 * FirebaseBootstrap: Handles initial data synchronization and session hydration.
 * Strictly enforces SaaS architecture boundaries.
 */
object FirebaseBootstrap {

    private val db = FirebaseDatabase.getInstance()

    /**
     * Initializes the session for a user.
     * @param uid Firebase Auth UID
     * @param onDone Callback returning true if successful
     */
    fun initialize(uid: String, onDone: (Boolean) -> Unit) {
        // Step 1: Resolve User Identity and Base Role
        val userRef = db.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { userSnap ->
            if (!userSnap.exists()) {
                onDone(false)
                return@addOnSuccessListener
            }

            val role = userSnap.child("role").value?.toString() ?: "User"
            val assignedCompanyId = userSnap.child("companyId").value?.toString()

            // Global Master Admin check - Master Admins might not belong to a specific company initially
            if (role == "MasterAdmin") {
                UserSession.userId = uid
                UserSession.role = role
                UserSession.companyId = assignedCompanyId // Could be null for Master
                UserSession.isAccountActive = true
                onDone(true)
                return@addOnSuccessListener
            }

            if (assignedCompanyId.isNullOrEmpty()) {
                onDone(false)
                return@addOnSuccessListener
            }

            // Step 2: Validate Tenant Status and Subscription
            val companyRef = db.getReference("Companies").child(assignedCompanyId)
            companyRef.get().addOnSuccessListener { companySnap ->
                if (!companySnap.exists()) {
                    onDone(false)
                    return@addOnSuccessListener
                }

                val isActive = companySnap.child("isActive").getValue(Boolean::class.java) ?: false
                if (!isActive) {
                    onDone(false) // Account suspended
                    return@addOnSuccessListener
                }

                // Hydrate Session
                UserSession.userId = uid
                UserSession.role = role
                UserSession.companyId = assignedCompanyId
                UserSession.companyName = companySnap.child("name").value?.toString()
                UserSession.subscriptionTier = companySnap.child("tier").value?.toString() ?: "Free"
                UserSession.maxUsersAllowed = companySnap.child("maxUsers").getValue(Int::class.java) ?: 5
                UserSession.isAccountActive = true

                // Step 3: Load Role-Based Permissions from Tenant Configuration
                // Permissions are stored at: Businesses/{companyId}/config/roles/{role}
                val permissionsRef = db.getReference("Businesses/$assignedCompanyId/config/roles/$role")
                permissionsRef.get().addOnSuccessListener { permSnap ->
                    val permissions = mutableMapOf<String, Map<String, Boolean>>()
                    
                    permSnap.children.forEach { moduleSnap ->
                        val moduleName = moduleSnap.key ?: ""
                        val actions = mutableMapOf<String, Boolean>()
                        moduleSnap.children.forEach { actionSnap ->
                            actions[actionSnap.key ?: ""] = actionSnap.value as? Boolean ?: false
                        }
                        permissions[moduleName] = actions
                    }
                    
                    UserSession.permissions = permissions
                    onDone(true)
                }.addOnFailureListener {
                    onDone(false)
                }

            }.addOnFailureListener {
                onDone(false)
            }
        }.addOnFailureListener {
            onDone(false)
        }
    }
}