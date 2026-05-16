package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            text = "Financial Performance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gross Profit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.grossProfit.value >= 0)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Gross Profit", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", viewModel.grossProfit.value)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Margin: ${String.format(Locale.US, "%.1f", viewModel.profitMargin.value)}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Revenue vs COGS breakdown
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Sales", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", viewModel.totalRevenue.value)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total COGS", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", viewModel.totalCogs.value)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Financial Summary", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ReportRow("Total Invoices", viewModel.invoices.size.toString())
                ReportRow(
                    "Avg. Ticket Size",
                    "₹${if(viewModel.invoices.isNotEmpty()) String.format(Locale.US, "%.2f", viewModel.totalRevenue.value/viewModel.invoices.size) else "0.00"}"
                )
                ReportRow(
                    "Inventory/Sales Ratio",
                    String.format(Locale.US, "%.2f", if(viewModel.totalRevenue.value > 0) viewModel.totalCogs.value/viewModel.totalRevenue.value else 0.0)
                )
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
