package com.example.piggybank.util

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction

data class MonthlyBudgetUsage(
    val usedExpense: Long,
    val budgetLimit: Long,
    val usagePercentage: Int,
    val usageFraction: Float,
    val remainingAmount: Long,
    val overBudgetAmount: Long,
    val isOverBudget: Boolean
)

fun calculateTotalAssets(
    transactions: List<Transaction>,
    goals: List<SavingsGoal>
): Long {
    val totalIncome = transactions
        .filter { it.type.equals("income", ignoreCase = true) }
        .sumOf { it.amount }
    val totalExpense = transactions
        .filter { it.type.equals("expense", ignoreCase = true) }
        .sumOf { it.amount }
    val cashBalance = totalIncome - totalExpense
    val goalSavings = goals.sumOf { it.currentAmount }
    return cashBalance + goalSavings
}

fun calculateMonthlyBudgetUsage(
    usedExpense: Long,
    budgetLimit: Long
): MonthlyBudgetUsage {
    val safeExpense = usedExpense.coerceAtLeast(0L)
    val safeLimit = budgetLimit.coerceAtLeast(0L)
    if (safeLimit <= 0L) {
        return MonthlyBudgetUsage(
            usedExpense = safeExpense,
            budgetLimit = 0L,
            usagePercentage = 0,
            usageFraction = 0f,
            remainingAmount = 0L,
            overBudgetAmount = 0L,
            isOverBudget = false
        )
    }

    val percentage = ((safeExpense * 100) / safeLimit).toInt().coerceAtLeast(0)
    val fraction = (safeExpense.toFloat() / safeLimit.toFloat()).coerceIn(0f, 1f)
    val remaining = (safeLimit - safeExpense).coerceAtLeast(0L)
    val overBudget = (safeExpense - safeLimit).coerceAtLeast(0L)

    return MonthlyBudgetUsage(
        usedExpense = safeExpense,
        budgetLimit = safeLimit,
        usagePercentage = percentage,
        usageFraction = fraction,
        remainingAmount = remaining,
        overBudgetAmount = overBudget,
        isOverBudget = safeExpense > safeLimit
    )
}
