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
import com.harshkanjariya.autohome.api.Mqtt
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.ui.components.MainDrawer
import com.harshkanjariya.autohome.ui.main.MainContract
import com.harshkanjariya.autohome.ui.theme.AutoHomeTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    state: MainContract.State,
    mqtt: Mqtt,
    onLogout: () -> Unit,
    openFindDeviceActivity: () -> Unit
) {
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
                    onLogout = onLogout,
                    closeDrawer = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = "devicesList",
            ) {
                composable("devicesList") {
                    DevicesHome(context, openFindDeviceActivity) { device ->
                        navController.navigate("deviceDetails/${device.toJson()}")
                    }
                }
                composable("deviceDetails/{device}") { backStackEntry ->
                    val device = backStackEntry.arguments?.getString("device")
                    val parsedDevice = device?.let {
                        DeviceEntity.fromJson(it)
                    }
                    DeviceDetailScreen(parsedDevice!!, context = context, mqtt = mqtt)
                }
                composable("new_device") {
                    SetupNewDevice(state.gatewayIp)
                }
            }
        }
    }

}