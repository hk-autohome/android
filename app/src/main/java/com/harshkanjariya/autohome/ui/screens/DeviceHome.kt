package com.harshkanjariya.autohome.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.harshkanjariya.autohome.api.checkDeviceStatus

@Composable
fun DevicesHome(context: Context, navigate: (String) -> Unit) {
    val db = remember { AppDatabase.getDatabase(context) }
    var devices by remember { mutableStateOf(listOf<DeviceEntity>()) }
    var refreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showDeviceFinder by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            devices = db.deviceDao().getDevices()
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing),
        onRefresh = {
            refreshing = true
            coroutineScope.launch(Dispatchers.IO) {
                devices = db.deviceDao().getDevices()
                refreshing = false
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
//                    .clickable { mqtt.sendMessage("This is msg") }
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
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

            FloatingActionButton(
                onClick = { showDeviceFinder = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("+")
            }

            if (showDeviceFinder) {
                showDeviceFinderDialog(
                    onDismiss = {
                        showDeviceFinder = false
                        coroutineScope.launch(Dispatchers.IO) {
                            devices = db.deviceDao().getDevices()
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
    var deviceStatus by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(device.id) {
        coroutineScope.launch(Dispatchers.IO) {
            buttons = getButtonsForDevice(device.id, context)
            deviceStatus = checkDeviceStatus(device.ip, device.id) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = device.name, style = MaterialTheme.typography.bodyLarge)

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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
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
