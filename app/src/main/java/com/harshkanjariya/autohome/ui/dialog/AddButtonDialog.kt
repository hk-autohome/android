package com.harshkanjariya.autohome.ui.dialog

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.harshkanjariya.autohome.db.entity.ButtonEntity


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddButtonDialog(
    existingButtons: List<ButtonEntity>,
    onDismiss: () -> Unit,
    onAddButton: (Int, String) -> Unit
) {
    var newName by remember { mutableStateOf("") } // For name input
    var selectedIndex by remember { mutableStateOf<Int?>(null) } // For number selection
    val context = LocalContext.current

    val existingNumbers = existingButtons.map { it.pinNumber }
    val allowedNumbers =
        listOf(2, 4, 13, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27, 32, 33)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Button") },
        text = {
            Column(Modifier.clipToBounds()) {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Button Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    allowedNumbers.forEachIndexed { index, _ ->
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = if (selectedIndex == index) null else index
                            },
                            enabled = !existingNumbers.contains(index),
                            label = { Text(index.toString()) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (selectedIndex == null) {
                        Toast.makeText(
                            context,
                            "Please select a valid button number",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onAddButton(selectedIndex!!, newName)
                        onDismiss()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
