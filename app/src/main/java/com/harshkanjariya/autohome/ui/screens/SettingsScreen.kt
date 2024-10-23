package com.harshkanjariya.autohome.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.harshkanjariya.autohome.api.deleteAccountPermanently
import com.harshkanjariya.autohome.utils.DataStoreKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(dataStore: DataStore<Preferences>, onDelete: () -> Unit) {
    var showDeviceDetails by remember { mutableStateOf(false) }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }
    var showFinalConfirmationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        dataStore.data.collect {
            showDeviceDetails = it[DataStoreKeys.SETTINGS_SHOW_DEVICE_DETAILS] ?: false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Show Device Details Switch
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Show Device Details")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = showDeviceDetails,
                onCheckedChange = {
                    coroutineScope.launch(Dispatchers.IO) {
                        dataStore.edit { preferences ->
                            preferences[DataStoreKeys.SETTINGS_SHOW_DEVICE_DETAILS] = it
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Delete Account Button
        Button(
            onClick = { showDeleteWarningDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Delete Account Permanently",
                color = Color.White
            )
        }
    }

    // First warning dialog
    if (showDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWarningDialog = false },
            title = { Text("Warning") },
            text = {
                Text(
                    "This action cannot be undone. If you delete your account, " +
                            "you will lose all device data immediately, and if you re-create a new account, " +
                            "you'll have to add all the devices manually again."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteWarningDialog = false
                    showFinalConfirmationDialog = true
                }) {
                    Text("OK")
                }
            },
            dismissButton = null
        )
    }

    // Final confirmation dialog
    if (showFinalConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showFinalConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text(
                    "Are you sure you want to delete your account?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showFinalConfirmationDialog = false
                    deleteAccountPermanently(onDelete)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showFinalConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
