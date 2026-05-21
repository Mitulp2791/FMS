package com.fms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession
import com.fms.app.ui.theme.FMSTheme
import com.fms.app.ui.theme.LoginScreen
import com.fms.app.ui.theme.LoginViewModel
import com.fms.app.ui.theme.MainAppScreen
import com.fms.app.ui.theme.admin.MasterAdminScreen

/**
 * MainActivity: The root entry point for the FMS SaaS Application.
 * Orchestrates the top-level navigation between Login, Master Admin, and Tenant Workspace.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FMSTheme {
                // Persistent State Management for the App Session
                var currentAppState by remember { mutableStateOf(AppState.LOGGED_OUT) }
                val loginViewModel: LoginViewModel = viewModel()

                when (currentAppState) {
                    AppState.LOGGED_OUT -> {
                        LoginScreen(
                            onLoginClick = { code, email, pass ->
                                loginViewModel.performCompanySecureLogin(code, email, pass) { role ->
                                    currentAppState = AppState.LOGGED_IN
                                }
                            }
                        )
                    }
                    AppState.LOGGED_IN -> {
                        // Multi-Tenant Navigation Router
                        // 1. Master Admin (Global Context)
                        if (UserSession.isMasterAdmin() && !UserSession.isImpersonating) {
                            MasterAdminScreen(
                                onNavigateHome = { 
                                    // Refresh state after potential impersonation start or logout
                                    currentAppState = AppState.LOGGED_OUT 
                                }
                            )
                        } 
                        // 2. Tenant Workspace (Company Context)
                        // This applies to regular Admins, Users, and Master Admins in 'Impersonation' mode
                        else {
                            MainAppScreen(
                                onLogout = { 
                                    UserSession.clear()
                                    currentAppState = AppState.LOGGED_OUT 
                                },
                                onNavigateToModule = { moduleKey ->
                                    // Navigation logic to specific ERP modules
                                    // E.g., navController.navigate(moduleKey)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Global App State definition moved to a common package or kept here for simplicity
enum class AppState { LOGGED_OUT, LOGGED_IN }
