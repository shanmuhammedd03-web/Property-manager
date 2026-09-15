package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Booking
import com.example.data.model.BookingItem
import com.example.data.model.Property
import com.example.data.repository.AvailabilityResult
import com.example.data.repository.PropertyBookingRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalProperties: Int = 0,
    val todayBookingsCount: Int = 0,
    val upcomingBookingsCount: Int = 0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val todayBookings: List<BookingItem> = emptyList(),
    val upcomingBookings: List<BookingItem> = emptyList()
)

data class AvailabilityCheckState(
    val propertyId: Long = 0L,
    val date: String = DateTimeUtils.getTodayDate(),
    val startTime: String = "10:00",
    val endTime: String = "12:00",
    val result: AvailabilityResult? = null,
    val isChecking: Boolean = false
)

class PropertyBookingViewModel(
    private val repository: PropertyBookingRepository
) : ViewModel() {

    val properties: StateFlow<List<Property>> = repository.allProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBookings: StateFlow<List<BookingItem>> = repository.allBookings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dashboard stats derived state
    val dashboardStats: StateFlow<DashboardStats> = combine(properties, allBookings) { propList, bookList ->
        val todayStr = DateTimeUtils.getTodayDate()
        val todayList = bookList.filter { it.bookingDate == todayStr }
        val upcomingList = bookList.filter { it.bookingDate >= todayStr }

        var total = 0.0
        var paid = 0.0
        var unpaid = 0.0

        for (b in bookList) {
            total += b.amount
            if (b.isPaid) paid += b.amount else unpaid += b.amount
        }

        DashboardStats(
            totalProperties = propList.size,
            todayBookingsCount = todayList.size,
            upcomingBookingsCount = upcomingList.size,
            totalAmount = total,
            paidAmount = paid,
            unpaidAmount = unpaid,
            todayBookings = todayList,
            upcomingBookings = upcomingList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // Filter states for Bookings list
    private val _filterPropertyId = MutableStateFlow<Long?>(null)
    val filterPropertyId = _filterPropertyId.asStateFlow()

    private val _filterDate = MutableStateFlow<String?>(null)
    val filterDate = _filterDate.asStateFlow()

    private val _filterPaymentStatus = MutableStateFlow("ALL") // ALL, PAID, UNPAID
    val filterPaymentStatus = _filterPaymentStatus.asStateFlow()

    // Filtered bookings
    val filteredBookings: StateFlow<List<BookingItem>> = combine(
        allBookings,
        filterPropertyId,
        filterDate,
        filterPaymentStatus
    ) { bookings, propId, date, payment ->
        bookings.filter { b ->
            val matchesProp = propId == null || b.propertyId == propId
            val matchesDate = date == null || b.bookingDate == date
            val matchesPayment = when (payment) {
                "PAID" -> b.isPaid
                "UNPAID" -> !b.isPaid
                else -> true
            }
            matchesProp && matchesDate && matchesPayment
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Availability checker state
    private val _availabilityState = MutableStateFlow(AvailabilityCheckState())
    val availabilityState = _availabilityState.asStateFlow()

    // Observe bookings on selected date and property for the availability visual schedule
    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsOnAvailabilitySlot: StateFlow<List<Booking>> = availabilityState
        .flatMapLatest { state ->
            if (state.propertyId > 0) {
                repository.observeBookingsForPropertyAndDate(state.propertyId, state.date)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Toast/Alert message events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        // Automatically preselect the first property in availability checker once properties load
        viewModelScope.launch {
            properties.collect { list ->
                if (list.isNotEmpty() && _availabilityState.value.propertyId == 0L) {
                    _availabilityState.value = _availabilityState.value.copy(propertyId = list.first().id)
                    checkCurrentAvailability()
                }
            }
        }
    }

    // Property Actions
    fun addProperty(name: String, note: String = "") {
        if (name.isBlank()) {
            emitMessage("Property name cannot be empty")
            return
        }
        viewModelScope.launch {
            val id = repository.insertProperty(Property(name = name.trim(), note = note.trim()))
            emitMessage("Property '${name.trim()}' added")
            if (_availabilityState.value.propertyId == 0L) {
                _availabilityState.value = _availabilityState.value.copy(propertyId = id)
            }
        }
    }

    fun updateProperty(property: Property) {
        if (property.name.isBlank()) {
            emitMessage("Property name cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.updateProperty(property.copy(name = property.name.trim(), note = property.note.trim()))
            emitMessage("Property updated")
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            repository.deleteProperty(property)
            emitMessage("Deleted property '${property.name}' and its bookings")
            if (_availabilityState.value.propertyId == property.id) {
                val remaining = properties.value.filter { it.id != property.id }
                _availabilityState.value = _availabilityState.value.copy(
                    propertyId = remaining.firstOrNull()?.id ?: 0L,
                    result = null
                )
            }
        }
    }

    // Booking Actions
    fun saveBooking(
        bookingId: Long = 0L,
        propertyId: Long,
        customerName: String,
        date: String,
        startTime: String,
        endTime: String,
        amount: Double,
        isPaid: Boolean,
        notes: String = "",
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        if (propertyId <= 0L) {
            val err = "Please select a valid property"
            emitMessage(err)
            onError(err)
            return
        }
        if (customerName.isBlank()) {
            val err = "Customer name cannot be empty"
            emitMessage(err)
            onError(err)
            return
        }
        val sMin = DateTimeUtils.timeToMinutes(startTime)
        val eMin = DateTimeUtils.timeToMinutes(endTime)
        if (eMin <= sMin) {
            val err = "End time must be after start time"
            emitMessage(err)
            onError(err)
            return
        }

        viewModelScope.launch {
            // Check for overlap before saving
            val checkResult = repository.checkAvailability(
                propertyId = propertyId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                excludeBookingId = if (bookingId > 0) bookingId else null
            )

            if (!checkResult.isAvailable) {
                val conflict = checkResult.conflictingBookings.firstOrNull()
                val conflictMsg = if (conflict != null) {
                    "Double-booking prevented! Overlaps with ${conflict.customerName} (${conflict.startTime} - ${conflict.endTime})"
                } else {
                    checkResult.errorMessage ?: "Time slot is already booked"
                }
                emitMessage(conflictMsg)
                onError(conflictMsg)
                return@launch
            }

            val booking = Booking(
                id = bookingId,
                propertyId = propertyId,
                customerName = customerName.trim(),
                bookingDate = date,
                startTime = startTime,
                endTime = endTime,
                amount = amount,
                isPaid = isPaid,
                notes = notes.trim()
            )

            if (bookingId > 0) {
                repository.updateBooking(booking)
                emitMessage("Booking updated successfully")
            } else {
                repository.insertBooking(booking)
                emitMessage("Booking created successfully")
            }

            // Refresh availability checker if applicable
            checkCurrentAvailability()
            onSuccess()
        }
    }

    fun deleteBooking(bookingItem: BookingItem) {
        viewModelScope.launch {
            repository.deleteBookingById(bookingItem.id)
            emitMessage("Deleted booking for ${bookingItem.customerName}")
            checkCurrentAvailability()
        }
    }

    fun togglePaymentStatus(bookingItem: BookingItem) {
        viewModelScope.launch {
            val newStatus = !bookingItem.isPaid
            repository.updatePaymentStatus(bookingItem.id, newStatus)
            emitMessage("Marked as ${if (newStatus) "Paid" else "Unpaid"}")
        }
    }

    // Availability Actions
    fun updateAvailabilityProperty(propertyId: Long) {
        _availabilityState.value = _availabilityState.value.copy(propertyId = propertyId)
        checkCurrentAvailability()
    }

    fun updateAvailabilityDate(date: String) {
        _availabilityState.value = _availabilityState.value.copy(date = date)
        checkCurrentAvailability()
    }

    fun updateAvailabilityTimes(start: String, end: String) {
        _availabilityState.value = _availabilityState.value.copy(startTime = start, endTime = end)
        checkCurrentAvailability()
    }

    fun checkCurrentAvailability() {
        val current = _availabilityState.value
        if (current.propertyId <= 0L) return

        viewModelScope.launch {
            _availabilityState.value = _availabilityState.value.copy(isChecking = true)
            val result = repository.checkAvailability(
                propertyId = current.propertyId,
                date = current.date,
                startTime = current.startTime,
                endTime = current.endTime
            )
            _availabilityState.value = _availabilityState.value.copy(
                result = result,
                isChecking = false
            )
        }
    }

    // Filter Actions
    fun setFilterProperty(propertyId: Long?) {
        _filterPropertyId.value = propertyId
    }

    fun setFilterDate(date: String?) {
        _filterDate.value = date
    }

    fun setFilterPayment(status: String) {
        _filterPaymentStatus.value = status
    }

    fun clearFilters() {
        _filterPropertyId.value = null
        _filterDate.value = null
        _filterPaymentStatus.value = "ALL"
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}

class PropertyBookingViewModelFactory(
    private val repository: PropertyBookingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyBookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PropertyBookingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
