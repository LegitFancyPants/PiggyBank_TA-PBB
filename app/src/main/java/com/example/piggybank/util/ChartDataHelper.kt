package com.example.piggybank.util

import com.example.piggybank.model.Transaction
import java.util.Locale

data class ChartCategorySlice(
    val category: String,
    val totalAmount: Long,
    val percentage: Float,
    val fraction: Float,
    val transactionCount: Int
)

data class ChartLegendEntry(
    val label: String,
    val percentage: Float,
    val fraction: Float,
    val colorArgb: Long
)

private val categoryColorMap = mapOf(
    "makanan" to 0xFF16A34A,
    "makanan & minuman" to 0xFF16A34A,
    "transportasi" to 0xFF2563EB,
    "belanja" to 0xFFEA580C,
    "tagihan & utilitas" to 0xFFD97706,
    "pendidikan" to 0xFF7C3AED,
    "gaji" to 0xFF0F766E,
    "gaji & saku" to 0xFF0F766E,
    "gaji & uang saku" to 0xFF0F766E,
    "uang saku" to 0xFF0F766E,
    "bonus & hadiah" to 0xFFDB2777,
    "freelance" to 0xFF0891B2,
    "lainnya" to 0xFF64748B
)

private val customCategoryColorFallbacks = listOf(
    0xFF059669, // Emerald
    0xFF4F46E5, // Indigo
    0xFFE11D48, // Rose
    0xFF0284C7, // Sky
    0xFF9333EA, // Purple
    0xFFCA8A04, // Yellow-Amber
    0xFF0D9488, // Teal
    0xFFDC2626  // Red
)

fun chartCategoryColorArgb(category: String): Long {
    val normalized = category.trim().lowercase(Locale.ROOT)
    categoryColorMap[normalized]?.let { return it }
    val index = Math.floorMod(normalized.hashCode(), customCategoryColorFallbacks.size)
    return customCategoryColorFallbacks[index]
}

fun calculateChartCategorySlices(
    transactions: List<Transaction>,
    type: String,
    month: YearMonthSelection
): List<ChartCategorySlice> {
    val prefix = month.toYearMonthPrefix()
    val filtered = transactions.filter { tx ->
        tx.type.equals(type, ignoreCase = true) &&
            tx.date.trim().startsWith(prefix) &&
            tx.amount > 0L
    }
    val totalAmount = filtered.sumOf { it.amount }
    if (totalAmount <= 0L) return emptyList()

    return filtered
        .groupBy { it.category.trim().ifBlank { "Lainnya" } }
        .map { (category, items) ->
            val sum = items.sumOf { it.amount }
            val fraction = (sum.toDouble() / totalAmount.toDouble()).toFloat().coerceIn(0f, 1f)
            val percentage = (fraction * 100f).coerceIn(0f, 100f)
            ChartCategorySlice(
                category = category,
                totalAmount = sum,
                percentage = percentage,
                fraction = fraction,
                transactionCount = items.size
            )
        }
        .sortedWith(
            compareByDescending<ChartCategorySlice> { it.totalAmount }
                .thenBy { it.category.lowercase() }
        )
}

fun buildDonutLegendEntries(
    slices: List<ChartCategorySlice>,
    maxEntries: Int = 5
): List<ChartLegendEntry> {
    if (slices.isEmpty()) return emptyList()
    if (slices.size <= maxEntries) {
        return slices.map { slice ->
            ChartLegendEntry(
                label = slice.category,
                percentage = slice.percentage,
                fraction = slice.fraction,
                colorArgb = chartCategoryColorArgb(slice.category)
            )
        }
    }

    val topCount = (maxEntries - 1).coerceAtLeast(1)
    val topEntries = slices.take(topCount).map { slice ->
        ChartLegendEntry(
            label = slice.category,
            percentage = slice.percentage,
            fraction = slice.fraction,
            colorArgb = chartCategoryColorArgb(slice.category)
        )
    }
    val remaining = slices.drop(topCount)
    val otherFraction = remaining.sumOf { it.fraction.toDouble() }.toFloat().coerceIn(0f, 1f)
    val otherPercentage = remaining.sumOf { it.percentage.toDouble() }.toFloat().coerceIn(0f, 100f)
    return topEntries + ChartLegendEntry(
        label = "Lainnya",
        percentage = otherPercentage,
        fraction = otherFraction,
        colorArgb = chartCategoryColorArgb("Lainnya")
    )
}

fun buildChartMonthTimeline(
    transactions: List<Transaction>,
    currentMonth: YearMonthSelection,
    selectedMonth: YearMonthSelection
): List<YearMonthSelection> {
    val parsedMonths = transactions.mapNotNull { parseYearMonthSelection(it.date) }
    var earliest = currentMonth
    repeat(11) { earliest = earliest.previousMonth() }
    parsedMonths.minOrNull()?.let { minTxMonth ->
        if (minTxMonth < earliest) earliest = minTxMonth
    }
    if (selectedMonth < earliest) earliest = selectedMonth

    val latest = maxOf(currentMonth, selectedMonth, parsedMonths.maxOrNull() ?: currentMonth)

    val result = mutableListOf<YearMonthSelection>()
    var cursor = earliest
    var guard = 0
    while (cursor <= latest && guard < 240) {
        result.add(cursor)
        cursor = cursor.nextMonth()
        guard++
    }
    return result
}

fun formatMonthTabLabel(
    month: YearMonthSelection,
    currentMonth: YearMonthSelection
): String = when (month) {
    currentMonth -> "Bulan Ini"
    currentMonth.previousMonth() -> "Bulan Lalu"
    else -> month.formatLabel()
}

private val indonesianLocale: Locale = Locale.forLanguageTag("id-ID")

fun formatCompactRupiah(amount: Long): String {
    val absAmount = kotlin.math.abs(amount)
    val sign = if (amount < 0L) "-" else ""
    return when {
        absAmount >= 1_000_000_000L -> {
            val formatted = String.format(indonesianLocale, "%.2f", absAmount / 1_000_000_000.0)
            "${sign}Rp $formatted M"
        }
        absAmount >= 1_000_000L -> {
            val formatted = String.format(indonesianLocale, "%.2f", absAmount / 1_000_000.0)
            "${sign}Rp $formatted jt"
        }
        else -> amount.toRupiah()
    }
}

fun formatChartPercentage(percentage: Float): String =
    String.format(indonesianLocale, "%.2f%%", percentage)
