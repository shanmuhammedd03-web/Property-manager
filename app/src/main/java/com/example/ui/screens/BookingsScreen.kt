package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BookingItem
import com.example.ui.components.BookingCard
import com.example.ui.viewmodel.PropertyBookingViewModel
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: PropertyBookingViewModel,
    onNewBookingClick: () -> Unit,
    onEditBooking: (BookingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookings by viewModel.filteredBookings.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val selectedPropertyId by viewModel.filterPropertyId.collectAsStateWithLifecycle()
    val selectedDate by viewModel.filterDate.collectAsStateWithLifecycle()
    val selectedPayment by viewModel.filterPaymentStatus.collectAsStateWithLifecycle()

    var propertyMenuExpanded by remember { mutableStateOf(false) }

    val hasActiveFilters = selectedPropertyId != null || selectedDate != null || selectedPayment != "ALL"
    val selectedPropName = properties.find { it.id == selectedPropertyId }?.name ?: "All Properties"

    Scaffold(
        modifier = modifier.testTag("bookings_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewBookingClick,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("fab_add_booking")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Booking")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Title & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bookings",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Showing ${bookings.size} booking${if (bookings.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasActiveFilters) {
                    OutlinedButton(
                        onClick = { viewModel.clearFilters() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_filters_button")
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filters")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Bar: Horizontal Scrollable Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Property Filter Menu
                Column {
                    FilterChip(
                        selected = selectedPropertyId != null,
                        onClick = { propertyMenuExpanded = true },
                        label = { Text(selectedPropName) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("filter_property_chip")
                    )

                    DropdownMenu(
                        expanded = propertyMenuExpanded,
                        onDismissRequest = { propertyMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Properties") },
                            onClick = {
                                viewModel.setFilterProperty(null)
                                propertyMenuExpanded = false
                            }
                        )
                        properties.forEach { prop ->
                            DropdownMenuItem(
                                text = { Text(prop.name) },
                                onClick = {
                                    viewModel.setFilterProperty(prop.id)
                                    propertyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Date Quick Filters: All Dates, Today, Tomorrow
                FilterChip(
                    selected = selectedDate == null,
                    onClick = { viewModel.setFilterDate(null) },
                    label = { Text("All Dates") },
                    modifier = Modifier.testTag("filter_date_all")
                )

                FilterChip(
                    selected = selectedDate == DateTimeUtils.getTodayDate(),
                    onClick = {
                        val today = DateTimeUtils.getTodayDate()
                        if (selectedDate == today) viewModel.setFilterDate(null) else viewModel.setFilterDate(today)
                    },
                    label = { Text("Today") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("filter_date_today")
                )

                FilterChip(
                    selected = selectedDate == DateTimeUtils.getTomorrowDate(),
                    onClick = {
                        val tomorrow = DateTimeUtils.getTomorrowDate()
                        if (selectedDate == tomorrow) viewModel.setFilterDate(null) else viewModel.setFilterDate(tomorrow)
                    },
                    label = { Text("Tomorrow") },
                    modifier = Modifier.testTag("filter_date_tomorrow")
                )

                // 3. Payment Status Filter
                FilterChip(
                    selected = selectedPayment == "PAID",
                    onClick = {
                        viewModel.setFilterPayment(if (selectedPayment == "PAID") "ALL" else "PAID")
                    },
                    label = { Text("Paid Only") },
                    modifier = Modifier.testTag("filter_paid_only")
                )

                FilterChip(
                    selected = selectedPayment == "UNPAID",
                    onClick = {
                        viewModel.setFilterPayment(if (selectedPayment == "UNPAID") "ALL" else "UNPAID")
                    },
                    label = { Text("Unpaid Only") },
                    modifier = Modifier.testTag("filter_unpaid_only")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bookings List
            if (bookings.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("empty_bookings_view")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (hasActiveFilters) "No bookings match the selected filters." else "No bookings yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasActiveFilters) "Try resetting filters or choosing another date." else "Tap '+' to schedule a new property booking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasActiveFilters) {
                            OutlinedButton(onClick = { viewModel.clearFilters() }) {
                                Text("Clear Filters")
                            }
                        } else {
                            OutlinedButton(onClick = onNewBookingClick) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Booking")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookings, key = { it.id }) { booking ->
                        BookingCard(
                            booking = booking,
                            onEdit = onEditBooking,
                            onDelete = { viewModel.deleteBooking(it) },
                            onTogglePayment = { viewModel.togglePaymentStatus(it) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}
