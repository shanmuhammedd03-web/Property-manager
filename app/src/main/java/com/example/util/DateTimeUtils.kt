package com.example.util

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
        return isoDateFormat.format(Date())
    }

    fun getTomorrowDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return isoDateFormat.format(cal.time)
    }

    fun formatFriendlyDate(dateStr: String): String {
        return try {
            val date = isoDateFormat.parse(dateStr)
            if (date != null) friendlyDateFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatFriendlyTime(time24: String): String {
        return try {
            val date = timeFormat24.parse(time24)
            if (date != null) friendlyTimeFormat.format(date) else time24
        } catch (e: Exception) {
            time24
        }
    }

    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            return h * 60 + m
        }
        return 0
    }

    fun minutesToTime(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
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
        isPaid: Boolean
    ) {
        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val startDate = dateTimeFormat.parse("$dateStr $startTimeStr")
            val endDate = dateTimeFormat.parse("$dateStr $endTimeStr")

            val startMillis = startDate?.time ?: System.currentTimeMillis()
            val endMillis = endDate?.time ?: (startMillis + 60 * 60 * 1000)

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Booking: $propertyName - $customerName")
                putExtra(CalendarContract.Events.EVENT_LOCATION, propertyName)
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "Customer: $customerName\n" +
                            "Property: $propertyName\n" +
                            "Date: $dateStr\n" +
                            "Time: $startTimeStr - $endTimeStr\n" +
                            "Amount: \$${String.format(Locale.US, "%.2f", amount)}\n" +
                            "Payment Status: ${if (isPaid) "PAID" else "UNPAID"}"
                )
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open calendar application: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
