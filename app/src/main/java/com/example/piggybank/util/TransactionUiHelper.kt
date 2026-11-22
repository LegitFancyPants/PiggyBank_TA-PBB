package com.example.piggybank.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val indonesianMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

fun transactionCategoryIcon(category: String): ImageVector = when (category.trim().lowercase()) {
    "makanan", "makanan & minuman" -> Icons.Default.Restaurant
    "transportasi" -> Icons.Default.TwoWheeler
    "belanja" -> Icons.Default.ShoppingBag
    "gaji", "gaji & saku", "gaji & uang saku", "uang saku", "bonus & hadiah", "freelance" -> Icons.Default.Payments
    else -> Icons.AutoMirrored.Filled.ReceiptLong
}

fun formatDisplayDate(isoDate: String): String {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    if (month !in 1..12 || day !in 1..31) return isoDate
    return "$day ${indonesianMonths[month - 1]} $year"
}

fun todayIsoDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date())
}

fun millisToIsoDate(utcMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(utcMillis))
}

private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

fun isIsoDatePast(isoDate: String, todayIso: String = todayIsoDate()): Boolean {
    val trimmed = isoDate.trim()
    if (!isoDateRegex.matches(trimmed)) return false
    return trimmed < todayIso
}

fun parseIsoTimestampMillis(timestamp: String?): Long? {
    if (timestamp.isNullOrBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            val parsed = parser.parse(timestamp.trim())
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
            // Try next format
        }
    }
    return null
}
