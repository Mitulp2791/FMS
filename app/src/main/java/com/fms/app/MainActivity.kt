package com.fms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession
import com.fms.app.ui.theme.FMSTheme
import com.fms.app.ui.theme.LoginScreen
import com.fms.app.ui.theme.LoginViewModel
import com.fms.app.ui.theme.MainAppScreen
import com.fms.app.ui.theme.admin.MasterAdminScreen
import com.fms.app.ui.theme.BillingScreen
import com.fms.app.ui.theme.ItemMasterScreen

/**
 * MainActivity: The root entry point for the FMS SaaS Application.
 * Orchestrates top-level navigation, responsive touch interactions, and submodule rendering.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FMSTheme {
                // Persistent State Management for the App Session
                var currentAppState by remember { mutableStateOf(AppState.LOGGED_OUT) }
                val loginViewModel: LoginViewModel = viewModel()

                // State configuration tracker to register active workspace layouts upon touch interaction events
                var activeModuleScreen by remember { mutableStateOf<String?>(null) }

                // Hardware or system swipe back gesture interception to exit a module cleanly to the dashboard
                BackHandler(enabled = currentAppState == AppState.LOGGED_IN && activeModuleScreen != null) {
                    activeModuleScreen = null
                }

                // Keying the navigation on critical session state ensures that the UI
                // completely refreshes (clearing internal remember blocks) when switching tenants or roles.
                key(UserSession.companyId, UserSession.isImpersonating, UserSession.role) {
                    when (currentAppState) {
                        AppState.LOGGED_OUT -> {
                            LoginScreen(
                                onLoginClick = { companyCode, email, password ->
                                    loginViewModel.performCompanySecureLogin(
                                        companyCode = companyCode,
                                        email = email,
                                        pass = password,
                                        onSuccess = { role ->
                                            activeModuleScreen = null
                                            currentAppState = AppState.LOGGED_IN
                                        }
                                    )
                                },
                                viewModel = loginViewModel
                            )
                        }
                        AppState.LOGGED_IN -> {
                            // 1. SYSTEM Tier Panel (Master Configuration Control)
                            if (UserSession.companyId == "SYSTEM" && UserSession.isMasterAdmin()) {
                                MasterAdminScreen(
                                    onNavigateHome = {
                                        UserSession.clear()
                                        currentAppState = AppState.LOGGED_OUT
                                    },
                                    onNavigateToDashboard = {
                                        activeModuleScreen = null
                                    }
                                )
                            }
                            // 2. Tenant Workspace (Company Context)
                            else {
                                if (activeModuleScreen == null) {
                                    MainAppScreen(
                                        onLogout = {
                                            UserSession.clear()
                                            currentAppState = AppState.LOGGED_OUT
                                        },
                                        onNavigateToModule = { moduleKey ->
                                            // Handle reactive navigation upon touching any grid component item
                                            activeModuleScreen = moduleKey
                                        },
                                        onExitImpersonation = {
                                            UserSession.stopImpersonation()
                                            activeModuleScreen = null
                                        }
                                    )
                                } else {
                                    // Dynamic module routing maps each unique module key string to its concrete screen component
                                    when (activeModuleScreen) {
                                        "Billing" -> {
                                            BillingScreen()
                                        }
                                        "ItemMaster" -> {
                                            ItemMasterScreen()
                                        }
                                        else -> {
                                            // Fallback layout configuration container for modules not yet wired to a dedicated file
                                            Scaffold(
                                                topBar = {
                                                    @OptIn(ExperimentalMaterial3Api::class)
                                                    TopAppBar(
                                                        title = { Text(text = "$activeModuleScreen Panel", fontWeight = FontWeight.Bold) },
                                                        navigationIcon = {
                                                            TextButton(onClick = { activeModuleScreen = null }) {
                                                                Text("< Back")
                                                            }
                                                        }
                                                    )
                                                }
                                            ) { paddingValues ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(paddingValues),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$activeModuleScreen Screen Content Placeholder",
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppState { LOGGED_OUT, LOGGED_IN }