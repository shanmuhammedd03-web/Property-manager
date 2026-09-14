package com.example.data.model

data class BookingItem(
    val id: Long,
    val propertyId: Long,
    val propertyName: String,
    val customerName: String,
    val bookingDate: String, // "yyyy-MM-dd"
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"
    val amount: Double,
    val isPaid: Boolean,
    val createdAt: Long
)
