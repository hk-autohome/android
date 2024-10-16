package com.harshkanjariya.autohome

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
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.harshkanjariya.autohome.api.Mqtt
import com.harshkanjariya.autohome.ui.login.LoginActivity
import com.harshkanjariya.autohome.ui.screens.MainScreen
import com.harshkanjariya.autohome.utils.DataStoreKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    private var mqtt = Mqtt()
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }

        setupUI()
    }

    private fun setupUI() {
        val connectivityManager = getConnectivityManager()
        setContent {
            var gatewayIp by remember { mutableStateOf("") }
            var isAuthenticated by remember { mutableStateOf(false) }
            val networkCallback = remember {
                getNetworkCallBack { newGatewayIp ->
                    gatewayIp = newGatewayIp
                }
            }
            // Launch the coroutine for checking the token and authentication
            LaunchedEffect(Unit) {
                verifyAuthToken {
                    isAuthenticated = true
                }
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            }
            DisposableEffect(Unit) {
                onDispose {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            }

            if (isAuthenticated) {
                MainScreen(gatewayIp)
            } else {
                Text("Loading...")
            }
        }
    }

    private fun verifyAuthToken(then: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { preferences ->
                preferences[DataStoreKeys.TOKEN] ?: ""
            }.collect { token ->
                if (token.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        redirectToLogin()
                    }
                } else {
                    then()
                }
            }
        }
    }

    private fun getNetworkCallBack(onGatewayIpAvailable: (String) -> Unit): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val connectivityManager = getConnectivityManager()
                val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(network)

                // Extract the gateway IP address from LinkProperties
                linkProperties?.let {
                    Log.d("MainActivity", "Available routes: ${it.routes}")

                    val route = it.routes.find { route ->
                        route.gateway is Inet4Address && route.gateway?.hostAddress != "0.0.0.0"
                    }

                    val gatewayIp = (route?.gateway as? Inet4Address)?.hostAddress ?: ""
                    Log.d("MainActivity", "IPv4 Gateway IP: $gatewayIp")

                    onGatewayIpAvailable(gatewayIp) // Pass the updated gateway IP to the caller
                }
            }
        }
    }

    fun getConnectivityManager() =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqtt.disconnect()
    }
}
