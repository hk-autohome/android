package com.harshkanjariya.autohome.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.harshkanjariya.autohome.api.getEspDeviceId

@Composable
fun SetupNewDevice() {
    val context = LocalContext.current
    var deviceId: String? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(Unit) {
        val gatewayIpAddress = getGatewayIpAddress(context)
        gatewayIpAddress?.let {
            deviceId = getEspDeviceId(it)
        } ?: run {
            Log.e("SetupNewDevice", "Failed to obtain Gateway IP address")
        }
    }

    Text(text = "setting up...$deviceId")
}

private fun getGatewayIpAddress(context: Context): String? {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val dhcpInfo = wifiManager.dhcpInfo
    val gatewayIpInt = dhcpInfo.gateway
    return if (gatewayIpInt != 0) {
        Formatter.formatIpAddress(gatewayIpInt)
    } else {
        null
    }
}
