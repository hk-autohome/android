package com.harshkanjariya.autohome.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.harshkanjariya.autohome.api.Mqtt
import com.harshkanjariya.autohome.api.dto.EspButtonTriggerResponseDto
import com.harshkanjariya.autohome.api.dto.MqttPayloadDto
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.harshkanjariya.autohome.db.AppDatabase
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.ui.dialog.AddButtonDialog
import com.harshkanjariya.autohome.ui.dialog.NameChangeDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.harshkanjariya.autohome.api.getEspDeviceInfo
import com.harshkanjariya.autohome.api.repositories.DeviceRepository
import com.harshkanjariya.autohome.api.triggerCalibration
import com.harshkanjariya.autohome.api.triggerSwitch
import com.harshkanjariya.autohome.models.CalibrationData
import com.harshkanjariya.autohome.ui.dialog.CalibrationDialog
import com.harshkanjariya.autohome.utils.getDefaultButtons
import com.harshkanjariya.autohome.utils.getPinIndex
import com.harshkanjariya.autohome.utils.getPinNumbers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailScreen(device: DeviceEntity, context: Context, mqtt: Mqtt) {
    var buttons by remember { mutableStateOf(getDefaultButtons()) }
    var isDeviceLocallyOnline by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showNameDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val buttonToDelete by remember { mutableStateOf<ButtonEntity?>(null) }
    var calibrationResult by remember { mutableStateOf<List<CalibrationData>?>(null) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            device.let {
                isDeviceLocallyOnline = try {
                    getEspDeviceInfo(it.localIp)
                    true
                } catch (exception: Exception) {
                    false
                }
            }
        }

        mqtt.subscribe("esp32/${device.deviceId}") { message ->
            val type = getResponseType<EspButtonTriggerResponseDto>()
            val data = Gson().fromJson<EspButtonTriggerResponseDto>(message, type)
            val pinIndex = getPinIndex(data.pin)
            buttons = buttons.map {
                it.copy(
                    on = if (it.pinNumber == pinIndex) data.on else it.on
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            mqtt.unsubscribe("esp32/${device.deviceId}")
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxHeight()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(10.dp)
                    .background(
                        color = if (isDeviceLocallyOnline) Color(0, 144, 0) else Color.Red,
                        shape = CircleShape
                    )
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Edit, contentDescription = "Edit Button", Modifier.clickable {
                showNameDialog = true
            })
        }
        if (showNameDialog) {
            NameChangeDialog(onDismiss = { showNameDialog = false }) {
                DeviceRepository.updateDevice(device.deviceId, it, emptyList())
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "id: ${device.deviceId}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "ip: ${device.localIp}", style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = {
                    showCalibrationDialog = true
                    calibrationResult = null
                    coroutineScope.launch(Dispatchers.IO) {
                        calibrationResult = triggerCalibration(device.localIp) ?: emptyList()
                    }
                },
                modifier = Modifier
                    .padding(end = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                )
            ) {
                Text("Calibrate")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buttons.forEach { button ->
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            updateDevicePin(
                                isDeviceLocallyOnline,
                                device,
                                button,
                                mqtt,
                            ) { newState ->
                                buttons = buttons.map {
                                    it.copy(
                                        on = if (it.pinNumber == button.pinNumber) newState else it.on
                                    )
                                }
                            }
                        }
                    }, modifier = Modifier
                        .padding(end = 4.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (button.on) Color(0, 144, 0) else Color.Gray,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (button.on) Color(0, 144, 0) else Color.White,
                        contentColor = if (button.on) Color.White else Color.Black,
                    )
                ) {
                    Text("${button.pinNumber}")
                }
            }
        }

        if (showAddDialog) {
            AddButtonDialog(
                existingButtons = buttons,
                onDismiss = { showAddDialog = false },
                onAddButton = { newButton, name ->
                    coroutineScope.launch(Dispatchers.IO) {
                        DeviceRepository.addButtonForDevice(
                            device.deviceId,
                            newButton,
                            name,
                            context
                        )
                        buttons = DeviceRepository.getButtonsForDevice(
                            device.deviceId,
                            context
                        )
                        showAddDialog = false
                    }
                }
            )
        }

        if (showConfirmDialog && buttonToDelete != null) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(text = "Confirm Delete") },
                text = { Text(text = "Are you sure you want to delete this button?") },
                confirmButton = {
                    TextButton(onClick = {
                        buttonToDelete?.let { button ->
                            coroutineScope.launch(Dispatchers.IO) {
                                removeButton(context, device.deviceId, button.pinNumber)
                                buttons = DeviceRepository.getButtonsForDevice(
                                    device.deviceId,
                                    context
                                )
                                showConfirmDialog = false
                            }
                        }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        CalibrationDialog(
            isVisible = showCalibrationDialog,
            onDismiss = { showCalibrationDialog = false },
            calibrationResult = calibrationResult,
            onRetry = {
                calibrationResult = null
                coroutineScope.launch(Dispatchers.IO) {
                    calibrationResult = triggerCalibration(device.localIp) ?: emptyList()
                }
            },
            resultMessage = "Calibration complete"
        )

        errorMessage?.let {
            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun updateDevicePin(
    isDeviceLocallyOnline: Boolean,
    device: DeviceEntity,
    button: ButtonEntity,
    mqtt: Mqtt,
    onButtonUpdate: (Boolean) -> Unit
) {
    if (isDeviceLocallyOnline) {
        try {
            val value = triggerSwitch(
                deviceIp = device.localIp,
                pin = button.pinNumber,
                password = device.password,
            ) {}
            if (value == null) {
                val payload = MqttPayloadDto(
                    id = device.deviceId,
                    pin = button.pinNumber,
                )
                mqtt.sendMessage("esp32/${device.pinCode}", payload)
                onButtonUpdate(!button.on)
            } else {
                onButtonUpdate(value)
            }
        } catch (e: Exception) {
            Log.e("TAG", "DeviceDetailScreen: $e")
        }
    } else {
        val payload = MqttPayloadDto(
            id = device.deviceId,
            pin = button.pinNumber,
        )
        mqtt.sendMessage("esp32/${device.pinCode}", payload)
        onButtonUpdate(!button.on)
    }
}

fun removeButton(context: Context, deviceId: String, buttonNumber: Int) {
    val db = AppDatabase.getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.buttonDao().deleteButton(buttonNumber)
    }
}