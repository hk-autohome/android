package com.harshkanjariya.autohome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoNetworkPlugin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Pluto.Installer(application)
            .addPlugin(PlutoNetworkPlugin())
            .install()
        Pluto.showNotch(true)

        setContent {
            AutoHomeTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "devicesList") {
                    composable("devicesList") {
                        DevicesHome(this@MainActivity) {
                            navController.navigate("deviceDetails/$it")
                        }
                    }
                    composable("deviceDetails/{deviceId}") { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId")
                        DeviceDetailScreen(deviceId!!, context = this@MainActivity)
                    }
                }
            }
        }
    }
}
