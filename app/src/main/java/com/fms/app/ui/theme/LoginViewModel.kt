package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseBootstrap
import com.fms.app.data.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * LoginViewModel: Orchestrates the secure multi-tenant authentication flow.
 * Ensures that users are bound to their respective tenants and Master Admins have global reach.
 */
class LoginViewModel : ViewModel() {
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun clearError() {
        errorMessage = null
    }

    /**
     * Executes a secure login.
     * For SaaS: Validates Company ID, User Role, and Subscription Status.
     */
    fun performCompanySecureLogin(companyCode: String, email: String, pass: String, onSuccess: (String) -> Unit) {
        if (companyCode.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "Please fill all fields (Company Code, Email, and Password)"
            return
        }

        isLoading = true
        errorMessage = null

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                // Initialize session and verify tenant
                FirebaseBootstrap.initialize(uid) { success ->
                    if (success) {
                        // POST-INIT LOGIC:
                        // 1. If Master Admin, they can bypass the company check or impersonate the target code.
                        if (UserSession.role == "MasterAdmin") {
                            if (companyCode.isNotEmpty() && companyCode != "SYSTEM") {
                                // Master Admin is logging into a specific company context directly
                                UserSession.startImpersonation(companyCode)
                            }
                            isLoading = false
                            onSuccess(UserSession.role ?: "MasterAdmin")
                        } 
                        // 2. Regular User/Admin: Must match the companyCode they are logging into
                        else if (UserSession.companyId == companyCode) {
                            if (UserSession.isAccountActive) {
                                isLoading = false
                                onSuccess(UserSession.role ?: "User")
                            } else {
                                errorMessage = "This company account has been suspended. Please contact the Super Admin."
                                FirebaseAuth.getInstance().signOut()
                                UserSession.clear()
                                isLoading = false
                            }
                        } 
                        else {
                            errorMessage = "Access Denied: You do not belong to Company ID: $companyCode"
                            FirebaseAuth.getInstance().signOut()
                            UserSession.clear()
                            isLoading = false
                        }
                    } else {
                        errorMessage = "Login Failed: Invalid credentials or inactive account."
                        FirebaseAuth.getInstance().signOut()
                        UserSession.clear()
                        isLoading = false
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = e.localizedMessage ?: "Authentication Failed"
                isLoading = false
            }
    }
}