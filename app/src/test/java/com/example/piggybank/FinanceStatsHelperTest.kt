package com.example.piggybank

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.calculateAvailableYears
import com.example.piggybank.util.calculateMonthExpense
import com.example.piggybank.util.calculateMonthIncome
import com.example.piggybank.util.calculateMonthlyBudgetUsage
import com.example.piggybank.util.calculateMonthlyStatisticsByYear
import com.example.piggybank.util.calculateTotalAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceStatsHelperTest {

    @Test
    fun calculatesTotalAssetsFromCashAndGoals() {
        val transactions = listOf(
            Transaction(1L, "Gaji", 2_000_000L, "income", "Gaji", "2026-11-01"),
            Transaction(2L, "Makan", 300_000L, "expense", "Makanan", "2026-11-05"),
            Transaction(3L, "Bensin", 200_000L, "expense", "Transportasi", "2026-11-10")
        )
        val goals = listOf(
            SavingsGoal(1L, "Dana Darurat", 1_000_000L, 1_000_000L),
            SavingsGoal(2L, "SSD Laptop", 600_000L, 250_000L)
        )

        assertEquals(2_750_000L, calculateTotalAssets(transactions, goals))
    }

    @Test
    fun budgetUsageAdaptsDynamicallyToExistingExpensesAndLimitChanges() {
        val unset = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 0L)
        assertEquals(250_000L, unset.usedExpense)
        assertEquals(0, unset.usagePercentage)
        assertEquals(0f, unset.usagePercentageExact, 0.01f)
        assertFalse(unset.isOverBudget)

        val withinLimit = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 1_000_000L)
        assertEquals(25, withinLimit.usagePercentage)
        assertEquals(25f, withinLimit.usagePercentageExact, 0.01f)
        assertEquals(0.25f, withinLimit.usageFraction, 0.001f)
        assertEquals(750_000L, withinLimit.remainingAmount)
        assertEquals(750_000L, withinLimit.signedRemainingAmount)
        assertEquals(0L, withinLimit.overBudgetAmount)
        assertFalse(withinLimit.isOverBudget)

        val exceeded = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 200_000L)
        assertEquals(125, exceeded.usagePercentage)
        assertEquals(125f, exceeded.usagePercentageExact, 0.01f)
        assertEquals(1.0f, exceeded.usageFraction, 0.001f)
        assertEquals(0L, exceeded.remainingAmount)
        assertEquals(-50_000L, exceeded.signedRemainingAmount)
        assertEquals(50_000L, exceeded.overBudgetAmount)
        assertTrue(exceeded.isOverBudget)
    }

    @Test
    fun calculatesMonthlyStatisticsGroupedByYearAndFilteredByYear() {
        val transactions = listOf(
            Transaction(1L, "Gaji Nov", 2_000_000L, "income", "Gaji", "2026-11-01"),
            Transaction(2L, "Makan Nov", 500_000L, "expense", "Makanan", "2026-11-10"),
            Transaction(3L, "Bensin Okt", 300_000L, "expense", "Transportasi", "2026-10-15"),
            Transaction(4L, "Bonus 2025", 1_000_000L, "income", "Bonus", "2025-12-20")
        )

        assertEquals(listOf(2026, 2025), calculateAvailableYears(transactions))
        assertEquals(500_000L, calculateMonthExpense(transactions, YearMonthSelection(2026, 11)))
        assertEquals(2_000_000L, calculateMonthIncome(transactions, YearMonthSelection(2026, 11)))

        val allGroups = calculateMonthlyStatisticsByYear(transactions)
        assertEquals(2, allGroups.size)
        assertEquals(2026, allGroups[0].year)
        assertEquals(2, allGroups[0].months.size)
        assertEquals(11, allGroups[0].months[0].month)
        assertEquals(1_500_000L, allGroups[0].months[0].balance)
        assertEquals(10, allGroups[0].months[1].month)
        assertEquals(-300_000L, allGroups[0].months[1].balance)

        val only2025 = calculateMonthlyStatisticsByYear(transactions, filterYear = 2025)
        assertEquals(1, only2025.size)
        assertEquals(2025, only2025[0].year)
        assertEquals(1_000_000L, only2025[0].months[0].balance)
    }
}
