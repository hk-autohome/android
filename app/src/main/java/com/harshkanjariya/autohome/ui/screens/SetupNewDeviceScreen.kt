package com.harshkanjariya.autohome.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.harshkanjariya.autohome.api.getDeviceSsidList
import com.harshkanjariya.autohome.api.getEspDeviceId
import com.harshkanjariya.autohome.api.updateEspWifiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Enum class for connection states
enum class ConnectionState {
    LOADING,
    CONNECTED,
    NOT_CONNECTED,
    RESTARTING,
}

// New component to handle the form for Wi-Fi SSID and Password, managing its own state
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConfigForm(wifiList: List<String>, onSubmit: (ssid: String, password: String) -> Unit) {
    var selectedSsid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember(wifiList) {
        mutableStateOf(wifiList.isNotEmpty())
    }

    Column {
        Row {
            Text(text = "Wi-Fi: ")
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedSsid,
                    onValueChange = { selectedSsid = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    wifiList.forEach { ssid ->
                        DropdownMenuItem(onClick = {
                            selectedSsid = ssid
                            expanded = false
                        }, text = {
                            Text(ssid)
                        })
                    }
                }
            }
        }
        Row {
            Text(text = "Password: ")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(onClick = {
            onSubmit(selectedSsid, password)
        }) {
            Text(text = "Submit")
        }
    }
}

@Composable
fun SetupNewDevice(gatewayIp: String) {
    val context = LocalContext.current
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiList = remember { mutableStateListOf<String>() }
    var deviceId: String? by remember { mutableStateOf(null) }
    var connectionState by remember { mutableStateOf(ConnectionState.LOADING) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val list = getDeviceSsidList(gatewayIp)
                wifiList.addAll(list)
            } catch (e: Exception) {
                Log.e("SetupNewDevice", "Error retrieving ssids", e)
            }
            try {
                deviceId = getEspDeviceId(gatewayIp)
                connectionState = ConnectionState.CONNECTED
            } catch (e: Exception) {
                Log.e("SetupNewDevice", "Error retrieving device ID", e)
                connectionState = ConnectionState.NOT_CONNECTED
            }
        }
    }

    // Update displayed text based on the connection state
    val displayText = when (connectionState) {
        ConnectionState.LOADING -> "Loading..."
        ConnectionState.CONNECTED -> "Connected to device: $deviceId"
        ConnectionState.NOT_CONNECTED -> "Not connected to any AutoHome device"
        ConnectionState.RESTARTING -> "Restarting Device..."
    }

    Column(Modifier.padding(8.dp)) {
        Text(displayText, style = MaterialTheme.typography.bodyMedium)

        if (connectionState == ConnectionState.CONNECTED) {
            DeviceConfigForm(wifiList = wifiList) { ssid, password ->
                coroutineScope.launch(Dispatchers.IO) {
                    updateEspWifiConfig(gatewayIp, ssid, password) {
                        connectionState = ConnectionState.RESTARTING
                    }
                }
            }
        }
    }
}
