package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Business Health", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // Key Performance Indicators (KPIs)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    modifier = Modifier.weight(1f),
                    title = "Sales Today",
                    value = "₹${String.format(Locale.US, "%.0f", viewModel.salesToday.value)}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                DashboardCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Bills",
                    value = "${viewModel.activeOrders.value}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        item {
            DashboardCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Total Inventory Valuation (Asset)",
                value = "₹${String.format(Locale.US, "%.2f", viewModel.totalStockValue.value)}",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        // Alerts Section
        item {
            Text("Critical Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (viewModel.lowStockItems.value > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Low Stock Warning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        Text("${viewModel.lowStockItems.value} items are below the safety threshold.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            item {
                Text("All stock levels are healthy.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DashboardCard(modifier: Modifier, title: String, value: String, containerColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}
