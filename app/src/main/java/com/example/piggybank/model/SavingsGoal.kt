package com.example.piggybank.model

import com.example.piggybank.util.isIsoDatePast
import com.example.piggybank.util.parseIsoTimestampMillis
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
            .coerceAtLeast(0)

    val remainingAmount: Long
        get() = (targetAmount - currentAmount).coerceAtLeast(0L)

    fun isOverdue(todayIso: String = todayIsoDate()): Boolean =
        currentAmount < targetAmount && isIsoDatePast(deadline, todayIso)
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

private fun compareCreationOrder(a: SavingsGoal, b: SavingsGoal): Int {
    val aTime = parseIsoTimestampMillis(a.createdAt)
    val bTime = parseIsoTimestampMillis(b.createdAt)
    if (aTime != null && bTime != null && aTime != bTime) {
        return aTime.compareTo(bTime)
    }
    if (!a.createdAt.isNullOrBlank() && !b.createdAt.isNullOrBlank() && a.createdAt != b.createdAt) {
        return a.createdAt.compareTo(b.createdAt)
    }
    return a.id.compareTo(b.id)
}

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
