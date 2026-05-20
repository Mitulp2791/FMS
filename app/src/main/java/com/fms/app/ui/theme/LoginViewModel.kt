package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun performCompanySecureLogin(
        companyCode: String,
        email: String,
        pass: String,
        onSuccess: (role: String) -> Unit
    ) {
        isLoading = true
        errorMessage = null

        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                FirebaseDatabase.getInstance()
                    .getReference("Users/$uid")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val serverCompanyId = snapshot.child("companyId").value?.toString() ?: ""
                        val serverRole = snapshot.child("role").value?.toString() ?: "Staff"

                        if (serverRole == "MasterAdmin") {
                            // Perfectly matched to your uploaded UserSession.kt
                            UserSession.userId = uid
                            UserSession.companyId = "SYSTEM"
                            UserSession.role = serverRole
                            isLoading = false
                            onSuccess(serverRole)
                        } else if (serverCompanyId.equals(companyCode, ignoreCase = true)) {
                            // Perfectly matched to your uploaded UserSession.kt
                            UserSession.userId = uid
                            UserSession.companyId = serverCompanyId
                            UserSession.role = serverRole
                            isLoading = false
                            onSuccess(serverRole)
                        } else {
                            auth.signOut()
                            errorMessage = "Invalid Company / Branch Code for this account."
                            isLoading = false
                        }
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        errorMessage = "Failed to retrieve user profile data."
                        isLoading = false
                    }
            }
            .addOnFailureListener { exception ->
                errorMessage = exception.localizedMessage ?: "Authentication failed."
                isLoading = false
            }
    }

    fun clearError() {
        errorMessage = null
    }
}