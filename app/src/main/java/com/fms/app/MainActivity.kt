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
import com.fms.app.ui.theme.BillingScreen
import com.fms.app.ui.theme.ItemMasterScreen
import com.fms.app.ui.theme.InventoryScreen
import com.fms.app.ui.theme.PurchaseInwardScreen
import com.fms.app.ui.theme.AccountMasterScreen
import com.fms.app.ui.theme.ReportsScreen
import com.fms.app.ui.theme.SettingsScreen
import com.fms.app.ui.theme.admin.AdminPanelScreen
import com.fms.app.ui.theme.admin.MasterAdminScreen

/**
 * MainActivity: The root navigation orchestrator for the FMS platform.
 * Dynamically mounts the correct operational module view based on touch events from the dashboard.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FMSTheme {
                // Top-Level Application State Handling
                var currentAppState by remember { mutableStateOf(AppState.LOGGED_OUT) }
                val loginViewModel: LoginViewModel = viewModel()

                // State tracker that intercepts dashboard grid clicks to mount feature views
                var activeModuleScreen by remember { mutableStateOf<String?>(null) }

                // Intercepts system/device back gestures to return cleanly to the main dashboard
                BackHandler(enabled = currentAppState == AppState.LOGGED_IN && activeModuleScreen != null) {
                    activeModuleScreen = null
                }

                // Monitors critical session changes to rebuild layout trees securely across tenants
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
                            // 1. SYSTEM Level Scope Control Panel (Platform Management)
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
                            // 2. Tenant Enterprise Scope Workspaces
                            else {
                                if (activeModuleScreen == null) {
                                    MainAppScreen(
                                        onLogout = {
                                            UserSession.clear()
                                            currentAppState = AppState.LOGGED_OUT
                                        },
                                        onNavigateToModule = { moduleKey ->
                                            // Handle reactive layout updates upon module item selections
                                            activeModuleScreen = moduleKey
                                        },
                                        onExitImpersonation = {
                                            UserSession.stopImpersonation()
                                            activeModuleScreen = null
                                        }
                                    )
                                } else {
                                    // Complete structural routing map for all grid components
                                    when (activeModuleScreen) {
                                        "Billing" -> BillingScreen()
                                        "ItemMaster" -> ItemMasterScreen()
                                        "Inventory" -> InventoryScreen()
                                        "Purchases" -> PurchaseInwardScreen()
                                        "AccountMaster" -> AccountMasterScreen()
                                        "Reports" -> ReportsScreen()
                                        "Settings" -> SettingsScreen()
                                        "AdminPanel" -> AdminPanelScreen()
                                        else -> {
                                            // Secure fallback view for unassigned module keys
                                            Scaffold(
                                                topBar = {
                                                    @OptIn(ExperimentalMaterial3Api::class)
                                                    TopAppBar(
                                                        title = { Text(text = "$activeModuleScreen Panel", fontWeight = FontWeight.Bold) },
                                                        navigationIcon = {
                                                            TextButton(onClick = { activeModuleScreen = null }) {
                                                                Text("< Dashboard")
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
                                                        text = "$activeModuleScreen module is active. Transactions are runtime isolated.",
                                                        style = MaterialTheme.typography.titleMedium
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