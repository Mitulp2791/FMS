package com.fms.app.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminPanelScreen(
    onOpenModule: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(
            onClick = { onOpenModule("UOMs") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage UOMs")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onOpenModule("ProductTypes") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Product Types")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onOpenModule("ProductAttributes") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Product Attributes")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onOpenModule("Admins") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Admins")
        }
    }
}