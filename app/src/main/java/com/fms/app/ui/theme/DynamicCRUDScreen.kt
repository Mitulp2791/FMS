package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fms.app.data.ERPFieldSchema
import com.fms.app.data.FirebaseRepository

@Composable
fun DynamicCRUDScreen(moduleName: String) {
    // Dynamic State Management
    var schema by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var formData by remember { mutableStateOf<MutableMap<String, Any>>(mutableMapOf()) }
    var isSaving by remember { mutableStateOf(false) }

    // Load the schema definition for this specific module
    LaunchedEffect(moduleName) {
        ERPFieldSchema.getSchema(moduleName) { fetchedSchema ->
            schema = fetchedSchema
            // Initialize empty form state based on schema keys
            val initialData = mutableMapOf<String, Any>()
            fetchedSchema.keys.forEach { key -> initialData[key] = "" }
            formData = initialData
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Manage $moduleName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(schema.keys.toList().size) { index ->
                val key = schema.keys.toList()[index]
                val type = schema[key] ?: "String"

                OutlinedTextField(
                    value = formData[key]?.toString() ?: "",
                    onValueChange = { newValue ->
                        formData = formData.toMutableMap().apply { put(key, newValue) }
                    },
                    label = { Text("$key ($type)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                isSaving = true
                // Persist through the tenant-scoped repository
                FirebaseRepository.saveDynamicRecord(moduleName, null, formData) { success ->
                    isSaving = false
                    if (success) {
                        // Reset form
                        val resetData = mutableMapOf<String, Any>()
                        schema.keys.forEach { key -> resetData[key] = "" }
                        formData = resetData
                    }
                }
            },
            enabled = !isSaving
        ) {
            Text(if (isSaving) "Saving to Tenant Cloud..." else "Save to $moduleName")
        }
    }
}