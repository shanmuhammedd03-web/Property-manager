package com.example.data.repository

import com.example.data.local.BookingDao
import com.example.data.local.PropertyDao
import com.example.data.model.Booking
import com.example.data.model.BookingItem
import com.example.data.model.Property
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

data class AvailabilityResult(
    val isAvailable: Boolean,
    val conflictingBookings: List<Booking> = emptyList(),
    val errorMessage: String? = null
)

class PropertyBookingRepository(
    private val propertyDao: PropertyDao,
    private val bookingDao: BookingDao
) {
    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties()
    val allBookings: Flow<List<BookingItem>> = bookingDao.getAllBookings()

    suspend fun insertProperty(property: Property): Long = propertyDao.insertProperty(property)
    suspend fun updateProperty(property: Property) = propertyDao.updateProperty(property)
    suspend fun deleteProperty(property: Property) = propertyDao.deleteProperty(property)
    suspend fun deletePropertyById(id: Long) = propertyDao.deletePropertyById(id)

    suspend fun insertBooking(booking: Booking): Long = bookingDao.insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = bookingDao.updateBooking(booking)
    suspend fun deleteBooking(booking: Booking) = bookingDao.deleteBooking(booking)
    suspend fun deleteBookingById(id: Long) = bookingDao.deleteBookingById(id)
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean) = bookingDao.updatePaymentStatus(id, isPaid)

    fun observeBookingsForPropertyAndDate(propertyId: Long, date: String): Flow<List<Booking>> {
        return bookingDao.observeBookingsForPropertyAndDate(propertyId, date)
    }

    suspend fun checkAvailability(
        propertyId: Long,
        date: String,
        startTime: String,
        endTime: String,
        excludeBookingId: Long? = null
    ): AvailabilityResult {
        val sMin = DateTimeUtils.timeToMinutes(startTime)
        val eMin = DateTimeUtils.timeToMinutes(endTime)
        if (eMin <= sMin) {
            return AvailabilityResult(
                isAvailable = false,
                errorMessage = "End time must be after start time."
            )
        }

        val existing = bookingDao.getBookingsForPropertyAndDate(propertyId, date)
        val conflicts = existing.filter { booking ->
            if (excludeBookingId != null && booking.id == excludeBookingId) {
                false
            } else {
                DateTimeUtils.doTimesOverlap(startTime, endTime, booking.startTime, booking.endTime)
            }
        }

        return if (conflicts.isEmpty()) {
            AvailabilityResult(isAvailable = true)
        } else {
            AvailabilityResult(isAvailable = false, conflictingBookings = conflicts)
        }
    }
}
