package com.fms.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fms.app.data.FirebaseBootstrap
import com.fms.app.data.UserSession
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginViewModel: Directs security context routing during user initialization.
 * Verifies structural boundaries and lets platform managers bypass tenant scope barriers.
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
     * Validates corporate user claims against real-time security nodes.
     */
    fun performCompanySecureLogin(companyCode: String, email: String, pass: String, onSuccess: (String) -> Unit) {
        if (companyCode.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "All fields (Company Code, Email, and Password) are mandatory."
            return
        }

        isLoading = true
        errorMessage = null

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""

                // Contextual single-parameter callback sequence loop matching FirebaseBootstrap signature exactly
                FirebaseBootstrap.initialize(uid, companyCode) { success ->
                    if (success) {
                        val resolvedRole = UserSession.role
                        if (resolvedRole == "MasterAdmin" || UserSession.companyId == companyCode) {
                            if (UserSession.isAccountActive) {
                                isLoading = false
                                onSuccess(resolvedRole ?: "User")
                            } else {
                                errorMessage = "Access Blocked: Workspace account is currently suspended."
                                FirebaseAuth.getInstance().signOut()
                                UserSession.clear()
                                isLoading = false
                            }
                        } else {
                            errorMessage = "Access Denied: Security assignment error for Company: $companyCode"
                            FirebaseAuth.getInstance().signOut()
                            UserSession.clear()
                            isLoading = false
                        }
                    } else {
                        errorMessage = "Login Failed: Check credentials, active status, or company code."
                        FirebaseAuth.getInstance().signOut()
                        UserSession.clear()
                        isLoading = false
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMessage = e.localizedMessage ?: "Authentication Handshake Aborted."
                isLoading = false
            }
    }
}