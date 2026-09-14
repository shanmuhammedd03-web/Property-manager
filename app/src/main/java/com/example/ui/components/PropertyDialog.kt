package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Property

@Composable
fun PropertyDialog(
    propertyToEdit: Property? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(propertyToEdit?.name ?: "") }
    var note by remember { mutableStateOf(propertyToEdit?.note ?: "") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isNameError = hasAttemptedSubmit && name.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (propertyToEdit == null) "Add New Property" else "Edit Property",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the property name (e.g., Property A, Villa Sun) and optional details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Property Name *") },
                    placeholder = { Text("e.g., Property A") },
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text("Property name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("property_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Location (Optional)") },
                    placeholder = { Text("e.g. 2 Beds, Pool View") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("property_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), note.trim())
                    }
                },
                modifier = Modifier.testTag("save_property_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_property_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
