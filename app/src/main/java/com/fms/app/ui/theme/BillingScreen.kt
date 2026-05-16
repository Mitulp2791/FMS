package com.fms.app.ui.theme

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

// Professional ERP Palette
private val PrimaryColor = Color(0xFF3F51B5) 
private val SecondaryColor = Color(0xFFD32F2F)
private val BgColor = Color(0xFFF1F5F9)
private val SurfaceColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: BillingViewModel = viewModel()) {
    val context = LocalContext.current
    val finishedGoods = viewModel.masterItems.filter { it.second["type"] == "FG" }
    var expandedCustomers by remember { mutableStateOf(false) }

    val baseTotal = viewModel.cart.sumOf { it.lineTotal }
    val g = if (viewModel.isGstApplicable.value) (viewModel.gstPercent.value.toDoubleOrNull() ?: 0.0) else 0.0
    val grandTotal = baseTotal + (baseTotal * (g / 100))

    val canSave = viewModel.selectedCustomerId.value.isNotEmpty() && 
                  viewModel.cart.any { it.itemId.isNotEmpty() && it.qtyDouble > 0 }

    // Ensure at least one line item exists for a fresh invoice
    LaunchedEffect(Unit) {
        if (viewModel.cart.isEmpty()) {
            viewModel.addBlankLine()
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = { Text("Sale Invoice", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    Button(
                        onClick = { 
                            viewModel.generateInvoice { 
                                Toast.makeText(context, "Invoice Saved Successfully", Toast.LENGTH_SHORT).show() 
                            } 
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = Color.Black
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceColor,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp) // Reduced padding
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GRAND TOTAL", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("₹${String.format(Locale.US, "%.2f", grandTotal)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PrimaryColor)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                // 1. CUSTOMER SECTION
                item {
                    MinimalBillingCard(title = "Customer Information") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedCustomers,
                                onExpandedChange = { expandedCustomers = !expandedCustomers }
                            ) {
                                OutlinedTextField(
                                    value = if (viewModel.selectedCustomerId.value.isEmpty()) "Select Account Type" else viewModel.saleTypeDisplay.value,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Account Name / Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomers) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                ExposedDropdownMenu(expanded = expandedCustomers, onDismissRequest = { expandedCustomers = false }) {
                                    DropdownMenuItem(text = { Text("Walk-in Customer") }, onClick = { 
                                        viewModel.selectedCustomerId.value = "WALKIN"
                                        viewModel.saleTypeDisplay.value = "Walk-in Customer"
                                        viewModel.selectedCustomerName.value = "" // Open for manual entry
                                        expandedCustomers = false 
                                    })
                                    DropdownMenuItem(text = { Text("Direct Customer") }, onClick = { 
                                        viewModel.selectedCustomerId.value = "DIRECT"
                                        viewModel.saleTypeDisplay.value = "Direct Customer"
                                        viewModel.selectedCustomerName.value = "" // Open for manual entry
                                        expandedCustomers = false 
                                    })
                                    // Active Exhibitions
                                    viewModel.getActiveExhibitions().forEach { exName ->
                                        DropdownMenuItem(text = { Text(exName) }, onClick = { 
                                            viewModel.selectedCustomerId.value = "EXHIBITION"
                                            viewModel.saleTypeDisplay.value = exName
                                            viewModel.selectedCustomerName.value = "" // Open for manual entry
                                            expandedCustomers = false 
                                        })
                                    }
                                    viewModel.customerItems.forEach { customer ->
                                        val name = customer.second["name"]?.toString() ?: "Unknown"
                                        DropdownMenuItem(text = { Text(name) }, onClick = { 
                                            viewModel.selectedCustomerId.value = customer.first
                                            viewModel.saleTypeDisplay.value = "Registered Customer"
                                            viewModel.selectedCustomerName.value = name
                                            expandedCustomers = false 
                                        })
                                    }
                                }
                            }
                            
                            // Always allow entering/editing the name if a type is selected
                            if (viewModel.selectedCustomerId.value.isNotEmpty()) {
                                OutlinedTextField(
                                    value = viewModel.selectedCustomerName.value, 
                                    onValueChange = { viewModel.selectedCustomerName.value = it }, 
                                    label = { Text("Billing Name") }, 
                                    placeholder = { Text("Enter Customer Name") },
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(6.dp), 
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                
                                OutlinedTextField(
                                    value = viewModel.customerMobile.value, 
                                    onValueChange = { viewModel.customerMobile.value = it }, 
                                    label = { Text("Contact No. (Optional)") }, 
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(6.dp), 
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                            }
                        }
                    }
                }

                // 2. INVOICE LINE ITEMS
                item { 
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Invoice Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryColor)
                        TextButton(
                            onClick = { viewModel.addBlankLine() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Line", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                itemsIndexed(viewModel.cart) { index, cartItem -> 
                    CompactInvoiceLine(cartItem, finishedGoods, viewModel, onRemove = { viewModel.removeFromCart(index) }) 
                }

                // 3. TAXATION & TOTALS
                if (viewModel.cart.any { it.itemId.isNotEmpty() }) {
                    item { 
                        MinimalBillingCard(title = "Taxation & Totals") { TaxAndBreakdown(viewModel) } 
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalBillingCard(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = SurfaceColor, shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PrimaryColor, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactInvoiceLine(
    item: CartItem, 
    products: List<Pair<String, Map<String, Any?>>>, 
    viewModel: BillingViewModel,
    onRemove: () -> Unit
) {
    var expandedProducts by remember { mutableStateOf(false) }
    val availableStock = viewModel.getAvailableStock(item.itemId)

    Surface(
        modifier = Modifier.fillMaxWidth(), 
        color = SurfaceColor, 
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Product Selection
                ExposedDropdownMenuBox(
                    expanded = expandedProducts,
                    onExpandedChange = { expandedProducts = !expandedProducts },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (item.name.isEmpty()) "Search Product..." else item.name,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProducts) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = BgColor.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    ExposedDropdownMenu(expanded = expandedProducts, onDismissRequest = { expandedProducts = false }) {
                        products.forEach { prod ->
                            val name = prod.second["name"]?.toString() ?: "Unknown"
                            DropdownMenuItem(text = { Text(name, fontSize = 13.sp) }, onClick = { 
                                item.itemId = prod.first
                                item.name = name
                                item.price = prod.second["sellingPrice"]?.toString() ?: ""
                                if (item.qty.isEmpty()) item.qty = "" // Ensure it's empty for typing
                                expandedProducts = false 
                            })
                        }
                    }
                }
                
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) { 
                    Icon(Icons.Default.Close, null, tint = SecondaryColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) 
                }
            }

            if (item.itemId.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PCS Field (Numeric Only)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pcs", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                        OutlinedTextField(
                            value = item.qty,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) item.qty = it },
                            placeholder = { Text("0", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true
                        )
                        Text("Stock: $availableStock", fontSize = 9.sp, color = if(availableStock <= 0) Color.Red else Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    }

                    // RATE Field
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rate", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                        OutlinedTextField(
                            value = item.price,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) item.price = it },
                            placeholder = { Text("0.0", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true
                        )
                    }

                    // LINE TOTAL (Amount)
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                        Text("Amount", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                        Text("₹${String.format(Locale.US, "%.2f", item.lineTotal)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaxAndBreakdown(viewModel: BillingViewModel) {
    val baseTotal = viewModel.cart.sumOf { it.lineTotal }
    val g = if (viewModel.isGstApplicable.value) (viewModel.gstPercent.value.toDoubleOrNull() ?: 0.0) else 0.0
    val totalGst = baseTotal * (g / 100)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sub-total", fontSize = 13.sp, color = Color.Gray)
            Text("₹${String.format(Locale.US, "%.2f", baseTotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = PrimaryColor)
            Spacer(Modifier.width(4.dp))
            Text("Apply GST", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Switch(checked = viewModel.isGstApplicable.value, onCheckedChange = { viewModel.isGstApplicable.value = it }, modifier = Modifier.scale(0.6f))
        }
        
        if (viewModel.isGstApplicable.value) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = viewModel.gstPercent.value, onValueChange = { viewModel.gstPercent.value = it }, label = { Text("GST %") }, modifier = Modifier.width(70.dp), shape = RoundedCornerShape(4.dp), textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                Row(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(BgColor).padding(2.dp)) {
                    val isLocal = viewModel.gstType.value == "CGST_SGST"
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if(isLocal) PrimaryColor else Color.Transparent).clickable { viewModel.gstType.value = "CGST_SGST" }, contentAlignment = Alignment.Center) { Text("Local", fontSize = 10.sp, color = if(isLocal) Color.White else Color.Gray, fontWeight = if(isLocal) FontWeight.Bold else FontWeight.Normal) }
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if(!isLocal) PrimaryColor else Color.Transparent).clickable { viewModel.gstType.value = "IGST" }, contentAlignment = Alignment.Center) { Text("IGST", fontSize = 10.sp, color = if(!isLocal) Color.White else Color.Gray, fontWeight = if(!isLocal) FontWeight.Bold else FontWeight.Normal) }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax Amount", fontSize = 13.sp, color = Color.Gray)
                Text("₹${String.format(Locale.US, "%.2f", totalGst)}", fontSize = 13.sp, color = SecondaryColor)
            }
        }
    }
}
