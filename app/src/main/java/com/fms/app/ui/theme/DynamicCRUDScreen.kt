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
import com.fms.app.data.UserSession

/**
 * DynamicCRUDScreen: A metadata-driven UI for managing various modules.
 * Strictly adheres to multi-tenant isolation by routing all saves through the FirebaseRepository.
 */
@Composable
fun DynamicCRUDScreen(moduleName: String) {
    // Dynamic State Management
    var schema by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var formData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

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
            text = "Module: $moduleName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Active Tenant: ${UserSession.companyId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (feedbackMessage != null) {
            Text(
                text = feedbackMessage!!,
                color = if (feedbackMessage!!.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            val keys = schema.keys.toList()
            items(keys.size) { index ->
                val key = keys[index]
                val type = schema[key] ?: "String"

                OutlinedTextField(
                    value = formData[key]?.toString() ?: "",
                    onValueChange = { newValue ->
                        val updated = formData.toMutableMap()
                        updated[key] = newValue
                        formData = updated
                    },
                    label = { Text("$key ($type)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
                if (isSaving) return@Button
                isSaving = true
                feedbackMessage = "Synchronizing with Cloud..."
                
                // Persist through the tenant-scoped repository
                // saveItem adds tenantId, timestamp, and updatedBy automatically
                FirebaseRepository.saveItem(moduleName, null, formData) { success ->
                    isSaving = false
                    if (success) {
                        feedbackMessage = "Success: Entry added to $moduleName"
                        // Reset form
                        val resetData = mutableMapOf<String, Any>()
                        schema.keys.forEach { key -> resetData[key] = "" }
                        formData = resetData
                    } else {
                        feedbackMessage = "Error: Permission Denied or Network Failure"
                    }
                }
            },
            enabled = !isSaving && formData.values.any { it.toString().isNotBlank() },
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Secure Save to Tenant Vault")
            }
        }
    }
}