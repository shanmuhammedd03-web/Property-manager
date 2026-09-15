package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookingItem
import com.example.data.model.Property
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.ui.theme.UnpaidOrange
import com.example.ui.theme.UnpaidOrangeContainer
import com.example.util.DateTimeUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
        isPaid: Boolean,
        notes: String,
        onError: (String) -> Unit
    ) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

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
    var notes by remember { mutableStateOf(bookingToEdit?.notes ?: "") }

    var propertyDropdownExpanded by remember { mutableStateOf(false) }

    // Validation error states for clear screen display
    var validationError by remember { mutableStateOf<String?>(null) }
    var customerNameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    fun clearErrors() {
        validationError = null
        customerNameError = null
        dateError = null
        startTimeError = null
        endTimeError = null
        amountError = null
    }

    val executeSave: () -> Unit = {
        clearErrors()
        focusManager.clearFocus()
        keyboardController?.hide()

        var hasError = false

        if (selectedPropertyId <= 0L) {
            validationError = "Please select a property from the dropdown."
            hasError = true
        }
        if (customerName.isBlank()) {
            customerNameError = "Customer name is required"
            if (validationError == null) validationError = "Customer name cannot be empty."
            hasError = true
        }
        if (bookingDate.isBlank()) {
            dateError = "Booking date is required"
            if (validationError == null) validationError = "Please enter a valid date (YYYY-MM-DD)."
            hasError = true
        } else {
            val dateRegex = Regex("""^\d{4}-\d{1,2}-\d{1,2}$""")
            if (!bookingDate.matches(dateRegex)) {
                dateError = "Format must be YYYY-MM-DD"
                if (validationError == null) validationError = "Please enter date in YYYY-MM-DD format."
                hasError = true
            }
        }

        val timeRegex = Regex("""^([01]?[0-9]|2[0-3]):[0-5][0-9]$""")
        if (!startTime.matches(timeRegex)) {
            startTimeError = "Time must be HH:mm (e.g. 10:00)"
            if (validationError == null) validationError = "Start time must be in 24-hour HH:mm format."
            hasError = true
        }
        if (!endTime.matches(timeRegex)) {
            endTimeError = "Time must be HH:mm (e.g. 12:00)"
            if (validationError == null) validationError = "End time must be in 24-hour HH:mm format."
            hasError = true
        }

        if (!hasError) {
            val sMin = DateTimeUtils.timeToMinutes(startTime)
            val eMin = DateTimeUtils.timeToMinutes(endTime)
            if (eMin <= sMin) {
                endTimeError = "End time must be after start time"
                validationError = "End time ($endTime) must be after start time ($startTime)."
                hasError = true
            }
        }

        val parsedAmount = amountText.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount < 0) {
            amountError = "Enter a valid positive number"
            if (validationError == null) validationError = "Please enter a valid booking amount."
            hasError = true
        }

        if (!hasError) {
            onSave(
                bookingToEdit?.id ?: 0L,
                selectedPropertyId,
                customerName.trim(),
                bookingDate.trim(),
                startTime.trim(),
                endTime.trim(),
                parsedAmount!!,
                isPaid,
                notes.trim()
            ) { serverError ->
                validationError = serverError
            }
        }
    }

    // If properties were not loaded yet on open, set when loaded
    LaunchedEffect(properties) {
        if (selectedPropertyId == 0L && properties.isNotEmpty()) {
            selectedPropertyId = properties.first().id
        }
    }

    val selectedProperty = properties.find { it.id == selectedPropertyId }

    Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismiss()
                }
                .imePadding()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Tapping whitespace clears keyboard focus
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .testTag("booking_dialog_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- 1. Top Action Bar: Create and Cancel on top side of the keyboard ---
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("cancel_booking_dialog_button")
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (bookingToEdit == null) "New Booking" else "Edit Booking",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Button(
                                onClick = executeSave,
                                enabled = properties.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("save_booking_submit_button")
                            ) {
                                Text(
                                    text = if (bookingToEdit == null) "Create" else "Save",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- 2. Scrollable Middle Form ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Top Prominent Error Banner
                        if (validationError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = validationError ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

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
                                            clearErrors()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Customer Name
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = {
                                customerName = it
                                customerNameError = null
                                if (validationError != null) validationError = null
                            },
                            label = { Text("Customer Name *") },
                            placeholder = { Text("e.g., John Doe") },
                            singleLine = true,
                            isError = customerNameError != null,
                            supportingText = if (customerNameError != null) {
                                { Text(customerNameError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("booking_customer_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                dateError = null
                                if (validationError != null) validationError = null
                            },
                            label = { Text("Booking Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                            },
                            singleLine = true,
                            isError = dateError != null,
                            supportingText = if (dateError != null) {
                                { Text(dateError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
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
                                onClick = {
                                    bookingDate = today
                                    dateError = null
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("Today") }
                            )
                            FilterChip(
                                selected = bookingDate == tomorrow,
                                onClick = {
                                    bookingDate = tomorrow
                                    dateError = null
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("Tomorrow") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

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
                                    startTimeError = null
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("Start Time") },
                                placeholder = { Text("10:00") },
                                isError = startTimeError != null,
                                supportingText = if (startTimeError != null) {
                                    { Text(startTimeError!!, color = MaterialTheme.colorScheme.error) }
                                } else null,
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
                                    endTimeError = null
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("End Time") },
                                placeholder = { Text("12:00") },
                                isError = endTimeError != null,
                                supportingText = if (endTimeError != null) {
                                    { Text(endTimeError!!, color = MaterialTheme.colorScheme.error) }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_end_time_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. Booking Amount
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                amountError = null
                                if (validationError != null) validationError = null
                            },
                            label = { Text("Booking Amount ($) *") },
                            placeholder = { Text("100.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = amountError != null,
                            supportingText = if (amountError != null) {
                                { Text(amountError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("booking_amount_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 7. Additional Information (Optional Text Area) - No keyboard hide buttons
                        Text(
                            text = "Additional Information (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Special Requests") },
                            placeholder = { Text("Add any extra notes, customer requests, or special details here...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("booking_notes_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Bottom Error banner if present
                        if (validationError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
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
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- 3. Bottom Actions Bar (Also accessible if scrolled down) ---
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onDismiss()
                                }
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = executeSave,
                                enabled = properties.isNotEmpty()
                            ) {
                                Text(if (bookingToEdit == null) "Create Booking" else "Save Changes")
                            }
                        }
                    }
                }
            }
        }
    }
}
