package com.fms.app.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fms.app.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterAdminScreen(
    onNavigateHome: () -> Unit,
    viewModel: MasterAdminViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FMS SaaS Super Admin", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { UserSession.clear(); onNavigateHome() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Company")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            
            Text("Active Companies", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (viewModel.errorMessage != null) {
                Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.companies) { company ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (company.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(company.name, style = MaterialTheme.typography.headlineSmall)
                                    Text("ID: ${company.id} | Tier: ${company.tier}", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = company.isActive,
                                    onCheckedChange = { viewModel.toggleCompanyStatus(company.id, company.isActive) }
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { viewModel.loginAsTenant(company.id) { onNavigateHome() } },
                                    enabled = company.isActive
                                ) {
                                    Text("Login as Admin")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            OnboardCompanyDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { id, name, tier, users ->
                    viewModel.createCompany(id, name, tier, users)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun OnboardCompanyDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Int) -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf("Pro") }
    var maxUsers by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Onboard New Company") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = id, onValueChange = { id = it.uppercase() }, label = { Text("Company ID (Unique)") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") })
                OutlinedTextField(value = maxUsers, onValueChange = { maxUsers = it }, label = { Text("Max Users") })
                
                Text("Subscription Tier", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tier == "Free", onClick = { tier = "Free" })
                    Text("Free")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = tier == "Pro", onClick = { tier = "Pro" })
                    Text("Pro")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = tier == "Enterprise", onClick = { tier = "Enterprise" })
                    Text("Enterprise")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(id, name, tier, maxUsers.toIntOrNull() ?: 5) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}