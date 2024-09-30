package com.harshkanjariya.autohome.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.pluto.Pluto
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

@Composable
fun DeviceDetailScreen(device: DeviceEntity, onRemovePrefs: () -> Unit) {
    // Track power state (1 for ON, 0 for OFF)
    var powerState by remember { mutableStateOf(0) }

    Column {

        Row {
            Column(Modifier.weight(1.0f)) {
                Text(text = "Device ID: ${device.id}")
                Text(text = "Device IP: ${device.ip}")
            }
            Button(onClick = onRemovePrefs) {
                Text(text = "Reset")
            }
        }

        // Power Button
        Button(onClick = {
            // Toggle power state between 1 and 0
            powerState = if (powerState == 0) 1 else 0

            // Send API call to device
            sendPowerControlRequest(device.ip, powerState)
        }) {
            Text(text = if (powerState == 0) "Turn ON" else "Turn OFF")
        }
    }
}

/**
 * Function to send API request to control the power state of the device.
 */
fun sendPowerControlRequest(deviceIp: String, powerState: Int) {
    val client = OkHttpClient.Builder()
        .addInterceptor(PlutoOkhttpInterceptor)
        .build()
    val url = "http://$deviceIp/control?state=$powerState"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response: Response ->
                if (response.isSuccessful) {
                    // Handle successful response
                    println("Power state changed successfully to $powerState")
                } else {
                    println("Failed to change power state: ${response.message}")
                }
            }
        } catch (e: IOException) {
            println("Error sending power control request: ${e.message}")
            Pluto.open()
        }
    }
}
