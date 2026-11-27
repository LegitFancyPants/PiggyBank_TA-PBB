package com.example.piggybank.model

import com.example.piggybank.util.compareSyncAwareCreationOrder
import com.example.piggybank.util.isIsoDatePast
import com.example.piggybank.util.todayIsoDate
import com.google.gson.annotations.SerializedName

data class SavingsGoal(
    val id: Long,
    val name: String,
    @SerializedName("target_amount")
    val targetAmount: Long,
    @SerializedName("current_amount")
    val currentAmount: Long,
    val deadline: String = "",
    val note: String = "",
    @SerializedName("created_at")
    val createdAt: String? = null
) {
    val progressFraction: Float
        get() = (if (targetAmount > 0L) currentAmount.toFloat() / targetAmount.toFloat() else 0f)
            .coerceIn(0f, 1f)

    val progressPercentage: Int
        get() = (if (targetAmount > 0L) ((currentAmount * 100) / targetAmount).toInt() else 0)
            .coerceIn(0, 100)

    val remainingAmount: Long
        get() = (targetAmount - currentAmount).coerceAtLeast(0L)

    val surplusAmount: Long
        get() = (currentAmount - targetAmount).coerceAtLeast(0L)

    val isCompleted: Boolean
        get() = targetAmount > 0L && currentAmount >= targetAmount

    fun isOverdue(todayIso: String = todayIsoDate()): Boolean =
        currentAmount < targetAmount && isIsoDatePast(deadline, todayIso)
}

enum class GoalStatusFilter(val label: String) {
    ALL("Semua"),
    IN_PROGRESS("Belum Tercapai"),
    COMPLETED("Tercapai")
}

enum class GoalSortOption(val label: String) {
    ALPHABETICAL("Sesuai Abjad (A–Z)"),
    OLDEST("Dari Terlama"),
    NEWEST("Dari Terbaru"),
    TARGET_DESC("Target Terbesar"),
    TARGET_ASC("Target Terkecil")
}

fun filterAndSortGoals(
    goals: List<SavingsGoal>,
    statusFilter: GoalStatusFilter = GoalStatusFilter.ALL,
    searchQuery: String = "",
    sortOption: GoalSortOption = GoalSortOption.NEWEST
): List<SavingsGoal> {
    val keyword = searchQuery.trim()
    val filtered = goals.filter { goal ->
        val matchesStatus = when (statusFilter) {
            GoalStatusFilter.ALL -> true
            GoalStatusFilter.IN_PROGRESS -> !goal.isCompleted
            GoalStatusFilter.COMPLETED -> goal.isCompleted
        }
        val matchesQuery = keyword.isEmpty() || goal.name.contains(keyword, ignoreCase = true)
        matchesStatus && matchesQuery
    }

    return when (sortOption) {
        GoalSortOption.ALPHABETICAL -> filtered.sortedWith(
            compareBy<SavingsGoal> { it.name.trim().lowercase() }
                .thenComparator { a, b -> compareCreationOrder(b, a) }
        )
        GoalSortOption.OLDEST -> filtered.sortedWith { a, b -> compareCreationOrder(a, b) }
        GoalSortOption.NEWEST -> filtered.sortedWith { a, b -> compareCreationOrder(b, a) }
        GoalSortOption.TARGET_DESC -> filtered.sortedWith(
            compareByDescending<SavingsGoal> { it.targetAmount }
                .thenComparator { a, b -> compareCreationOrder(b, a) }
        )
        GoalSortOption.TARGET_ASC -> filtered.sortedWith(
            compareBy<SavingsGoal> { it.targetAmount }
                .thenComparator { a, b -> compareCreationOrder(b, a) }
        )
    }
}

fun List<SavingsGoal>.sortedByDeadlineAndCreation(): List<SavingsGoal> =
    sortedWith { a, b ->
        val aDeadline = a.deadline.trim()
        val bDeadline = b.deadline.trim()
        val aHasDeadline = aDeadline.isNotEmpty()
        val bHasDeadline = bDeadline.isNotEmpty()

        when {
            aHasDeadline != bHasDeadline -> if (aHasDeadline) -1 else 1
            aHasDeadline && aDeadline != bDeadline -> aDeadline.compareTo(bDeadline)
            else -> compareCreationOrder(a, b)
        }
    }

private fun compareCreationOrder(a: SavingsGoal, b: SavingsGoal): Int =
    compareSyncAwareCreationOrder(a.id, a.createdAt, b.id, b.createdAt)

data class GoalRequest(
    val name: String,
    @SerializedName("target_amount")
    val targetAmount: Long,
    @SerializedName("current_amount")
    val currentAmount: Long,
    val deadline: String = "",
    val note: String = ""
)

fun sampleSavingsGoals(): List<SavingsGoal> = listOf(
    SavingsGoal(
        id = 1L,
        name = "Dana Darurat Mahasiswa",
        targetAmount = 3_000_000L,
        currentAmount = 1_800_000L,
        deadline = "2027-03-01",
        note = "Sisihkan Rp 50.000 setiap akhir pekan dari uang saku."
    ),
    SavingsGoal(
        id = 2L,
        name = "Upgrade SSD Laptop",
        targetAmount = 600_000L,
        currentAmount = 150_000L,
        deadline = "2027-01-15",
        note = "Untuk kebutuhan kompilasi proyek Android Studio."
    )
)
