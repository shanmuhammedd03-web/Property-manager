package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingItem
import com.example.data.model.Property
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.ui.theme.UnpaidOrange
import com.example.ui.theme.UnpaidOrangeContainer
import com.example.util.DateTimeUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingDialog(
    properties: List<Property>,
    bookingToEdit: BookingItem? = null,
    initialPropertyId: Long? = null,
    initialDate: String? = null,
    initialStartTime: String? = null,
    initialEndTime: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        bookingId: Long,
        propertyId: Long,
        customerName: String,
        date: String,
        startTime: String,
        endTime: String,
        amount: Double,
        isPaid: Boolean
    ) -> Unit
) {
    var selectedPropertyId by remember {
        mutableStateOf(
            bookingToEdit?.propertyId
                ?: initialPropertyId
                ?: properties.firstOrNull()?.id
                ?: 0L
        )
    }
    var customerName by remember { mutableStateOf(bookingToEdit?.customerName ?: "") }
    var bookingDate by remember {
        mutableStateOf(
            bookingToEdit?.bookingDate
                ?: initialDate
                ?: DateTimeUtils.getTodayDate()
        )
    }
    var startTime by remember {
        mutableStateOf(
            bookingToEdit?.startTime
                ?: initialStartTime
                ?: "10:00"
        )
    }
    var endTime by remember {
        mutableStateOf(
            bookingToEdit?.endTime
                ?: initialEndTime
                ?: "12:00"
        )
    }
    var amountText by remember {
        mutableStateOf(
            bookingToEdit?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "100.00"
        )
    }
    var isPaid by remember { mutableStateOf(bookingToEdit?.isPaid ?: false) }

    var propertyDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // If properties were not loaded yet on open, set when loaded
    LaunchedEffect(properties) {
        if (selectedPropertyId == 0L && properties.isNotEmpty()) {
            selectedPropertyId = properties.first().id
        }
    }

    val selectedProperty = properties.find { it.id == selectedPropertyId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (bookingToEdit == null) "New Booking" else "Edit Booking",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (properties.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Please add at least one property first before creating a booking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 1. Property Selection Dropdown
                Text(
                    text = "Select Property *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = propertyDropdownExpanded,
                    onExpandedChange = { propertyDropdownExpanded = !propertyDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProperty?.name ?: "No Property Selected",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propertyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("booking_property_selector"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = propertyDropdownExpanded,
                        onDismissRequest = { propertyDropdownExpanded = false }
                    ) {
                        properties.forEach { prop ->
                            DropdownMenuItem(
                                text = { Text(prop.name) },
                                onClick = {
                                    selectedPropertyId = prop.id
                                    propertyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        validationError = null
                    },
                    label = { Text("Customer Name *") },
                    placeholder = { Text("e.g., John Doe") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_customer_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Date Selection
                Text(
                    text = "Date (YYYY-MM-DD) *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = bookingDate,
                    onValueChange = {
                        bookingDate = it.trim()
                        validationError = null
                    },
                    label = { Text("Booking Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick date chips
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = DateTimeUtils.getTodayDate()
                    val tomorrow = DateTimeUtils.getTomorrowDate()

                    FilterChip(
                        selected = bookingDate == today,
                        onClick = { bookingDate = today },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = bookingDate == tomorrow,
                        onClick = { bookingDate = tomorrow },
                        label = { Text("Tomorrow") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Start & End Time
                Text(
                    text = "Booking Times (24-Hour HH:mm) *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it.trim()
                            validationError = null
                        },
                        label = { Text("Start Time") },
                        placeholder = { Text("10:00") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_start_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it.trim()
                            validationError = null
                        },
                        label = { Text("End Time") },
                        placeholder = { Text("12:00") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_end_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Quick common time presets
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "09:00" to "11:00",
                        "10:00" to "12:00",
                        "12:00" to "14:00",
                        "14:00" to "16:00",
                        "16:00" to "18:00",
                        "18:00" to "20:00"
                    )
                    presets.forEach { (s, e) ->
                        FilterChip(
                            selected = startTime == s && endTime == e,
                            onClick = {
                                startTime = s
                                endTime = e
                            },
                            label = { Text("$s - $e", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Booking Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("Booking Amount ($) *") },
                    placeholder = { Text("100.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Payment Status: Paid / Unpaid
                Text(
                    text = "Payment Status *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        label = { Text("PAID") },
                        leadingIcon = if (isPaid) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaidGreen) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaidGreenContainer,
                            selectedLabelColor = PaidGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_paid_chip")
                    )
                    FilterChip(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        label = { Text("UNPAID") },
                        leadingIcon = if (!isPaid) {
                            { Icon(Icons.Default.Warning, contentDescription = null, tint = UnpaidOrange) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UnpaidOrangeContainer,
                            selectedLabelColor = UnpaidOrange
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("status_unpaid_chip")
                    )
                }

                // Error / Overlap message banner
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPropertyId <= 0L) {
                        validationError = "Please select a property."
                        return@Button
                    }
                    if (customerName.isBlank()) {
                        validationError = "Customer name cannot be empty."
                        return@Button
                    }
                    if (bookingDate.isBlank()) {
                        validationError = "Please enter a valid date (YYYY-MM-DD)."
                        return@Button
                    }
                    val dateRegex = Regex("""^\d{4}-\d{1,2}-\d{1,2}$""")
                    if (!bookingDate.matches(dateRegex)) {
                        validationError = "Please enter date in YYYY-MM-DD format."
                        return@Button
                    }
                    val timeRegex = Regex("""^([01]?[0-9]|2[0-3]):[0-5][0-9]$""")
                    if (!startTime.matches(timeRegex)) {
                        validationError = "Start time must be in HH:mm format (e.g. 10:00)."
                        return@Button
                    }
                    if (!endTime.matches(timeRegex)) {
                        validationError = "End time must be in HH:mm format (e.g. 12:00)."
                        return@Button
                    }
                    val sMin = DateTimeUtils.timeToMinutes(startTime)
                    val eMin = DateTimeUtils.timeToMinutes(endTime)
                    if (eMin <= sMin) {
                        validationError = "End time must be after start time."
                        return@Button
                    }
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount < 0) {
                        validationError = "Please enter a valid booking amount."
                        return@Button
                    }

                    onSave(
                        bookingToEdit?.id ?: 0L,
                        selectedPropertyId,
                        customerName.trim(),
                        bookingDate.trim(),
                        startTime.trim(),
                        endTime.trim(),
                        parsedAmount,
                        isPaid
                    )
                },
                enabled = properties.isNotEmpty(),
                modifier = Modifier.testTag("save_booking_submit_button")
            ) {
                Text(if (bookingToEdit == null) "Create Booking" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_booking_dialog_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
