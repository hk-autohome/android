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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DeviceDetailScreen(deviceId: String, context: Context) {
    val db = remember { AppDatabase.getDatabase(context) }
    var device by remember { mutableStateOf<DeviceEntity?>(null) }
    var buttons by remember { mutableStateOf(listOf<ButtonEntity>()) }
    var deviceStatus by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showNameDialog by remember { mutableStateOf(false) }

    // Load device details
    LaunchedEffect(deviceId) {
        coroutineScope.launch(Dispatchers.IO) {
            device = db.deviceDao().getDeviceById(deviceId)
            buttons = getButtonsForDevice(deviceId, context)
            // Check device status
            device?.let {
                deviceStatus = checkDeviceStatus(it.ip, it.id) {
                    errorMessage = it
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text(text = device?.name ?: "", style = MaterialTheme.typography.headlineSmall)
            Icon(Icons.Default.Edit, contentDescription = "Edit Button", Modifier.clickable {
                showNameDialog = true
            })
        }
        if (showNameDialog && device != null) {
            InputNameDialog(onDismiss = { showNameDialog = false }) {
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
                    Text(text = it.name, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "id: ${it.id}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "ip: ${it.id}", style = MaterialTheme.typography.bodyLarge)
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(10.dp)
                        .background(
                            color = if (deviceStatus) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons associated with the device
            buttons.forEach { button ->
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        callApi(it.ip, button.buttonNumber + 3, {
                            Log.e("TAG", "Button Pressed: $button")
                        }) {
                            errorMessage = it
                        }
                    }
                }, modifier = Modifier.padding(end = 4.dp)) {
                    Text("$button")
                }
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

@Composable
fun InputNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Enter Name") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    } else {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

