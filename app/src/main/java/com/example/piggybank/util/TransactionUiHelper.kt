package com.example.piggybank.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val indonesianMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

val fullIndonesianMonths = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

data class YearMonthSelection(
    val year: Int,
    val month: Int
) : Comparable<YearMonthSelection> {
    fun toYearMonthPrefix(): String = String.format(Locale.US, "%04d-%02d", year, month)

    fun formatLabel(): String = "${indonesianMonths[(month - 1).coerceIn(0, 11)]} $year"

    fun previousMonth(): YearMonthSelection =
        if (month <= 1) YearMonthSelection(year - 1, 12) else YearMonthSelection(year, month - 1)

    fun nextMonth(): YearMonthSelection =
        if (month >= 12) YearMonthSelection(year + 1, 1) else YearMonthSelection(year, month + 1)

    override fun compareTo(other: YearMonthSelection): Int =
        if (year != other.year) year.compareTo(other.year) else month.compareTo(other.month)
}

fun currentYearMonthSelection(): YearMonthSelection {
    val calendar = Calendar.getInstance()
    return YearMonthSelection(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1
    )
}

fun parseYearMonthSelection(isoDate: String): YearMonthSelection? {
    val trimmed = isoDate.trim()
    if (!isValidCalendarDate(trimmed)) return null
    val parts = trimmed.split("-")
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    return YearMonthSelection(year = year, month = month)
}

private val indonesianDays = listOf(
    "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
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

fun formatDateWithDay(
    isoDate: String,
    referenceYear: Int = Calendar.getInstance().get(Calendar.YEAR)
): String {
    val trimmed = isoDate.trim()
    if (!isValidCalendarDate(trimmed)) return trimmed
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val parsed = parser.parse(trimmed) ?: return trimmed
        val calendar = Calendar.getInstance().apply { time = parsed }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = indonesianMonths[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        val dayName = indonesianDays[calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]
        if (year == referenceYear) {
            "$day $month  $dayName"
        } else {
            "$day $month $year  $dayName"
        }
    } catch (_: Exception) {
        trimmed
    }
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

fun sanitizeDateDigits(input: String): String =
    input.filter { it.isDigit() }.take(8)

fun formatDateDigitsToIso(digits: String): String {
    val clean = sanitizeDateDigits(digits)
    return when {
        clean.length <= 4 -> clean
        clean.length <= 6 -> "${clean.substring(0, 4)}-${clean.substring(4)}"
        else -> "${clean.substring(0, 4)}-${clean.substring(4, 6)}-${clean.substring(6)}"
    }
}

fun isValidCalendarDate(isoDate: String): Boolean {
    val trimmed = isoDate.trim()
    if (!isoDateRegex.matches(trimmed)) return false
    val year = trimmed.substring(0, 4).toIntOrNull() ?: return false
    if (year !in 1900..2100) return false
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val parsed = parser.parse(trimmed) ?: return false
        parser.format(parsed) == trimmed
    } catch (_: Exception) {
        false
    }
}

fun isoDateToUtcMillis(isoDate: String): Long? {
    if (!isValidCalendarDate(isoDate)) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        parser.parse(isoDate.trim())?.time
    } catch (_: Exception) {
        null
    }
}

class DateIsoVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text.take(8)
        if (original.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder(10)
        val origToTrans = IntArray(original.length + 1)
        for (i in original.indices) {
            origToTrans[i] = sb.length
            if (i == 4 || i == 6) {
                sb.append('-')
            }
            sb.append(original[i])
        }
        val transformed = sb.toString()
        origToTrans[original.length] = transformed.length

        val transToOrig = IntArray(transformed.length + 1)
        var digitCount = 0
        for (i in transformed.indices) {
            transToOrig[i] = digitCount
            if (transformed[i] != '-') {
                digitCount++
            }
        }
        transToOrig[transformed.length] = original.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, original.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, transformed.length)]
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

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
