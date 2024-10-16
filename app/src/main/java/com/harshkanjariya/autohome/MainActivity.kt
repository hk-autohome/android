package com.harshkanjariya.autohome

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.harshkanjariya.autohome.api.Mqtt
import com.harshkanjariya.autohome.ui.components.MainDrawer
import com.harshkanjariya.autohome.ui.login.LoginActivity
import com.harshkanjariya.autohome.ui.screens.DeviceDetailScreen
import com.harshkanjariya.autohome.ui.screens.DevicesHome
import com.harshkanjariya.autohome.ui.screens.MainScreen
import com.harshkanjariya.autohome.ui.screens.SetupNewDevice
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import com.harshkanjariya.autohome.utils.DataStoreKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    private lateinit var googleSignInClient: GoogleSignInClient
    private var mqtt = Mqtt()
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    val networkCallback = getNetworkCallBack()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            redirectToLogin()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { preferences ->
                preferences[DataStoreKeys.TOKEN] ?: ""
            }.collect { token ->
                if (token.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        redirectToLogin()
                    }
                } else {
//                    mqtt.connect(this@MainActivity, "7802004735")
                    setContent { MainScreen() }
                }
            }
        }
    }

    private fun getNetworkCallBack(): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {    //when Wifi is on
                super.onAvailable(network)

                Toast.makeText(this@MainActivity, "Wifi is on!", Toast.LENGTH_SHORT).show()
            }

            override fun onLost(network: Network) {    //when Wifi 【turns off】
                super.onLost(network)

                Toast.makeText(this@MainActivity, "Wifi turns off!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getConnectivityManager() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {    //start monitoring when in the foreground
        super.onResume()

        getConnectivityManager().registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onPause() {    //stop monitoring when not fully visible
        super.onPause()

        getConnectivityManager().unregisterNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        mqtt.disconnect()
    }
}
