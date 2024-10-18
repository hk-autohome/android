package com.harshkanjariya.autohome.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.harshkanjariya.autohome.api.checkDeviceStatus
import com.harshkanjariya.autohome.api.getEspDeviceInfo
import com.harshkanjariya.autohome.api.repositories.DeviceRepository

const val LIMIT = 10

@Composable
fun DevicesHome(context: Context, openFindDeviceActivity: () -> Unit, navigate: (String) -> Unit) {
    var devices by remember { mutableStateOf(listOf<DeviceEntity>()) }
    var refreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMoreData by remember { mutableStateOf(true) }

    // Load initial devices on launch
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            DeviceRepository.getDevices(page, LIMIT).let { newDevices ->
                devices = newDevices
                hasMoreData = newDevices.size == LIMIT
                page = if (hasMoreData) 2 else 1
            }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing),
        onRefresh = {
            refreshing = true
            page = 1
            coroutineScope.launch(Dispatchers.IO) {
                DeviceRepository.getDevices(page).let { newDevices ->
                    devices = newDevices
                    hasMoreData = newDevices.size == LIMIT
                    page = if (hasMoreData) 2 else 1
                    refreshing = false
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (devices.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(devices.size) { index ->
                        val device = devices[index]
                        DeviceListItem(device = device, context = context) {
                            navigate(device.deviceId)
                        }

                        if (index == devices.size - 1 && hasMoreData && !isLoading) {
                            isLoading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                DeviceRepository.getDevices(page + 1).let { newDevices ->
                                    if (newDevices.isNotEmpty()) {
                                        devices = devices + newDevices
                                        page += 1
                                    }
                                    hasMoreData = newDevices.isNotEmpty()
                                    isLoading = false
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            } else {
                Text(text = "No devices found", modifier = Modifier.align(Alignment.Center))
            }

            FloatingActionButton(
                onClick = openFindDeviceActivity,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
fun DeviceListItem(device: DeviceEntity, context: Context, onClick: () -> Unit) {
    var buttons by remember { mutableStateOf(listOf<ButtonEntity>()) }
    var deviceStatus by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(device.deviceId) {
        coroutineScope.launch(Dispatchers.IO) {
            buttons = DeviceRepository.getButtonsForDevice(device.deviceId, context)
            deviceStatus = try {
                getEspDeviceInfo(device.localIp)
                true
            } catch (error: Exception) {
                false
            }
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
