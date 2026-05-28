package com.fms.app.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fms.app.data.UserSession

/**
 * AppModuleItem: Enhanced with Icon and RBAC Module Key.
 */
data class AppModuleItem(
    val name: String,
    val icon: ImageVector,
    val moduleKey: String,
    val requiredRole: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onNavigateToModule: (String) -> Unit, 
    onLogout: () -> Unit,
    onExitImpersonation: (() -> Unit)? = null
) {
    // Handle back button to exit impersonation or logout instead of exiting app
    BackHandler {
        if (UserSession.isImpersonating && onExitImpersonation != null) {
            onExitImpersonation()
        } else {
            onLogout()
        }
    }

    val allModules = listOf(
        AppModuleItem("Sales & Billing", Icons.Default.ShoppingCart, "Billing"),
        AppModuleItem("Inventory", Icons.AutoMirrored.Filled.List, "Inventory"),
        AppModuleItem("Purchases", Icons.Default.AddCircle, "Purchases"),
        AppModuleItem("Item Master", Icons.Default.Build, "ItemMaster"),
        AppModuleItem("Account Master", Icons.Default.Person, "AccountMaster"),
        AppModuleItem("Reports", Icons.Default.Info, "Reports"),
        AppModuleItem("Settings", Icons.Default.Settings, "Settings"),
        AppModuleItem("User Management", Icons.Default.Lock, "AdminPanel", requiredRole = "Admin")
    )

    // Filter modules based on Tenant-specific RBAC permissions
    // Added keys to remember to ensure UI refreshes correctly during session changes
    val authorizedModules = remember(UserSession.companyId, UserSession.isImpersonating, UserSession.role) {
        allModules.filter { module ->
            UserSession.isMasterAdmin() || UserSession.hasPermission(module.moduleKey, "view")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FMS Console", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${UserSession.companyName ?: "Business"} (${UserSession.companyId})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    if (UserSession.isImpersonating) {
                        IconButton(onClick = { onExitImpersonation?.invoke() }) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings, 
                                contentDescription = "Exit Impersonation",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "SANDBOX",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // SaaS Banner: Displaying License Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subscription: ${UserSession.subscriptionTier}", fontWeight = FontWeight.Bold)
                        Text("User Role: ${UserSession.role}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (UserSession.isMasterAdmin()) {
                        Icon(Icons.Default.Star, contentDescription = "Master Access", tint = Color(0xFFFFD700))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(authorizedModules) { module ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable { onNavigateToModule(module.moduleKey) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = module.name,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
