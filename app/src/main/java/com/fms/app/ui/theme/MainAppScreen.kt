package com.fms.app.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fms.app.data.UserSession

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val moduleKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var currentScreen by remember { mutableStateOf("Dashboard") }

    val allNavigationItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Home, "Dashboard"),
        NavigationItem("Billing (Sales)", Icons.Default.ShoppingCart, "Billing"),
        NavigationItem("Purchase Inward", Icons.Default.AddCircle, "Transactions"),
        NavigationItem("Inventory", Icons.Default.CheckCircle, "Inventory"),
        NavigationItem("Item Masters", Icons.Default.Build, "Masters"),
        NavigationItem("Account Ledger", Icons.Default.AccountBox, "Accounts"),
        NavigationItem("Production / Jobs", Icons.Default.Refresh, "Processes"),
        NavigationItem("Reports Center", Icons.AutoMirrored.Filled.List, "Reports"),
        NavigationItem("System Settings", Icons.Default.Settings, "Settings")
    )

    val authorizedNavigationItems = allNavigationItems.filter { item ->
        UserSession.hasAccessToModule(item.moduleKey)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FMS Enterprise Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tenant: ${UserSession.companyId ?: "Unknown"} | Scope: ${UserSession.role ?: "Staff"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentScreen == "Dashboard") {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Operational Command Center",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(authorizedNavigationItems) { navItem ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clickable { currentScreen = navItem.moduleKey }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = navItem.icon,
                                        contentDescription = navItem.title,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = navItem.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "Billing" -> BillingScreen()
                        "Transactions" -> PurchaseInwardScreen()
                        "Inventory" -> InventoryScreen()
                        "Masters" -> ItemMasterScreen()
                        "Accounts" -> AccountMasterScreen()
                        "Processes" -> ProductionScreen()
                        "Reports" -> ReportsScreen()
                        "Settings" -> SettingsScreen()
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Module Not Configured")
                                Button(onClick = { currentScreen = "Dashboard" }) {
                                    Text("Return to Hub")
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Button(
                            onClick = { currentScreen = "Dashboard" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hub")
                        }
                    }
                }
            }
        }
    }
}