package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fms.app.data.ERPFieldSchema
import com.fms.app.data.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicCRUDScreen(module: String) {
    val items = remember { mutableStateListOf<Pair<String, Map<String, Any?>>>() }
    val schema = remember { mutableStateMapOf<String, String>() }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    val formData = remember { mutableStateMapOf<String, String>() }

    // Hardcoded options for Dropdowns
    val uomOptions = listOf("KG", "ML", "LT", "BT", "Pcs", "GM")
    val typeOptions = listOf("RM", "FG", "SV")

    LaunchedEffect(module) {
        FirebaseRepository.getModuleData(module) { data ->
            items.clear()
            items.addAll(data)
        }
        ERPFieldSchema.getSchema(module) { fieldMap ->
            schema.clear()
            schema.putAll(fieldMap)
        }
    }

    val filteredItems = if (searchQuery.isEmpty()) items
    else items.filter { it.second.values.any { v -> v?.toString()?.contains(searchQuery, true) == true } }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search $module...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                formData.clear()
                schema.keys.forEach { formData[it] = "" }
                editId = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) { Text("+ Add New to $module") }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No records found in $module.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filteredItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("ID: ${item.first.take(6)}", style = MaterialTheme.typography.labelSmall)
                                Row {
                                    TextButton(onClick = {
                                        formData.clear()
                                        schema.keys.forEach { key -> formData[key] = item.second[key]?.toString() ?: "" }
                                        editId = item.first
                                        showDialog = true
                                    }) { Text("Edit") }
                                    TextButton(onClick = { FirebaseRepository.deleteItem(module, item.first) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            item.second.forEach { (k, v) ->
                                Text("$k: $v", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(onClick = {
                    FirebaseRepository.saveItem(module, editId, formData.toMap())
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text(if (editId == null) "Add New Item" else "Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    schema.forEach { (fieldName, fieldHint) ->
                        when (fieldName) {
                            "uom" -> DropdownField("UOM", formData["uom"] ?: "", uomOptions) { formData["uom"] = it }
                            "type" -> DropdownField("Type", formData["type"] ?: "", typeOptions) { formData["type"] = it }
                            else -> {
                                OutlinedTextField(
                                    value = formData[fieldName] ?: "",
                                    onValueChange = { formData[fieldName] = it },
                                    label = { Text(fieldName.uppercase()) },
                                    placeholder = { Text(fieldHint) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, selectedValue: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}