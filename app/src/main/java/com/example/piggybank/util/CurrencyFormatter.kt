package com.example.piggybank.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Suppress("DEPRECATION")
fun Long.toRupiah(prefix: String = ""): String {
    val formatter = NumberFormat.getNumberInstance(Locale("in", "ID")).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
    }
    val formattedNumber = formatter.format(abs(this))
    val effectivePrefix = if (prefix.isEmpty() && this < 0L) "-" else prefix
    return "${effectivePrefix}Rp $formattedNumber"
}
