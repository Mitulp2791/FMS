package com.fms.app.data

import com.google.firebase.database.FirebaseDatabase

object FirebaseBootstrap {

    private val db = FirebaseDatabase.getInstance()

    // =========================
    // MAIN INIT FUNCTION
    // =========================
    fun initialize(uid: String, onDone: (Boolean) -> Unit) {

        val userRef = db.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { userSnap ->

            if (!userSnap.exists()) {
                onDone(false)
                return@addOnSuccessListener
            }

            // =========================
            // STEP 1: BASIC USER DATA
            // =========================
            val companyId = userSnap.child("companyId").value?.toString()
            val role = userSnap.child("role").value?.toString()

            if (companyId.isNullOrEmpty() || role.isNullOrEmpty()) {
                onDone(false)
                return@addOnSuccessListener
            }

            UserSession.userId = uid
            UserSession.companyId = companyId
            UserSession.role = role

            // =========================
            // STEP 2: LOAD COMPANY DATA
            // =========================
            val companyRef = db.getReference("companies").child(companyId)

            companyRef.get().addOnSuccessListener { companySnap ->

                val modulesMap = mutableMapOf<String, Boolean>()

                companySnap.child("modules").children.forEach {
                    modulesMap[it.key ?: ""] = it.value as? Boolean ?: false
                }

                UserSession.companyModules = modulesMap

                // =========================
                // STEP 3: LOAD ROLE PERMISSIONS
                // =========================
                val roleRef = db.getReference("roles").child(role)

                roleRef.get().addOnSuccessListener { roleSnap ->

                    val permissions = mutableMapOf<String, Any>()

                    roleSnap.children.forEach { moduleSnap ->
                        val moduleName = moduleSnap.key ?: return@forEach

                        val permMap = mutableMapOf<String, Boolean>()

                        moduleSnap.children.forEach { action ->
                            permMap[action.key ?: ""] = action.value as? Boolean ?: false
                        }

                        permissions[moduleName] = permMap
                    }

                    UserSession.permissions = permissions

                    // =========================
                    // STEP 4: LOAD GLOBAL CONFIG
                    // =========================
                    val globalRef = db.getReference("globalConfig").child("modules")

                    globalRef.get().addOnSuccessListener { globalSnap ->

                        val globalModules = mutableMapOf<String, Boolean>()

                        globalSnap.children.forEach {
                            globalModules[it.key ?: ""] = it.value as? Boolean ?: false
                        }

                        UserSession.globalModules = globalModules

                        // =========================
                        // DONE
                        // =========================
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

        }.addOnFailureListener {
            onDone(false)
        }
    }
}