package com.harshkanjariya.autohome.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.harshkanjariya.autohome.api.Mqtt
import com.harshkanjariya.autohome.ui.login.LoginActivity
import com.harshkanjariya.autohome.ui.screens.FindDevicesActivity
import com.harshkanjariya.autohome.ui.screens.MainScreen
import com.harshkanjariya.autohome.utils.collectAsStateWithLifecycleWithValue
import dagger.hilt.android.AndroidEntryPoint
import java.net.Inet4Address

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var mqtt = Mqtt()
    private val PERMISSION_REQUEST_CODE = 1

    private val viewModel: MainViewModel by viewModels()

    private val connectivityManager: ConnectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handlePermissions()

        setupUI()

        viewModel.verifyAuthToken({ email ->
            mqtt.connect(this, "android/$email")
        }) {
            redirectToLogin()
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallBack)
    }

    private fun handlePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupUI() {
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycleWithValue()

            if (state.isAuthenticated) {
                MainScreen(state, mqtt, onLogout = {
                    viewModel.logout {
                        redirectToLogin()
                    }
                }) {
                    startActivity(Intent(this@MainActivity, FindDevicesActivity::class.java))
                }
            } else {
                Text("Loading...")
            }
        }
    }

    private val networkCallBack = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(network)

            // Extract the gateway IP address from LinkProperties
            linkProperties?.let {
                val route = it.routes.find { route ->
                    route.gateway is Inet4Address && route.gateway?.hostAddress != "0.0.0.0"
                }

                val gatewayIp = (route?.gateway as? Inet4Address)?.hostAddress ?: ""
                Log.d("MainActivity", "IPv4 Gateway IP: $gatewayIp")

                viewModel.updateGatewayIp(gatewayIp)
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqtt.disconnect()
    }
}
