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
import androidx.room.Room
import com.harshkanjariya.autohome.db.AppDatabase
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

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "your-database-name"
        ).build()


        setContent {
            AutoHomeTheme {
                var showDeviceFinder by remember { mutableStateOf(false) }
                var reloadDevices by remember { mutableStateOf(false) }  // Flag to reload devices

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showDeviceFinder = true }) {
                            Text("+")  // FloatingActionButton content
                        }
                    }
                ) {
                    if (showDeviceFinder) {
                        showDeviceFinderDialog({
                            showDeviceFinder = false
                            reloadDevices = !reloadDevices
                        }, this@MainActivity)
                    }
                    DevicesHome(this@MainActivity, reloadDevices, Modifier.padding(it))
                }
            }
        }
    }
}
