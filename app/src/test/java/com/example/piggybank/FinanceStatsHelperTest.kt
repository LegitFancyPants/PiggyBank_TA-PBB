package com.example.piggybank

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.util.calculateMonthlyBudgetUsage
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
        // Pengeluaran sudah ada Rp 250.000 sebelum anggaran diatur
        val unset = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 0L)
        assertEquals(250_000L, unset.usedExpense)
        assertEquals(0, unset.usagePercentage)
        assertFalse(unset.isOverBudget)

        // Baru diatur batas Rp 1.000.000 -> otomatis 25%, sisa Rp 750.000
        val withinLimit = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 1_000_000L)
        assertEquals(25, withinLimit.usagePercentage)
        assertEquals(0.25f, withinLimit.usageFraction, 0.001f)
        assertEquals(750_000L, withinLimit.remainingAmount)
        assertEquals(0L, withinLimit.overBudgetAmount)
        assertFalse(withinLimit.isOverBudget)

        // Batas diubah lebih kecil dari pengeluaran yang sudah ada (Rp 200.000) -> 125%, over budget Rp 50.000
        val exceeded = calculateMonthlyBudgetUsage(usedExpense = 250_000L, budgetLimit = 200_000L)
        assertEquals(125, exceeded.usagePercentage)
        assertEquals(1.0f, exceeded.usageFraction, 0.001f)
        assertEquals(0L, exceeded.remainingAmount)
        assertEquals(50_000L, exceeded.overBudgetAmount)
        assertTrue(exceeded.isOverBudget)
    }
}
