package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Booking
import com.example.data.model.BookingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("""
        SELECT b.id, b.propertyId, COALESCE(p.name, 'Unknown Property') AS propertyName, 
               b.customerName, b.bookingDate, b.startTime, b.endTime, b.amount, b.isPaid, b.createdAt
        FROM bookings b
        LEFT JOIN properties p ON b.propertyId = p.id
        ORDER BY b.bookingDate ASC, b.startTime ASC
    """)
    fun getAllBookings(): Flow<List<BookingItem>>

    @Query("SELECT * FROM bookings WHERE propertyId = :propertyId AND bookingDate = :date ORDER BY startTime ASC")
    suspend fun getBookingsForPropertyAndDate(propertyId: Long, date: String): List<Booking>

    @Query("SELECT * FROM bookings WHERE propertyId = :propertyId AND bookingDate = :date ORDER BY startTime ASC")
    fun observeBookingsForPropertyAndDate(propertyId: Long, date: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Long): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Long)

    @Query("UPDATE bookings SET isPaid = :isPaid WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean)
}
