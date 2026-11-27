package com.example.piggybank.util

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction

data class MonthlyBudgetUsage(
    val usedExpense: Long,
    val budgetLimit: Long,
    val usagePercentage: Int,
    val usagePercentageExact: Float,
    val usageFraction: Float,
    val remainingAmount: Long,
    val signedRemainingAmount: Long,
    val overBudgetAmount: Long,
    val isOverBudget: Boolean
)

data class MonthlyStatRow(
    val year: Int,
    val month: Int,
    val totalExpense: Long,
    val totalIncome: Long
) {
    val balance: Long
        get() = totalIncome - totalExpense
}

data class YearlyStatisticsGroup(
    val year: Int,
    val months: List<MonthlyStatRow>
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

fun calculateMonthExpense(
    transactions: List<Transaction>,
    month: YearMonthSelection
): Long {
    val prefix = month.toYearMonthPrefix()
    return transactions
        .filter { tx ->
            tx.type.equals("expense", ignoreCase = true) &&
                tx.date.trim().startsWith(prefix)
        }
        .sumOf { it.amount }
}

fun calculateMonthIncome(
    transactions: List<Transaction>,
    month: YearMonthSelection
): Long {
    val prefix = month.toYearMonthPrefix()
    return transactions
        .filter { tx ->
            tx.type.equals("income", ignoreCase = true) &&
                tx.date.trim().startsWith(prefix)
        }
        .sumOf { it.amount }
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
            usagePercentageExact = 0f,
            usageFraction = 0f,
            remainingAmount = 0L,
            signedRemainingAmount = 0L,
            overBudgetAmount = 0L,
            isOverBudget = false
        )
    }

    val exactPercentage = ((safeExpense.toDouble() * 100.0) / safeLimit.toDouble()).toFloat().coerceAtLeast(0f)
    val percentage = ((safeExpense * 100) / safeLimit).toInt().coerceAtLeast(0)
    val fraction = (safeExpense.toFloat() / safeLimit.toFloat()).coerceIn(0f, 1f)
    val signedRemaining = safeLimit - safeExpense
    val remaining = signedRemaining.coerceAtLeast(0L)
    val overBudget = (safeExpense - safeLimit).coerceAtLeast(0L)

    return MonthlyBudgetUsage(
        usedExpense = safeExpense,
        budgetLimit = safeLimit,
        usagePercentage = percentage,
        usagePercentageExact = exactPercentage,
        usageFraction = fraction,
        remainingAmount = remaining,
        signedRemainingAmount = signedRemaining,
        overBudgetAmount = overBudget,
        isOverBudget = safeExpense > safeLimit
    )
}

fun calculateAvailableYears(transactions: List<Transaction>): List<Int> =
    transactions
        .mapNotNull { parseYearMonthSelection(it.date)?.year }
        .distinct()
        .sortedDescending()

fun calculateMonthlyStatisticsByYear(
    transactions: List<Transaction>,
    filterYear: Int? = null
): List<YearlyStatisticsGroup> {
    val groupedByMonth = linkedMapOf<YearMonthSelection, Pair<Long, Long>>()
    transactions.forEach { tx ->
        val ym = parseYearMonthSelection(tx.date) ?: return@forEach
        if (filterYear != null && ym.year != filterYear) return@forEach
        val (prevExpense, prevIncome) = groupedByMonth[ym] ?: (0L to 0L)
        groupedByMonth[ym] = if (tx.type.equals("income", ignoreCase = true)) {
            prevExpense to (prevIncome + tx.amount)
        } else {
            (prevExpense + tx.amount) to prevIncome
        }
    }

    return groupedByMonth.entries
        .map { (ym, totals) ->
            MonthlyStatRow(
                year = ym.year,
                month = ym.month,
                totalExpense = totals.first,
                totalIncome = totals.second
            )
        }
        .groupBy { it.year }
        .entries
        .sortedByDescending { it.key }
        .map { (year, rows) ->
            YearlyStatisticsGroup(
                year = year,
                months = rows.sortedByDescending { it.month }
            )
        }
}
