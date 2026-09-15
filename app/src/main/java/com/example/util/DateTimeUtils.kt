package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat24 = SimpleDateFormat("HH:mm", Locale.US)
    private val friendlyDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val friendlyTimeFormat = SimpleDateFormat("h:mm a", Locale.US)

    fun getTodayDate(): String {
        return try {
            isoDateFormat.format(Date())
        } catch (_: Exception) {
            "2026-09-15"
        }
    }

    fun getTomorrowDate(): String {
        return try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            isoDateFormat.format(cal.time)
        } catch (_: Exception) {
            "2026-09-16"
        }
    }

    fun formatFriendlyDate(dateStr: String): String {
        return try {
            val date = isoDateFormat.parse(dateStr)
            if (date != null) friendlyDateFormat.format(date) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatFriendlyTime(time24: String): String {
        return try {
            val date = timeFormat24.parse(time24)
            if (date != null) friendlyTimeFormat.format(date) else time24
        } catch (_: Exception) {
            time24
        }
    }

    fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                (h * 60 + m).coerceIn(0, 1439)
            } else 0
        } catch (_: Exception) {
            0
        }
    }

    fun minutesToTime(minutes: Int): String {
        val safeMinutes = minutes.coerceIn(0, 1439)
        val h = (safeMinutes / 60) % 24
        val m = safeMinutes % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    /**
     * Checks if two time windows [startA, endA) and [startB, endB) overlap.
     * Touching boundaries (e.g. 10:00-11:00 and 11:00-12:00) do NOT overlap.
     */
    fun doTimesOverlap(
        startA: String, endA: String,
        startB: String, endB: String
    ): Boolean {
        val sA = timeToMinutes(startA)
        val eA = timeToMinutes(endA)
        val sB = timeToMinutes(startB)
        val eB = timeToMinutes(endB)

        // Invalid ranges do not qualify
        if (eA <= sA || eB <= sB) return false

        return sA < eB && eA > sB
    }

    fun isDateToday(dateStr: String): Boolean {
        return dateStr == getTodayDate()
    }

    fun isDateUpcoming(dateStr: String): Boolean {
        return dateStr >= getTodayDate()
    }

    fun launchCalendarEvent(
        context: Context,
        propertyName: String,
        customerName: String,
        dateStr: String,
        startTimeStr: String,
        endTimeStr: String,
        amount: Double,
        isPaid: Boolean,
        notes: String = ""
    ) {
        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val startDate = try { dateTimeFormat.parse("$dateStr $startTimeStr") } catch (_: Exception) { null }
            val endDate = try { dateTimeFormat.parse("$dateStr $endTimeStr") } catch (_: Exception) { null }

            val startMillis = startDate?.time ?: System.currentTimeMillis()
            val endMillis = endDate?.time ?: (startMillis + 60 * 60 * 1000)

            val eventDescription = buildString {
                append("Customer: ").append(customerName).append("\n")
                append("Property: ").append(propertyName).append("\n")
                append("Date: ").append(dateStr).append("\n")
                append("Time: ").append(startTimeStr).append(" - ").append(endTimeStr).append("\n")
                append("Amount: $").append(String.format(Locale.US, "%.2f", amount)).append("\n")
                append("Payment Status: ").append(if (isPaid) "PAID" else "UNPAID")
                if (notes.isNotBlank()) {
                    append("\nAdditional Info: ").append(notes)
                }
            }

            val primaryIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Booking: $propertyName - $customerName")
                putExtra(CalendarContract.Events.EVENT_LOCATION, propertyName)
                putExtra(CalendarContract.Events.DESCRIPTION, eventDescription)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(primaryIntent)
            } catch (_: ActivityNotFoundException) {
                // Fallback intent for devices with alternate calendar handlers
                val fallbackIntent = Intent(Intent.ACTION_EDIT).apply {
                    type = "vnd.android.cursor.item/event"
                    putExtra(CalendarContract.Events.TITLE, "Booking: $propertyName - $customerName")
                    putExtra(CalendarContract.Events.EVENT_LOCATION, propertyName)
                    putExtra(CalendarContract.Events.DESCRIPTION, eventDescription)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context.applicationContext,
                "No calendar app found on this device.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context.applicationContext,
                "Could not open calendar: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
