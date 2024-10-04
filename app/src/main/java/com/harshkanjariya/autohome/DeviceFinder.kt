package com.harshkanjariya.autohome

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.ui.dialog.NameChangeDialog
import com.harshkanjariya.autohome.ui.dialog.PasswordValidatorDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

@Composable
fun showDeviceFinderDialog(onDismiss: () -> Unit, context: Context) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
        ) {
            DeviceFinder(
                onDeviceSelected = { device ->
                    addDeviceToDatabase(device, context)
                    onDismiss()
                },
                onCancel = {
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun DeviceFinder(
    onDeviceSelected: (DeviceEntity) -> Unit,
    onCancel: () -> Unit
) {
    var devices by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val isSearching by remember { mutableStateOf(true) }
    val baseUrl = getLocalIpAddressBase()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf(DeviceEntity("", "", "", "")) }

    LaunchedEffect(Unit) {
        searchJob?.cancel()

        searchJob = fetchDeviceIds(baseUrl) { foundDevice ->
            devices = devices + foundDevice
        }
    }

    if (showPasswordDialog) {
        PasswordValidatorDialog(
            deviceIp = selectedDevice.ip,
            onDismiss = { showPasswordDialog = false },
            onPasswordSubmit = {
                showNameDialog = true
                showPasswordDialog = false
                selectedDevice = selectedDevice.copy(password = it)
                searchJob?.cancel()
            }
        )
    }

    if (showNameDialog) {
        NameChangeDialog(
            onDismiss = { showNameDialog = false },
            onNameSubmit = { deviceName ->
                onDeviceSelected(
                    selectedDevice.copy(name = deviceName)
                )
                showNameDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)
        ) {
            if (isSearching) {
                Text("Searching for devices...", modifier = Modifier.padding(bottom = 16.dp))
            }

            devices.forEach { (deviceId, index) ->
                Text(
                    text = "$deviceId ($index)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPasswordDialog = true
                            selectedDevice = selectedDevice.copy(
                                id = deviceId,
                                ip = index
                            )
                        }
                        .padding(8.dp)
                )
                Divider()
            }
        }

        Button(
            onClick = {
                searchJob?.cancel()
                onCancel()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Cancel")
        }
    }
}

fun fetchDeviceIds(baseUrl: String, onDeviceFound: (Pair<String, String>) -> Unit): Job {
    val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    return CoroutineScope(Dispatchers.IO).launch {
        val requests = (0..255).flatMap { lastOctet1 ->
            (0..255).map { lastOctet2 ->
                async {
                    val url = "http://${baseUrl}${lastOctet1}.${lastOctet2}/device_id"
                    try {
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response: Response ->
                            if (response.isSuccessful) {
                                response.body?.string()?.let { deviceId ->
                                    synchronized(this) {
                                        onDeviceFound(
                                            Pair(
                                                deviceId,
                                                "${baseUrl}$lastOctet1.$lastOctet2"
                                            )
                                        )
                                    }
                                }
                            } else {
                                Log.e(
                                    "DeviceFinder",
                                    "Request failed for $url: ${response.message}"
                                )
                            }
                        }
                    } catch (_: IOException) {
                    }
                }
            }
        }

        requests.awaitAll()
    }
}

fun getLocalIpAddressBase(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue

            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val inetAddress = inetAddresses.nextElement()
                if (!inetAddress.isLoopbackAddress) {
                    // Get the first three octets of the IP address for base URL
                    val ip = inetAddress.hostAddress?.split(".") ?: return "192.168."
                    if (ip.size < 4) return "192.168."
                    return "${ip[0]}.${ip[1]}."
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DeviceFinder", "Error getting local IP address: ${e.message}")
    }
    return "192.168." // Default fallback if IP address cannot be found
}

fun addDeviceToDatabase(device: DeviceEntity, context: Context) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.deviceDao().insertDevice(device)
    }
}
