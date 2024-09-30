package com.harshkanjariya.autohome

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Composable
fun DevicesHome(context: Context, reload: Boolean, modifier: Modifier) {
    val db = remember { AppDatabase.getDatabase(context) }
    var devices by remember { mutableStateOf(listOf<DeviceEntity>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        coroutineScope.launch(Dispatchers.IO) {
            devices = db.deviceDao().getDevices()
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Stored Devices", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isNotEmpty()) {
            devices.forEach { device ->
                DeviceListItem(device = device, context = context)
            }
        } else {
            Text(text = "No devices found")
        }
    }
}

@Composable
fun DeviceListItem(device: DeviceEntity, context: Context) {
    var buttonNumbers by remember { mutableStateOf(listOf<Int>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use LaunchedEffect to load buttons for the device
    LaunchedEffect(device.id) {
        coroutineScope.launch(Dispatchers.IO) {
            buttonNumbers = getButtonsForDevice(device.id, context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = device.id, style = MaterialTheme.typography.bodyLarge)

            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Button")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        callApi(device.ip, 2) {
                            Log.e("TAG", "DeviceListItem: $it")
                        }
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("LED")
            }

            buttonNumbers.forEach { number ->
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            callApi(device.ip, number + 3) {
                                Log.e("TAG", "DeviceListItem: $it")
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("$number")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (showAddDialog) {
            AddButtonDialog(
                deviceId = device.id,
                existingButtons = buttonNumbers,
                onDismiss = { showAddDialog = false },
                onAddButton = { newButton ->
                    // Add the new button to the database and update UI
                    coroutineScope.launch(Dispatchers.IO) {
                        addButtonForDevice(device.id, newButton, context)
                        buttonNumbers = getButtonsForDevice(device.id, context) // Reload buttons
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

fun callApi(deviceIp: String, pin: Int, onComplete: (String) -> Unit) {
    val url = "http://$deviceIp/control?pin=$pin"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val body = response.body?.toString()
        if (body != null) {
            onComplete(body)
        }
    }
}

@Composable
fun AddButtonDialog(
    deviceId: String,
    existingButtons: List<Int>,
    onDismiss: () -> Unit,
    onAddButton: (Int) -> Unit
) {
    var newButton by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Button") },
        text = {
            Column {
                TextField(
                    value = newButton,
                    onValueChange = { newButton = it },
                    label = { Text("Button Number") },
                    singleLine = true
                )
                existingButtons.forEach { button ->
                    Text("Existing Button: $button")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newButton.toIntOrNull()?.let { buttonNumber ->
                        onAddButton(buttonNumber)
                    } ?: Toast.makeText(context, "Invalid Button Number", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getButtonsForDevice(deviceId: String, context: Context): List<Int> {
    val db = AppDatabase.getDatabase(context)
    return db.buttonDao().getButtonsForDevice(deviceId)
}

fun addButtonForDevice(deviceId: String, button: Int, context: Context) {
    val db = AppDatabase.getDatabase(context)
    val currentButtons = db.buttonDao().getButtonsForDevice(deviceId).toMutableList()

    if (button !in currentButtons) {
        currentButtons.add(button)
        db.buttonDao().insertButton(ButtonEntity(deviceId = deviceId, buttonNumber = button))
    }
}

fun DeviceEntity.toJson(): String {
    return Gson().toJson(this)
}
