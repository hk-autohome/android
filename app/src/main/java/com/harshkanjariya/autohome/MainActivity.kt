package com.harshkanjariya.autohome

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harshkanjariya.autohome.ui.DeviceDetailScreen
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoNetworkPlugin
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
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Pluto.Installer(application)
            .addPlugin(PlutoNetworkPlugin())
            .install()

        setContent {
            AutoHomeTheme {
                val navController = rememberNavController()

                val sharedPreferences = getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
                val deviceId = sharedPreferences.getString("selected_device_id", null)
                val deviceIp = sharedPreferences.getString("selected_device_ip", null)

                LaunchedEffect(deviceIp) {
                    if (deviceId != null && deviceIp != null) {
                        navController.navigate("deviceDetail/$deviceId/$deviceIp") // Navigate to device detail if exists
                    }
                }

                NavHost(navController = navController, startDestination = "deviceFinder") {
                    composable("deviceFinder") {
                        DeviceFinder { device ->
                            storeDeviceInPreferences(device, this@MainActivity)
                            navController.navigate("deviceDetail/${device.id}/${device.ip}")
                        }
                    }
                    // Device Detail Screen
                    composable("deviceDetail/{deviceId}/{deviceIp}") { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                        val deviceIp = backStackEntry.arguments?.getString("deviceIp") ?: ""
                        DeviceDetailScreen(
                            Device(deviceId, deviceIp)
                        ) {
                            val sharedPreferences =
                                getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.remove("selected_device_id")
                            editor.remove("selected_device_ip")
                            editor.apply()

                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceFinder(onDeviceSelected: (Device) -> Unit) {
    var devices by remember { mutableStateOf(listOf<Device>()) }
    var isSearching by remember { mutableStateOf(false) }
    val baseUrl = "192.168.1."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            isSearching = true
            fetchDeviceIds { foundDevices ->
                devices = foundDevices.map { Device(it.first, "${baseUrl}${it.second}") }
                isSearching = false
            }
        }) {
            Text(text = "Find ESP32 Devices")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Text("Searching for devices...")
        } else {
            if (devices.isNotEmpty()) {
                Text("Found Devices:")
                devices.forEach { device ->
                    DeviceListItem(device) {
                        onDeviceSelected(device)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceListItem(device: Device, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(text = device.id)
        Divider()
    }
}

fun storeDeviceInPreferences(device: Device, context: Context) {
    val sharedPreferences = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("selected_device_id", device.id)
    editor.putString("selected_device_ip", device.ip)
    editor.apply()
}

fun fetchDeviceIds(onDeviceFound: (List<Pair<String, String>>) -> Unit) {
    val client = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .build()
    val baseUrl = "http://192.168.1."

    CoroutineScope(Dispatchers.IO).launch {
        val devices = mutableListOf<Pair<String, String>>()
        val requests = (1..254).map { i ->
            async {
                val url = "${baseUrl}$i/device_id"
                try {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response: Response ->
                        if (response.isSuccessful) {
                            response.body?.string()?.let { deviceId ->
                                synchronized(devices) {
                                    devices.add(Pair(deviceId, "" + i))
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
