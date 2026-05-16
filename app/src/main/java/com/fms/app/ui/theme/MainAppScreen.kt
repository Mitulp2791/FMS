package com.fms.app.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fms.app.data.UserSession

data class NavItem(val label: String, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var currentRoute by remember { mutableStateOf("Dashboard") }

    // Professional ERP navigation: Filter based on User Permissions
    val allNavItems = listOf(
        NavItem("Dashboard", "Dashboard"),
        NavItem("Items", "Masters"),
        NavItem("Accounts", "Accounts"),
        NavItem("Exhibitions", "Exhibitions"), // Added Exhibition Master
        NavItem("Inventory", "Inventory"),
        NavItem("Billing", "Billing"),
        NavItem("Production", "Production"),
        NavItem("Inward", "Inward"),
        NavItem("Settings", "Settings")
    )

    // ENFORCEMENT: Only show modules enabled for this Company and User Role
    val allowedNavItems = remember {
        allNavItems.filter { item ->
            if (UserSession.companyModules.isEmpty()) true 
            else UserSession.canAccessModule(item.route)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentRoute, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedNavItems.forEach { item ->
                        FilterChip(
                            selected = currentRoute == item.route,
                            onClick = { currentRoute = item.route },
                            label = { Text(item.label, maxLines = 1) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Crossfade(targetState = currentRoute, label = "ScreenSwitch") { screen ->
                when (screen) {
                    "Dashboard" -> DashboardScreen()
                    "Masters" -> ItemMasterScreen()
                    "Accounts" -> AccountMasterScreen()
                    "Exhibitions" -> ExhibitionMasterScreen() // Added Exhibition Screen
                    "Inventory" -> InventoryScreen()
                    "Billing" -> BillingScreen()
                    "Production" -> ProductionScreen()
                    "Inward" -> PurchaseInwardScreen()
                    "Settings" -> SettingsScreen()
                }
            }
        }
    }
}
