package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = Property::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["propertyId"])]
)
data class Booking(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val propertyId: Long,
    val customerName: String,
    val bookingDate: String, // format "yyyy-MM-dd"
    val startTime: String,   // format "HH:mm"
    val endTime: String,     // format "HH:mm"
    val amount: Double,
    val isPaid: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
