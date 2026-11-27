package com.example.piggybank

import com.example.piggybank.model.Transaction
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.buildChartMonthTimeline
import com.example.piggybank.util.buildDonutLegendEntries
import com.example.piggybank.util.calculateChartCategorySlices
import com.example.piggybank.util.chartCategoryColorArgb
import com.example.piggybank.util.formatCompactRupiah
import com.example.piggybank.util.formatMonthTabLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataHelperTest {

    @Test
    fun calculatesCategorySlicesFilteredByMonthAndType() {
        val transactions = listOf(
            Transaction(1L, "Jaket", 300_000L, "expense", "Belanja", "2026-11-10"),
            Transaction(2L, "Kopi", 100_000L, "expense", "Makanan", "2026-11-11"),
            Transaction(3L, "Bensin", 100_000L, "expense", "Transportasi", "2026-11-12"),
            Transaction(4L, "Gaji", 2_000_000L, "income", "Gaji", "2026-11-01"),
            Transaction(5L, "Makan Okt", 500_000L, "expense", "Makanan", "2026-10-15")
        )

        val novExpenses = calculateChartCategorySlices(transactions, "expense", YearMonthSelection(2026, 11))
        assertEquals(3, novExpenses.size)
        assertEquals("Belanja", novExpenses[0].category)
        assertEquals(300_000L, novExpenses[0].totalAmount)
        assertEquals(60f, novExpenses[0].percentage, 0.01f)

        val novIncome = calculateChartCategorySlices(transactions, "income", YearMonthSelection(2026, 11))
        assertEquals(1, novIncome.size)
        assertEquals("Gaji", novIncome[0].category)
        assertEquals(100f, novIncome[0].percentage, 0.01f)

        val decExpenses = calculateChartCategorySlices(transactions, "expense", YearMonthSelection(2026, 12))
        assertTrue(decExpenses.isEmpty())
    }

    @Test
    fun rollsUpCategoriesBeyondMaxEntriesIntoLainnyaForLegend() {
        val transactions = listOf(
            Transaction(1L, "A", 500L, "expense", "Cat1", "2026-11-01"),
            Transaction(2L, "B", 200L, "expense", "Cat2", "2026-11-01"),
            Transaction(3L, "C", 150L, "expense", "Cat3", "2026-11-01"),
            Transaction(4L, "D", 100L, "expense", "Cat4", "2026-11-01"),
            Transaction(5L, "E", 30L, "expense", "Cat5", "2026-11-01"),
            Transaction(6L, "F", 20L, "expense", "Cat6", "2026-11-01")
        )

        val slices = calculateChartCategorySlices(transactions, "expense", YearMonthSelection(2026, 11))
        val legend = buildDonutLegendEntries(slices, maxEntries = 5)
        assertEquals(5, legend.size)
        assertEquals("Cat1", legend[0].label)
        assertEquals("Cat4", legend[3].label)
        assertEquals("Lainnya", legend[4].label)
        assertEquals(5f, legend[4].percentage, 0.01f)
    }

    @Test
    fun formatsMonthTabLabelsAndCompactRupiahCorrectly() {
        val current = YearMonthSelection(2026, 11)
        assertEquals("Bulan Ini", formatMonthTabLabel(YearMonthSelection(2026, 11), current))
        assertEquals("Bulan Lalu", formatMonthTabLabel(YearMonthSelection(2026, 10), current))
        assertEquals("Sep 2026", formatMonthTabLabel(YearMonthSelection(2026, 9), current))

        val timeline = buildChartMonthTimeline(emptyList(), current, current)
        assertEquals(12, timeline.size)
        assertEquals(current, timeline.last())

        assertTrue(formatCompactRupiah(7_230_000L).contains("7,23 jt"))
        assertTrue(formatCompactRupiah(760_951L).contains("760.951"))
    }

    @Test
    fun categoryColorsRemainConsistentRegardlessOfRankingChanges() {
        val before = listOf(
            Transaction(1L, "Makan", 200_000L, "expense", "Makanan & Minuman", "2026-11-01"),
            Transaction(2L, "Ojek", 50_000L, "expense", "Transportasi", "2026-11-02")
        )
        val afterAddingLargerCategory = before + listOf(
            Transaction(3L, "Sepatu", 500_000L, "expense", "Belanja", "2026-11-03")
        )

        val legendBefore = buildDonutLegendEntries(
            calculateChartCategorySlices(before, "expense", YearMonthSelection(2026, 11))
        ).associateBy { it.label }

        val legendAfter = buildDonutLegendEntries(
            calculateChartCategorySlices(afterAddingLargerCategory, "expense", YearMonthSelection(2026, 11))
        ).associateBy { it.label }

        assertEquals(
            chartCategoryColorArgb("Makanan & Minuman"),
            legendBefore.getValue("Makanan & Minuman").colorArgb
        )
        assertEquals(
            legendBefore.getValue("Makanan & Minuman").colorArgb,
            legendAfter.getValue("Makanan & Minuman").colorArgb
        )
        assertEquals(
            legendBefore.getValue("Transportasi").colorArgb,
            legendAfter.getValue("Transportasi").colorArgb
        )
    }
}
