package com.harshkanjariya.autohome.ui.screens

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harshkanjariya.autohome.api.getEspDeviceInfo
import com.harshkanjariya.autohome.api.repositories.DeviceRepository
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.models.DiscoveredDevice
import com.harshkanjariya.autohome.ui.dialog.NameChangeDialog
import com.harshkanjariya.autohome.ui.dialog.PasswordValidatorDialog
import com.harshkanjariya.autohome.utils.collectAsStateWithLifecycleWithValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FindDevicesActivity : ComponentActivity() {

    private lateinit var nsdManager: NsdManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private val discoveredDevices: MutableStateFlow<List<DiscoveredDevice>> = MutableStateFlow(
        emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        setContent { FindDeviceScreen() }

        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("autoHomeMulticastLock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        initializeDiscoveryListener()
        discoverServices()
    }


    @Composable
    fun FindDeviceScreen() {
        val devices = discoveredDevices.collectAsStateWithLifecycleWithValue()
        var showPasswordDialog by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf(DiscoveredDevice("", "", false, 0, "")) }
        var showNameDialog by remember { mutableStateOf(false) }

        if (showPasswordDialog) {
            PasswordValidatorDialog(
                deviceIp = selectedDevice.ip,
                onDismiss = { showPasswordDialog = false },
                onPasswordSubmit = {
                    showNameDialog = true
                    showPasswordDialog = false
                    selectedDevice = selectedDevice.copy(password = it)
                }
            )
        }

        if (showNameDialog) {
            NameChangeDialog(
                onDismiss = { showNameDialog = false },
                onNameSubmit = { deviceName ->
                    DeviceRepository.addDevice(
                        DeviceEntity(
                            deviceId = selectedDevice.id,
                            localIp = selectedDevice.ip,
                            password = selectedDevice.password ?: "",
                            version = selectedDevice.version,
                            name = deviceName
                        ),
                    ) {
                        finish()
                    }
                    showNameDialog = false
                }
            )
        }
        LazyColumn(Modifier.padding(8.dp)) {
            items(devices.size) {
                FindDeviceListItem(device = devices[it]) {
                    selectedDevice = it
                    showPasswordDialog = true
                }
            }
        }
    }

    @Composable
    fun FindDeviceListItem(device: DiscoveredDevice, onClick: (device: DiscoveredDevice) -> Unit) {
        Row(
            Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable { onClick(device) }
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(text = "id: ${device.id}")
                Text(text = "ip: ${device.ip}")
            }
            Column {
                Text(text = "Version: ${device.version}")
            }
        }
    }

    private fun handleDeviceInfoLoading(ip: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val info = getEspDeviceInfo(ip)
            val device = DiscoveredDevice(
                ip = ip,
                id = info.deviceId,
                version = info.version,
                isRegistered = info.isRegistered,
                pinCode = info.pinCode,
            )
            discoveredDevices.value += device
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val host: String = serviceInfo.host.hostAddress?.let {
                if (it.startsWith("/"))
                    it.replace("/", "")
                else
                    it
            } ?: ""
            handleDeviceInfoLoading(host)
        }
    }
    private lateinit var discoveryListener: NsdManager.DiscoveryListener

    private fun initializeDiscoveryListener() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == "_http._tcp." && service.serviceName.contains("esp32")) {
                    nsdManager.resolveService(service, resolveListener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

    private fun discoverServices() {
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (multicastLock != null && multicastLock?.isHeld == true) {
            multicastLock?.release();
        }
    }
}