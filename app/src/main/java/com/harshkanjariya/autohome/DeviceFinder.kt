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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                onDeviceSelected = { deviceId, deviceIndex ->
                    val device = DeviceEntity(deviceId, deviceIndex, "")
                    addDeviceToDatabase(device, context)
                    onDismiss() // Close the dialog after adding the device
                },
                onCancel = {
                    onDismiss() // Close the dialog when cancel is pressed
                }
            )
        }
    }
}

@Composable
fun DeviceFinder(
    onDeviceSelected: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var devices by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var isSearching by remember { mutableStateOf(true) }  // Start searching by default
    val baseUrl = getLocalIpAddressBase()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Automatically start searching when the composable enters the composition
    LaunchedEffect(Unit) {
        searchJob?.cancel() // Cancel any previous search job

        searchJob = fetchDeviceIds(baseUrl) { foundDevice ->
            devices = devices + foundDevice // Add found devices to the list
        }
    }

    // Use a Box to position elements on top of each other
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Column for the list of devices found
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)  // Reserve space for the cancel button
        ) {
            if (isSearching) {
                Text("Searching for devices...", modifier = Modifier.padding(bottom = 16.dp))
            }

            // Display the list of found devices
            devices.forEach { (deviceId, index) ->
                Text(
                    text = "$deviceId ($index)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeviceSelected(deviceId, index)
                            searchJob?.cancel() // Stop searching when a device is selected
                        }
                        .padding(8.dp)
                )
                Divider()
            }
        }

        // Cancel button positioned at the bottom
        Button(
            onClick = {
                searchJob?.cancel() // Stop the search job
                onCancel() // Trigger the cancellation logic to close the dialog
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
        // Iterate over the last two octets of the IP address
        val requests = (0..255).flatMap { lastOctet1 ->
            (0..255).map { lastOctet2 ->
                async {
                    val url = "http://${baseUrl}${lastOctet1}.${lastOctet2}/device_id"
                    try {
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response: Response ->
                            if (response.isSuccessful) {
                                response.body?.string()?.let { deviceId ->
                                    // Notify of the found device
                                    synchronized(this) {
                                        onDeviceFound(Pair(deviceId, "${baseUrl}$lastOctet1.$lastOctet2")) // Store device ID with last two octets
                                    }
                                }
                            } else {
                                Log.e("DeviceFinder", "Request failed for $url: ${response.message}")
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("DeviceFinder", "fetchDeviceIds: Error fetching $url - ${e.message}")
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
                    val ip = inetAddress.hostAddress?.split(".")
                    return "${ip?.get(0)}.${ip?.get(1)}."
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
