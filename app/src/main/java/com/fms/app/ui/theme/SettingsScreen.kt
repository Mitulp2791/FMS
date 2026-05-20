package com.fms.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fms.app.data.UserSession
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SettingsScreen() {
    var prefix by remember { mutableStateOf("DOC") }
    var useMonth by remember { mutableStateOf(true) }
    var useYear by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    val dbRef = FirebaseDatabase.getInstance()
        .getReference("Businesses/${UserSession.companyId ?: "UNAUTHORIZED_TENANT"}/System/Settings/DocumentSequences/BILLING")

    // Load existing settings
    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                prefix = snapshot.child("prefix").value?.toString() ?: "DOC"
                useMonth = snapshot.child("useMonth").value as? Boolean ?: true
                useYear = snapshot.child("useYear").value as? Boolean ?: true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Document Sequences", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("Document Prefix (e.g. INV)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Include Month in Sequence")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = useMonth, onCheckedChange = { useMonth = it })
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Include Year in Sequence")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = useYear, onCheckedChange = { useYear = it })
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                isLoading = true
                val settings = mapOf(
                    "prefix" to prefix,
                    "useMonth" to useMonth,
                    "useYear" to useYear
                )
                dbRef.updateChildren(settings).addOnCompleteListener {
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Saving..." else "Update Tenant Settings")
        }
    }
}