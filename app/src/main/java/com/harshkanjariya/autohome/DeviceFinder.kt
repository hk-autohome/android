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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            DeviceFinder { deviceId, deviceIndex ->
                val device = DeviceEntity(deviceId, deviceIndex.toString())
                addDeviceToDatabase(device, context)
                onDismiss()
            }
        }
    }
}

@Composable
fun DeviceFinder(onDeviceSelected: (String, String) -> Unit) {
    var devices by remember { mutableStateOf(listOf<Pair<String, Int>>()) }
    var isSearching by remember { mutableStateOf(false) }
    val baseUrl = getLocalIpAddressBase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            isSearching = true // Start searching
            fetchDeviceIds(baseUrl) { foundDevices ->
                devices = foundDevices // Update devices
                isSearching = false // Stop searching
            }
        }) {
            Text("Find Devices")
        }

        if (isSearching) {
            Text("Searching for devices...")
        }

        Spacer(modifier = Modifier.height(16.dp))

        devices.forEach { (deviceId, index) ->
            Text(
                text = deviceId + " (${baseUrl}${index})",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDeviceSelected(deviceId, baseUrl + index)
                    }
                    .padding(8.dp)
            )
            Divider()
        }
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
                    return "${ip?.get(0)}.${ip?.get(1)}.${ip?.get(2)}."
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DeviceFinder", "Error getting local IP address: ${e.message}")
    }
    return "192.168.1." // Default fallback if IP address cannot be found
}

fun fetchDeviceIds(baseUrl: String, onDeviceFound: (List<Pair<String, Int>>) -> Unit) {
    val client = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        val devices = mutableListOf<Pair<String, Int>>()
        val requests = (1..254).map { i ->
            async {
                val url = "http://${baseUrl}$i/device_id"
                try {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response: Response ->
                        if (response.isSuccessful) {
                            response.body?.string()?.let { deviceId ->
                                synchronized(devices) {
                                    devices.add(Pair(deviceId, i)) // Store device ID and index
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

        requests.awaitAll()

        withContext(Dispatchers.Main) {
            Log.e("TAG", "fetchDeviceIds: " + devices.size)
            onDeviceFound(devices)
        }
    }
}

fun addDeviceToDatabase(device: DeviceEntity, context: Context) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.deviceDao().insertDevice(device)
    }
}
