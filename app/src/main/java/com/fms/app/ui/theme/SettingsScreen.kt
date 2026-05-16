package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fms.app.data.FirebaseRepository
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val docTypes = listOf("INWARD" to "Purchases & GRN", "PRODUCTION" to "Manufacturing Orders", "BILLING" to "Sales Invoices")

    var expanded by remember { mutableStateOf(false) }
    var selectedDocType by remember { mutableStateOf(docTypes[0]) }

    var prefix by remember { mutableStateOf("GRN") }
    var includeYear by remember { mutableStateOf(true) }
    var includeMonth by remember { mutableStateOf(true) }
    var currentSequence by remember { mutableStateOf("1") }

    // Fetch current settings when document type changes
    LaunchedEffect(selectedDocType) {
        FirebaseRepository.getDocumentConfig(selectedDocType.first) { config ->
            prefix = config["prefix"]?.toString() ?: selectedDocType.first.take(3)
            includeYear = config["useYear"] as? Boolean ?: true
            includeMonth = config["useMonth"] as? Boolean ?: true
            currentSequence = config["sequence"]?.toString() ?: "1"
        }
    }

    // Preview Generator Math
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR) % 100
    val fy = if (month >= 4) "$year-${year + 1}" else "${year - 1}-$year"
    val mm = String.format(java.util.Locale.US, "%02d", month)

    var preview = prefix
    if (includeYear) preview += "/$fy"
    if (includeMonth) preview += "/$mm"
    preview += "-${String.format(java.util.Locale.US, "%04d", currentSequence.toLongOrNull() ?: 1L)}"

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Document Sequencing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedDocType.second, onValueChange = {}, readOnly = true,
                label = { Text("Select Module to Configure") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                docTypes.forEach { type ->
                    DropdownMenuItem(text = { Text(type.second) }, onClick = { selectedDocType = type; expanded = false })
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = prefix, onValueChange = { prefix = it.uppercase() },
                    label = { Text("Document Prefix (e.g., PI, INV, MFG)") }, modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Financial Year (e.g., 26-27)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = includeYear, onCheckedChange = { includeYear = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Current Month (e.g., 05)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = includeMonth, onCheckedChange = { includeMonth = it })
                }

                OutlinedTextField(
                    value = currentSequence, onValueChange = { currentSequence = it },
                    label = { Text("Next Sequence Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Live Preview", style = MaterialTheme.typography.labelMedium)
                Text(preview, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick = {
                val seq = currentSequence.toLongOrNull() ?: 1L
                FirebaseRepository.saveDocumentConfig(selectedDocType.first, prefix, includeYear, includeMonth, seq)
                Toast.makeText(context, "${selectedDocType.second} Series Updated", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("Save Document Series")
        }
    }
}