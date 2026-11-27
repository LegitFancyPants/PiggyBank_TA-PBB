package com.example.piggybank

import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.filterAndSortTransactions
import com.example.piggybank.model.groupTransactionsForDisplay
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.formatDateWithDay
import com.example.piggybank.util.parseYearMonthSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHelperTest {

    @Test
    fun formatsDateWithIndonesianDayName() {
        assertEquals("21 Nov  Sabtu", formatDateWithDay("2026-11-21", referenceYear = 2026))
        assertEquals("24 Nov  Selasa", formatDateWithDay("2026-11-24", referenceYear = 2026))
        assertEquals("21 Nov 2025  Jumat", formatDateWithDay("2025-11-21", referenceYear = 2026))
        assertEquals("invalid", formatDateWithDay("invalid", referenceYear = 2026))
    }

    @Test
    fun filtersByTypeAndFlexibleSearchOnTitleAndCategory() {
        val transactions = listOf(
            Transaction(1L, "Kopi Susu", 28_000L, "expense", "Makanan & Minuman", "2026-11-21"),
            Transaction(2L, "Nasi Padang", 27_500L, "expense", "Makanan & Minuman", "2026-11-21"),
            Transaction(3L, "Uang Saku", 1_500_000L, "income", "Gaji & Uang Saku", "2026-11-20")
        )

        assertEquals(3, filterAndSortTransactions(transactions).size)
        assertEquals(
            listOf("Uang Saku"),
            filterAndSortTransactions(transactions, typeFilter = "income").map { it.title }
        )
        assertEquals(
            listOf("Nasi Padang", "Kopi Susu"),
            filterAndSortTransactions(transactions, typeFilter = "expense").map { it.title }
        )
        assertEquals(
            listOf("Kopi Susu"),
            filterAndSortTransactions(transactions, searchQuery = "sUsU").map { it.title }
        )
        assertEquals(
            listOf("Nasi Padang", "Kopi Susu"),
            filterAndSortTransactions(transactions, searchQuery = "makanan").map { it.title }
        )
        assertTrue(filterAndSortTransactions(transactions, searchQuery = "tidak ada").isEmpty())
    }

    @Test
    fun sortsByAllFiveOptionsAndGroupsWithDailyAccumulation() {
        val transactions = listOf(
            Transaction(1L, "Kopi Susu", 28_000L, "expense", "Makanan", "2026-11-21"),
            Transaction(2L, "Bakso", 27_500L, "expense", "Makanan", "2026-11-21"),
            Transaction(3L, "Uang Saku", 1_500_000L, "income", "Gaji", "2026-11-20"),
            Transaction(4L, "Apel", 15_000L, "expense", "Makanan", "2026-11-20")
        )

        val newest = filterAndSortTransactions(transactions, sortOption = TransactionSortOption.NEWEST)
        val newestGroups = groupTransactionsForDisplay(newest, TransactionSortOption.NEWEST)
        assertEquals(2, newestGroups.size)
        assertEquals("2026-11-21", newestGroups[0].date)
        assertEquals(0L, newestGroups[0].totalIncome)
        assertEquals(55_500L, newestGroups[0].totalExpense)
        assertEquals(2, newestGroups[0].transactions.size)

        assertEquals("2026-11-20", newestGroups[1].date)
        assertEquals(1_500_000L, newestGroups[1].totalIncome)
        assertEquals(15_000L, newestGroups[1].totalExpense)

        val oldest = filterAndSortTransactions(transactions, sortOption = TransactionSortOption.OLDEST)
        assertEquals("2026-11-20", oldest.first().date)

        val alphabetical = filterAndSortTransactions(transactions, sortOption = TransactionSortOption.ALPHABETICAL)
        assertEquals(listOf("Apel", "Bakso", "Kopi Susu", "Uang Saku"), alphabetical.map { it.title })

        val amountDesc = filterAndSortTransactions(transactions, sortOption = TransactionSortOption.AMOUNT_DESC)
        assertEquals(listOf("Uang Saku", "Kopi Susu", "Bakso", "Apel"), amountDesc.map { it.title })

        val amountAsc = filterAndSortTransactions(transactions, sortOption = TransactionSortOption.AMOUNT_ASC)
        assertEquals(listOf("Apel", "Bakso", "Kopi Susu", "Uang Saku"), amountAsc.map { it.title })
    }

    @Test
    fun yearMonthSelectionStepsAcrossYearsAndFiltersByMonthPrefix() {
        val jan = YearMonthSelection(2026, 1)
        assertEquals(YearMonthSelection(2025, 12), jan.previousMonth())
        assertEquals("Des 2025", jan.previousMonth().formatLabel())

        val dec = YearMonthSelection(2026, 12)
        assertEquals(YearMonthSelection(2027, 1), dec.nextMonth())
        assertEquals("Jan 2027", dec.nextMonth().formatLabel())

        assertEquals(YearMonthSelection(2026, 11), parseYearMonthSelection("2026-11-26"))
        assertNull(parseYearMonthSelection("2026-13-01"))

        val txs = listOf(
            Transaction(1L, "Nov Tx", 20_000L, "expense", "Makanan", "2026-11-20"),
            Transaction(2L, "Okt Tx", 30_000L, "expense", "Makanan", "2026-10-15")
        )
        assertEquals(
            listOf("Nov Tx"),
            filterAndSortTransactions(txs, monthPrefix = "2026-11").map { it.title }
        )
        assertEquals(
            listOf("Okt Tx"),
            filterAndSortTransactions(txs, monthPrefix = "2026-10").map { it.title }
        )
    }
}
