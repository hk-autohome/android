package com.harshkanjariya.autohome.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harshkanjariya.autohome.ui.components.MainDrawer
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(gatewayIp: String) {
    val context = LocalContext.current

    AutoHomeTheme {
        val navController = rememberNavController()
        val drawerState: DrawerState =
            rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MainDrawer(
                    navController = navController,
                    currentRoute = navController.currentDestination?.route ?: "",
                    closeDrawer = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        ) {
            Scaffold {
                NavHost(
                    navController = navController,
                    startDestination = "devicesList",
                    modifier = Modifier.padding(it)
                ) {
                    composable("devicesList") {
                        DevicesHome(context) {
                            navController.navigate("deviceDetails/$it")
                        }
                    }
                    composable("deviceDetails/{deviceId}") { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId")
                        DeviceDetailScreen(deviceId!!, context = context)
                    }
                    composable("new_device") {
                        SetupNewDevice(gatewayIp)
                    }
                }
            }
        }
    }

}