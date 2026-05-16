package com.fms.app.ui.theme

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitionMasterScreen(viewModel: ExhibitionMasterViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Search", "Exhibition Detail")

    LaunchedEffect(viewModel.isEntryStarted.value) {
        if (viewModel.isEntryStarted.value && selectedTab == 0) {
            selectedTab = 1
        }
        if (!viewModel.isEntryStarted.value) {
            selectedTab = 0
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exhibition Master", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.saveExhibition {
                        Toast.makeText(context, "Exhibition Saved Successfully", Toast.LENGTH_SHORT).show()
                        selectedTab = 0
                    }
                },
                enabled = viewModel.exhibitorName.value.isNotEmpty() && viewModel.isEntryStarted.value
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp)
            }
        }

        SecondaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                val isEnabled = index == 0 || viewModel.isEntryStarted.value
                Tab(
                    selected = selectedTab == index,
                    onClick = { if (isEnabled) selectedTab = index },
                    enabled = isEnabled,
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                when (selectedTab) {
                    0 -> ExhibitionSearchTab(viewModel)
                    1 -> ExhibitionDetailTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun ExhibitionSearchTab(viewModel: ExhibitionMasterViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search Exhibition", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = { viewModel.startNewEntry() }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Text("Add", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.filteredExhibitionList.value) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectExhibition(ex.first, ex.second) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.second["exhibitorName"]?.toString() ?: "", fontWeight = FontWeight.Bold)
                            Text("${ex.second["location"]} | ${ex.second["fromDate"]} to ${ex.second["toDate"]}", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteExhibition(ex.first) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExhibitionDetailTab(viewModel: ExhibitionMasterViewModel) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = viewModel.exhibitorName.value,
            onValueChange = { viewModel.exhibitorName.value = it },
            label = { Text("Exhibitor Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = viewModel.location.value,
            onValueChange = { viewModel.location.value = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = viewModel.fromDate.value,
                onValueChange = {},
                label = { Text("From Date") },
                modifier = Modifier.weight(1f).clickable {
                    DatePickerDialog(context, { _, y, m, d ->
                        viewModel.fromDate.value = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = LocalContentColor.current, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            OutlinedTextField(
                value = viewModel.toDate.value,
                onValueChange = {},
                label = { Text("To Date") },
                modifier = Modifier.weight(1f).clickable {
                    DatePickerDialog(context, { _, y, m, d ->
                        viewModel.toDate.value = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = LocalContentColor.current, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
