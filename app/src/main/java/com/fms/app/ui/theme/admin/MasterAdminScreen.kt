package com.fms.app.ui.theme.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
    onNavigateToDashboard: () -> Unit,
    viewModel: MasterAdminViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var companyToEdit by remember { mutableStateOf<TenantCompany?>(null) }

    BackHandler {
        UserSession.clear()
        onNavigateHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FMS SaaS Super Admin", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { UserSession.clear(); onNavigateHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Company")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.companies) { company ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(company.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("ID: ${company.id} | Plan Tier: ${company.tier}", style = MaterialTheme.typography.bodySmall)
                                        Text("Max Users Allowed: ${company.maxUsers}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                    }

                                    IconButton(onClick = { companyToEdit = company }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modify Company Parameters", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.toggleCompanyStatus(company.id, company.isActive) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (company.isActive) Color(0xFFC62828) else Color(0xFF2E7D32)
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(if (company.isActive) "Suspend" else "Activate")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loginAsTenant(company.id) {
                                                onNavigateToDashboard()
                                            }
                                        }
                                    ) {
                                        Text("Enter Workspace")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCompanyDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { id, name, tier, maxUsers ->
                    viewModel.createCompany(id, name, tier, maxUsers)
                    showAddDialog = false
                }
            )
        }

        if (companyToEdit != null) {
            EditCompanyDialog(
                company = companyToEdit!!,
                onDismiss = { companyToEdit = null },
                onConfirm = { name, tier, maxUsers ->
                    viewModel.updateCompanyDetails(companyToEdit!!.id, name, tier, maxUsers)
                    companyToEdit = null
                }
            )
        }
    }
}

@Composable
fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf("Free") }
    var maxUsers by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Onboard New Company Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = id, onValueChange = { id = it.uppercase() }, label = { Text("Company ID (Unique Code)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxUsers, onValueChange = { maxUsers = it }, label = { Text("Max Users Allowed") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(4.dp))
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

@Composable
fun EditCompanyDialog(
    company: TenantCompany,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(company.name) }
    var tier by remember { mutableStateOf(company.tier) }
    var maxUsers by remember { mutableStateOf(company.maxUsers.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modify Plan Limits (ID: ${company.id})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxUsers, onValueChange = { maxUsers = it }, label = { Text("Max Users Limit (Expandable)") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(4.dp))
                Text("Subscription Tier Upgrade", style = MaterialTheme.typography.labelMedium)
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
            Button(onClick = { onConfirm(name, tier, maxUsers.toIntOrNull() ?: company.maxUsers) }) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}