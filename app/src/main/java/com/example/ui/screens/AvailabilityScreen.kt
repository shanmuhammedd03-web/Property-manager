package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenContainer
import com.example.ui.theme.UnpaidOrange
import com.example.ui.theme.UnpaidOrangeContainer
import com.example.ui.viewmodel.PropertyBookingViewModel
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvailabilityScreen(
    viewModel: PropertyBookingViewModel,
    onBookSlot: (propertyId: Long, date: String, startTime: String, endTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.availabilityState.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val dayBookings by viewModel.bookingsOnAvailabilitySlot.collectAsStateWithLifecycle()

    var propertyDropdownExpanded by remember { mutableStateOf(false) }

    val selectedProperty = properties.find { it.id == state.propertyId }
    val result = state.result

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("availability_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check Availability",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Select property and hours to verify whether a slot is open or booked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Selection Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. Select Property
                    Text(
                        text = "1. Select Property",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = propertyDropdownExpanded,
                        onExpandedChange = { propertyDropdownExpanded = !propertyDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProperty?.name ?: "Select Property",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propertyDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("avail_property_selector"),
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
                                        viewModel.updateAvailabilityProperty(prop.id)
                                        propertyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Select Date
                    Text(
                        text = "2. Select Date",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = state.date,
                        onValueChange = { viewModel.updateAvailabilityDate(it.trim()) },
                        label = { Text("Date (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("avail_date_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val today = DateTimeUtils.getTodayDate()
                        val tomorrow = DateTimeUtils.getTomorrowDate()

                        FilterChip(
                            selected = state.date == today,
                            onClick = { viewModel.updateAvailabilityDate(today) },
                            label = { Text("Today") },
                            modifier = Modifier.testTag("avail_date_today_chip")
                        )
                        FilterChip(
                            selected = state.date == tomorrow,
                            onClick = { viewModel.updateAvailabilityDate(tomorrow) },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.testTag("avail_date_tomorrow_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Start & End Time
                    Text(
                        text = "3. Select Time Window (24-Hour)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.startTime,
                            onValueChange = { viewModel.updateAvailabilityTimes(it.trim(), state.endTime) },
                            label = { Text("Start Time") },
                            placeholder = { Text("10:00") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("avail_start_time_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.endTime,
                            onValueChange = { viewModel.updateAvailabilityTimes(state.startTime, it.trim()) },
                            label = { Text("End Time") },
                            placeholder = { Text("12:00") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("avail_end_time_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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
                                selected = state.startTime == s && state.endTime == e,
                                onClick = { viewModel.updateAvailabilityTimes(s, e) },
                                label = { Text("$s - $e", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }

        // Result Status Banner (Clear Available / Booked indicator)
        item {
            if (state.propertyId <= 0L) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please select or add a property above to check availability.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (result != null) {
                if (result.isAvailable) {
                    // CLEAR AVAILABLE BANNER
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaidGreenContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, PaidGreen, RoundedCornerShape(16.dp))
                            .testTag("availability_banner_available")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PaidGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Available",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AVAILABLE",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = androidx.compose.ui.graphics.Color(0xFF14532D)
                                        )
                                    )
                                    Text(
                                        text = "No conflicts found for ${selectedProperty?.name}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = androidx.compose.ui.graphics.Color(0xFF166534)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "The slot ${DateTimeUtils.formatFriendlyTime(state.startTime)} – ${DateTimeUtils.formatFriendlyTime(state.endTime)} on ${DateTimeUtils.formatFriendlyDate(state.date)} is open for booking.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = androidx.compose.ui.graphics.Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    onBookSlot(state.propertyId, state.date, state.startTime, state.endTime)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaidGreen,
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("avail_book_this_slot_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Book This Slot Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // CLEAR BOOKED / CONFLICT BANNER
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = UnpaidOrangeContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, UnpaidOrange, RoundedCornerShape(16.dp))
                            .testTag("availability_banner_booked")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(UnpaidOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Booked",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "BOOKED / UNAVAILABLE",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = androidx.compose.ui.graphics.Color(0xFF7C2D12)
                                        )
                                    )
                                    Text(
                                        text = "Double-booking prevented on ${selectedProperty?.name}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = androidx.compose.ui.graphics.Color(0xFF9A3412)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            val rawConflict = result.conflictingBookings.firstOrNull()
                            val conflict = rawConflict?.let { c ->
                                dayBookings.find { it.id == c.id } ?: c
                            }
                            if (conflict != null) {
                                Text(
                                    text = "Conflicting Booking:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = androidx.compose.ui.graphics.Color(0xFF7C2D12)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = conflict.customerName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${DateTimeUtils.formatFriendlyTime(conflict.startTime)} - ${DateTimeUtils.formatFriendlyTime(conflict.endTime)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (conflict.isPaid) PaidGreenContainer else UnpaidOrangeContainer
                                        ) {
                                            Text(
                                                text = if (conflict.isPaid) "PAID" else "UNPAID",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (conflict.isPaid) PaidGreen else UnpaidOrange,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = result.errorMessage ?: "Time slot is unavailable. Please choose another time.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = androidx.compose.ui.graphics.Color(0xFF7C2D12)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Schedule Timeline for the selected property on this date
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Existing Bookings on ${DateTimeUtils.formatFriendlyDate(state.date)} (${dayBookings.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (dayBookings.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No bookings currently scheduled on this date for this property. All day is free!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(dayBookings, key = { it.id }) { booking ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = booking.customerName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${DateTimeUtils.formatFriendlyTime(booking.startTime)} - ${DateTimeUtils.formatFriendlyTime(booking.endTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (booking.isPaid) PaidGreenContainer else UnpaidOrangeContainer
                        ) {
                            Text(
                                text = if (booking.isPaid) "PAID" else "UNPAID",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (booking.isPaid) PaidGreen else UnpaidOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
