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
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun DevicesHome(context: Context, modifier: Modifier, navigate: (String) -> Unit) {
    val db = remember { AppDatabase.getDatabase(context) }
    var devices by remember { mutableStateOf(listOf<DeviceEntity>()) }
    var refreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showDeviceFinder by remember { mutableStateOf(false) }

    // Swipe-to-refresh implementation
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing),
        onRefresh = {
            refreshing = true
            coroutineScope.launch(Dispatchers.IO) {
                devices = db.deviceDao().getDevices() // Load devices again
                refreshing = false
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Stored Devices", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (devices.isNotEmpty()) {
                    devices.forEach { device ->
                        DeviceListItem(device = device, context = context) {
                            navigate(device.id)
                        }
                    }
                } else {
                    Text(text = "No devices found")
                }
            }

            // FloatingActionButton at the bottom right
            FloatingActionButton(
                onClick = { showDeviceFinder = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Aligns to the bottom right
                    .padding(16.dp) // Adds padding from the edges
            ) {
                Text("+")  // FloatingActionButton content
            }

            if (showDeviceFinder) {
                showDeviceFinderDialog(
                    onDismiss = {
                        showDeviceFinder = false
                        coroutineScope.launch(Dispatchers.IO) {
                            devices = db.deviceDao().getDevices() // Refresh devices after adding a new one
                        }
                    },
                    context = context
                )
            }
        }
    }
}


@Composable
fun DeviceListItem(device: DeviceEntity, context: Context, onClick: () -> Unit) {
    var buttons by remember { mutableStateOf(listOf<ButtonEntity>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceStatus by remember { mutableStateOf(false) } // State to hold the device's status
    var errorMessage by remember { mutableStateOf<String?>(null) } // State to store error messages
    val coroutineScope = rememberCoroutineScope()

    // Use LaunchedEffect to load buttons for the device
    LaunchedEffect(device.id) {
        coroutineScope.launch(Dispatchers.IO) {
            buttons = getButtonsForDevice(device.id, context)
            // Call API to check device status
            deviceStatus = checkDeviceStatus(device.ip, device.id) {
                errorMessage = it
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        errorMessage?.let {
            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = device.id, style = MaterialTheme.typography.bodyLarge)

                // Red/Green dot to indicate status
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

            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Button")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        callApi(device.ip, 2, {
                            Log.e("TAG", "DeviceListItem: $it")
                        }) {
                            errorMessage = it
                        }
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("LED")
            }

            buttons.forEach { button ->
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            callApi(device.ip, button.buttonNumber + 3, {
                                Log.e("TAG", "DeviceListItem: $it")
                            }) {
                                errorMessage = it
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("$button")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (showAddDialog) {
            AddButtonDialog(
                existingButtons = buttons,
                onDismiss = { showAddDialog = false },
                onAddButton = { newButton, name ->
                    // Add the new button to the database and update UI
                    coroutineScope.launch(Dispatchers.IO) {
                        addButtonForDevice(device.id, newButton, name, context)
                        buttons = getButtonsForDevice(device.id, context) // Reload buttons
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

fun callApi(deviceIp: String, pin: Int, onComplete: (String) -> Unit, onError: (String) -> Unit) {
    val url = "http://$deviceIp/control?pin=$pin"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onError("Error: ${response.code} ${response.message}")
                return
            }
            val body = response.body?.string() ?: ""
            onComplete(body)
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

fun checkDeviceStatus(deviceIp: String, deviceId: String, onError: (String) -> Unit): Boolean {
    val url = "http://$deviceIp/device_id"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onError("Error: ${response.code} ${response.message}")
                return false
            }
            val body = response.body?.string() ?: ""
            onError("")
            body == deviceId // Return true if the ID matches
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
        false
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddButtonDialog(
    existingButtons: List<ButtonEntity>,
    onDismiss: () -> Unit,
    onAddButton: (Int, String) -> Unit
) {
    var newName by remember { mutableStateOf("") } // For name input
    var selectedNumber by remember { mutableStateOf<Int?>(null) } // For number selection
    val context = LocalContext.current

    // Allowed button numbers: 1, 10, 13 to 30
    val allowedNumbers = listOf(1, 10) + (13..30).toList()

    var expanded by remember { mutableStateOf(false) } // State to control dropdown visibility

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Button") },
        text = {
            Column {
                // TextField for entering the button name
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Button Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown for selecting button number
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedNumber?.toString() ?: "Select Number",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Button Number") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allowedNumbers.forEach { number ->
                            DropdownMenuItem(onClick = {
                                selectedNumber = number
                                expanded = false
                            }, text = {
                                Text(text = number.toString())
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show existing buttons
                existingButtons.forEach { button ->
                    Text("Existing Button: $button")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (selectedNumber == null) {
                        Toast.makeText(context, "Please select a valid button number", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddButton(selectedNumber!!, newName) // Pass selected number and name
                        onDismiss()
                    }
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

fun getButtonsForDevice(deviceId: String, context: Context): List<ButtonEntity> {
    val db = AppDatabase.getDatabase(context)
    return db.buttonDao().getButtonsForDevice(deviceId)
}

fun addButtonForDevice(deviceId: String, button: Int, name: String, context: Context) {
    val db = AppDatabase.getDatabase(context)
    val currentButtons = db.buttonDao().getButtonsForDevice(deviceId).toMutableList()

    if (button !in currentButtons.map { it.buttonNumber }) {
        val entity = ButtonEntity(
            deviceId = deviceId,
            buttonNumber = button,
            name = name
        )
        currentButtons.add(entity)
        db.buttonDao().insertButton(entity)
    }
}
