package com.harshkanjariya.autohome.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.harshkanjariya.autohome.models.CalibrationData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    calibrationResult: List<CalibrationData>?,
    onRetry: () -> Unit,
    resultMessage: String
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Calibration Results") },
            text = {
                if (calibrationResult == null) {
                    // Loading state
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Calibrating...", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                } else {
                    // Show calibration table
                    Column {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IPin", modifier = Modifier.weight(1f))
                            Text("OPin", modifier = Modifier.weight(1f))
                            Text("minOff", modifier = Modifier.weight(1f))
                            Text("maxOff", modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        calibrationResult.forEach {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(it.inPin.toString(), modifier = Modifier.weight(1f))
                                Text(it.outPin.toString(), modifier = Modifier.weight(1f))
                                Text(it.minOff.toString(), modifier = Modifier.weight(1f))
                                Text(it.maxOff.toString(), modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Result: $resultMessage", color = Color.Black)

                        if (resultMessage != "Calibration successful") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry) {
                                Text("Calibrate Again")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (calibrationResult != null) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
