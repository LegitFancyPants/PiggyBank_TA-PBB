package com.example.piggybank

import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.sampleSavingsGoals
import com.example.piggybank.model.sortedByDeadlineAndCreation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalTest {

    @Test
    fun calculatesProgressAndRemainingAccurately() {
        val samples = sampleSavingsGoals()
        assertEquals(2, samples.size)

        val emergencyFund = samples[0]
        assertEquals(60, emergencyFund.progressPercentage)
        assertEquals(0.6f, emergencyFund.progressFraction, 0.001f)
        assertEquals(1_200_000L, emergencyFund.remainingAmount)

        val ssdGoal = samples[1]
        assertEquals(25, ssdGoal.progressPercentage)
        assertEquals(0.25f, ssdGoal.progressFraction, 0.001f)
        assertEquals(450_000L, ssdGoal.remainingAmount)
    }

    @Test
    fun clampsFractionAndRemainingWhenOverTargetOrZeroTarget() {
        val overfunded = SavingsGoal(
            id = 3L,
            name = "Liburan",
            targetAmount = 1_000_000L,
            currentAmount = 1_250_000L
        )
        assertEquals(1.0f, overfunded.progressFraction, 0.001f)
        assertEquals(125, overfunded.progressPercentage)
        assertEquals(0L, overfunded.remainingAmount)

        val zeroTarget = SavingsGoal(
            id = 4L,
            name = "Kosong",
            targetAmount = 0L,
            currentAmount = 100_000L
        )
        assertEquals(0f, zeroTarget.progressFraction, 0.001f)
        assertEquals(0, zeroTarget.progressPercentage)
        assertEquals(0L, zeroTarget.remainingAmount)
    }

    @Test
    fun sortsByDeadlineThenCreationOrderWithEmptyDeadlinesLast() {
        val goals = listOf(
            SavingsGoal(id = 10L, name = "Tanpa Deadline 2", targetAmount = 100L, currentAmount = 0L, deadline = "", createdAt = "2026-11-22T02:00:00Z"),
            SavingsGoal(id = 11L, name = "Maret B", targetAmount = 100L, currentAmount = 0L, deadline = "2027-03-01", createdAt = "2026-11-22T05:00:00Z"),
            SavingsGoal(id = 12L, name = "Januari", targetAmount = 100L, currentAmount = 0L, deadline = "2027-01-15", createdAt = "2026-11-22T06:00:00Z"),
            SavingsGoal(id = 13L, name = "Maret A", targetAmount = 100L, currentAmount = 0L, deadline = "2027-03-01", createdAt = "2026-11-22T03:00:00Z"),
            SavingsGoal(id = 9L, name = "Tanpa Deadline 1", targetAmount = 100L, currentAmount = 0L, deadline = "", createdAt = "2026-11-22T01:00:00Z")
        )

        val sortedNames = goals.sortedByDeadlineAndCreation().map { it.name }
        assertEquals(
            listOf("Januari", "Maret A", "Maret B", "Tanpa Deadline 1", "Tanpa Deadline 2"),
            sortedNames
        )
    }

    @Test
    fun detectsOverdueGoalsOnlyWhenPastDeadlineAndNotYetReached() {
        val today = "2026-11-22"

        val overdue = SavingsGoal(
            id = 1L,
            name = "Keyboard",
            targetAmount = 750_000L,
            currentAmount = 115_000L,
            deadline = "2026-11-21"
        )
        assertTrue(overdue.isOverdue(today))

        val dueToday = overdue.copy(deadline = "2026-11-22")
        assertFalse(dueToday.isOverdue(today))

        val future = overdue.copy(deadline = "2026-11-23")
        assertFalse(future.isOverdue(today))

        val noDeadline = overdue.copy(deadline = "")
        assertFalse(noDeadline.isOverdue(today))

        val completedPastDeadline = overdue.copy(currentAmount = 750_000L)
        assertFalse(completedPastDeadline.isOverdue(today))
    }
}
