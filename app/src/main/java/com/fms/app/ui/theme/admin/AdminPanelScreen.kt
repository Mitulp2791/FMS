package com.fms.app.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AdminPanelScreen(viewModel: AdminPanelViewModel = viewModel()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Super Admin: Tenant Lifecycle Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.tenants) { tenant ->
                    val companyId = tenant["companyId"]?.toString() ?: ""
                    val isActive = tenant["isActive"] as? Boolean ?: true

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(tenant["name"]?.toString() ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                                Text("ID: $companyId", style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = { viewModel.toggleTenantStatus(companyId, isActive) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                            ) {
                                Text(if (isActive) "Suspend" else "Activate")
                            }
                        }
                    }
                }
            }
        }
    }
}