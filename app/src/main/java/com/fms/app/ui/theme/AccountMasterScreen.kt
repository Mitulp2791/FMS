package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMasterScreen(
    viewModel: AccountMasterViewModel = viewModel(),
    onAccountSelected: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Search", "Account detail", "Bank details", 
        "ID proofs", "Statutory details"
    )

    // Automatically switch to first detail tab when entry starts
    LaunchedEffect(viewModel.isEntryStarted.value) {
        if (viewModel.isEntryStarted.value && selectedTab == 0) {
            selectedTab = 1
        }
        if (!viewModel.isEntryStarted.value) {
            selectedTab = 0
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // TOP TOOLBAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Account detail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { 
                    viewModel.saveAccount { id, name ->
                        Toast.makeText(context, "Account Saved Successfully", Toast.LENGTH_SHORT).show()
                        if (onAccountSelected != null) {
                            onAccountSelected(id, name)
                        } else {
                            selectedTab = 0 // Return to search after save
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
                enabled = viewModel.name.value.isNotEmpty() && viewModel.isEntryStarted.value
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { /* Copy Logic */ },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
                enabled = viewModel.isEntryStarted.value
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", fontSize = 12.sp)
            }
        }

        // TABS
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isEnabled = index == 0 || viewModel.isEntryStarted.value
                Tab(
                    selected = selectedTab == index,
                    onClick = { if (isEnabled) selectedTab = index },
                    enabled = isEnabled,
                    text = { 
                        Text(
                            title, 
                            fontSize = 11.sp, 
                            maxLines = 1,
                            color = if (isEnabled) Color.Unspecified else Color.Gray
                        ) 
                    }
                )
            }
        }

        // CONTENT
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                when (selectedTab) {
                    0 -> SearchTab(viewModel, onAccountSelected)
                    1 -> AccountDetailTab(viewModel)
                    2 -> BankDetailsTab(viewModel)
                    3 -> IdProofsTab(viewModel)
                    4 -> StatutoryDetailsTab(viewModel)
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        Text("Section: ${tabs[selectedTab]}", color = Color.LightGray) 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(viewModel: AccountMasterViewModel, onAccountSelected: ((String, String) -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by Name, Mobile or City", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Button(
                onClick = { viewModel.startNewEntry() },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add New", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(viewModel.filteredAccountList.value) { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            val id = account.first
                            val name = account.second["name"]?.toString() ?: ""
                            if (onAccountSelected != null) {
                                onAccountSelected(id, name)
                            } else {
                                viewModel.selectAccount(id, account.second) 
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(account.second["name"]?.toString() ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${account.second["accountType"] ?: ""} | ${account.second["city"] ?: ""} | ${account.second["mobile"] ?: ""}", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (onAccountSelected != null) {
                            IconButton(onClick = { viewModel.selectAccount(account.first, account.second) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { viewModel.deleteAccount(account.first) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailTab(viewModel: AccountMasterViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Name Row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = viewModel.id.value, onValueChange = {}, label = { Text("ID") }, modifier = Modifier.width(60.dp), readOnly = true, enabled = false)
            OutlinedTextField(value = viewModel.name.value, onValueChange = { viewModel.name.value = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = viewModel.alias.value, onValueChange = { viewModel.alias.value = it }, label = { Text("Alias") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = viewModel.contactPerson.value, onValueChange = { viewModel.contactPerson.value = it }, label = { Text("Contact person") }, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(value = viewModel.address.value, onValueChange = { viewModel.address.value = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ERPDropdown(label = "Area", value = viewModel.area.value, onValueChange = { viewModel.area.value = it }, items = listOf("Default"), modifier = Modifier.weight(1f))
            ERPDropdown(label = "District", value = viewModel.district.value, onValueChange = { viewModel.district.value = it }, items = listOf("Default"), modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ERPDropdown(label = "City / Village", value = viewModel.city.value, onValueChange = { viewModel.city.value = it }, items = listOf("MUMBAI", "SURAT", "PUNE"), modifier = Modifier.weight(1f))
            OutlinedTextField(value = viewModel.pincode.value, onValueChange = { viewModel.pincode.value = it }, label = { Text("Pincode") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        ERPDropdown(label = "State", value = viewModel.state.value, onValueChange = { viewModel.state.value = it }, items = listOf("MAHARASHTRA", "GUJARAT"), modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = viewModel.email.value, onValueChange = { viewModel.email.value = it }, label = { Text("E-mail") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = viewModel.fax.value, onValueChange = { viewModel.fax.value = it }, label = { Text("Fax") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = viewModel.phone.value, onValueChange = { viewModel.phone.value = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = viewModel.mobile.value, onValueChange = { viewModel.mobile.value = it }, label = { Text("Mobile") }, modifier = Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.isWhatsappSame.value, onCheckedChange = { 
                viewModel.isWhatsappSame.value = it
                if(it) viewModel.whatsappNo.value = viewModel.mobile.value
            })
            Text("WhatsApp same as mobile", fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = viewModel.whatsappNo.value, onValueChange = { viewModel.whatsappNo.value = it }, label = { Text("WhatsApp no.") }, modifier = Modifier.weight(1f))
        }

        ERPDropdown(label = "Account Type", value = viewModel.accountType.value, onValueChange = { viewModel.accountType.value = it }, items = viewModel.accountTypes, modifier = Modifier.fillMaxWidth())
        ERPDropdown(label = "Group", value = viewModel.group.value, onValueChange = { viewModel.group.value = it }, items = listOf("Sundry Debtors", "Sundry Creditors", "Expense"), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.reference.value, onValueChange = { viewModel.reference.value = it }, label = { Text("Reference") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun BankDetailsTab(viewModel: AccountMasterViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = viewModel.bankName.value, onValueChange = { viewModel.bankName.value = it }, label = { Text("Bank name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.branchName.value, onValueChange = { viewModel.branchName.value = it }, label = { Text("Branch name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.branchLocation.value, onValueChange = { viewModel.branchLocation.value = it }, label = { Text("Branch location") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.bankAccountNo.value, onValueChange = { viewModel.bankAccountNo.value = it }, label = { Text("Account no.") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.ifscCode.value, onValueChange = { viewModel.ifscCode.value = it }, label = { Text("IFSC code") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.micrCode.value, onValueChange = { viewModel.micrCode.value = it }, label = { Text("MICR code") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = viewModel.swiftCode.value, onValueChange = { viewModel.swiftCode.value = it }, label = { Text("SWIFT code") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun IdProofsTab(viewModel: AccountMasterViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val proofs = listOf(
            "PAN no." to viewModel.panNo,
            "Aadhaar no." to viewModel.aadhaarNo,
            "Passport no." to viewModel.passportNo,
            "GSTIN" to viewModel.gstin,
            "Voter ID" to viewModel.voterId,
            "Driving license" to viewModel.drivingLicense
        )
        proofs.forEach { (label, state) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(value = state.value, onValueChange = { state.value = it }, label = { Text(label) }, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(20.dp)) }
                Text("View", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {})
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(value = viewModel.dob.value, onValueChange = { viewModel.dob.value = it }, label = { Text("DOB") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = viewModel.annDate.value, onValueChange = { viewModel.annDate.value = it }, label = { Text("Ann. date") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = viewModel.dueDays.value, onValueChange = { viewModel.dueDays.value = it }, label = { Text("Due days") }, modifier = Modifier.width(120.dp))
    }
}

@Composable
fun StatutoryDetailsTab(viewModel: AccountMasterViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.isMsmeRegistered.value, onCheckedChange = { viewModel.isMsmeRegistered.value = it })
            Text("MSME registered", fontSize = 12.sp)
        }
        ERPDropdown("MSME category", viewModel.msmeCategory.value, { viewModel.msmeCategory.value = it }, listOf("Micro", "Small", "Medium"))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.isKyc.value, onCheckedChange = { viewModel.isKyc.value = it })
            Text("KYC", fontSize = 12.sp)
        }
        
        Text("TDS / TCS applicable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row {
            viewModel.tdsOptions.forEach { opt ->
                RadioButton(selected = viewModel.tdsTcsApplicable.value == opt, onClick = { viewModel.tdsTcsApplicable.value = opt })
                Text(opt, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
        ERPDropdown("Assesse Type", viewModel.assesseType.value, { viewModel.assesseType.value = it }, viewModel.assesseTypes)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ERPDropdown(label: String, value: String, onValueChange: (String) -> Unit, items: List<String>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
