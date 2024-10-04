package com.harshkanjariya.autohome

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.harshkanjariya.autohome.api.checkDeviceStatus
import com.harshkanjariya.autohome.api.triggerSwitch
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.ui.dialog.AddButtonDialog
import com.harshkanjariya.autohome.ui.dialog.NameChangeDialog
import com.pluto.Pluto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun DeviceDetailScreen(deviceId: String, context: Context) {
    val db = remember { AppDatabase.getDatabase(context) }
    var device by remember { mutableStateOf<DeviceEntity?>(null) }
    var buttons by remember { mutableStateOf(listOf<ButtonEntity>()) }
    var deviceStatus by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showNameDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var buttonToDelete by remember { mutableStateOf<ButtonEntity?>(null) }
    val defaultLEDButton = listOf(ButtonEntity(1L, "", 0, "LED"))

    LaunchedEffect(deviceId) {
        coroutineScope.launch(Dispatchers.IO) {
            device = db.deviceDao().getDeviceById(deviceId)
            buttons = getButtonsForDevice(deviceId, context) + defaultLEDButton
            device?.let {
                deviceStatus = checkDeviceStatus(it.ip, it.id) {
                    errorMessage = it
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(10.dp)
                    .background(
                        color = if (deviceStatus) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            Text(
                text = device?.name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Edit, contentDescription = "Edit Button", Modifier.clickable {
                showNameDialog = true
            })
        }
        if (showNameDialog && device != null) {
            NameChangeDialog(onDismiss = { showNameDialog = false }) {
                updateDeviceName(context, it, device?.id ?: "")
            }
        }

        device?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "id: ${it.id}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "ip: ${it.ip}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            buttons.forEach { button ->
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(button.name, modifier = Modifier.padding(end = 8.dp))

                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            triggerSwitch(it.ip, button.buttonNumber, it.password, {
                                Log.d("TAG", "Button Pressed: $button")
                            }) {
                                errorMessage = it
                            }
                        }
                    }, modifier = Modifier.padding(end = 4.dp)) {
                        Text("${button.buttonNumber}")
                    }

                    if (button.buttonNumber != 0) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete Button",
                            Modifier.clickable {
                                buttonToDelete = button
                                showConfirmDialog = true
                            })
                    }
                }
            }

            if (showAddDialog) {
                AddButtonDialog(
                    existingButtons = buttons,
                    onDismiss = { showAddDialog = false },
                    onAddButton = { newButton, name ->
                        coroutineScope.launch(Dispatchers.IO) {
                            addButtonForDevice(it.id, newButton, name, context)
                            buttons = getButtonsForDevice(it.id, context) + defaultLEDButton
                            showAddDialog = false
                        }
                    }
                )
            }

            if (showConfirmDialog && buttonToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text(text = "Confirm Delete") },
                    text = { Text(text = "Are you sure you want to delete this button?") },
                    confirmButton = {
                        TextButton(onClick = {
                            buttonToDelete?.let { button ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    removeButton(context, it.id, button.buttonNumber)
                                    buttons = getButtonsForDevice(it.id, context) + defaultLEDButton
                                    showConfirmDialog = false
                                }
                            }
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Button")
            }
        }

        errorMessage?.let {
            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun updateDeviceName(context: Context, name: String, id: String) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.deviceDao().updateName(name, id)
    }
}

fun removeButton(context: Context, deviceId: String, buttonNumber: Int) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.buttonDao().deleteButton(deviceId, buttonNumber)
    }
}