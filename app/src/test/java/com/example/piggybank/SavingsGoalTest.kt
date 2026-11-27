package com.example.piggybank

import com.example.piggybank.model.GoalSortOption
import com.example.piggybank.model.GoalStatusFilter
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.filterAndSortGoals
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
        assertEquals(100, overfunded.progressPercentage)
        assertEquals(0L, overfunded.remainingAmount)
        assertEquals(250_000L, overfunded.surplusAmount)
        assertTrue(overfunded.isCompleted)

        val zeroTarget = SavingsGoal(
            id = 4L,
            name = "Kosong",
            targetAmount = 0L,
            currentAmount = 100_000L
        )
        assertEquals(0f, zeroTarget.progressFraction, 0.001f)
        assertEquals(0, zeroTarget.progressPercentage)
        assertEquals(0L, zeroTarget.remainingAmount)
        assertFalse(zeroTarget.isCompleted)
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

    @Test
    fun filtersByStatusAndFlexibleNameOnlySearch() {
        val goals = listOf(
            SavingsGoal(id = 1L, name = "Dana Darurat", targetAmount = 1_000_000L, currentAmount = 500_000L, note = "Khusus laptop"),
            SavingsGoal(id = 2L, name = "Upgrade SSD Laptop", targetAmount = 600_000L, currentAmount = 600_000L, note = "Rutin"),
            SavingsGoal(id = 3L, name = "Liburan Bali", targetAmount = 2_000_000L, currentAmount = 200_000L, note = "Akhir tahun")
        )

        assertEquals(3, filterAndSortGoals(goals, statusFilter = GoalStatusFilter.ALL).size)
        assertEquals(
            listOf("Liburan Bali", "Dana Darurat"),
            filterAndSortGoals(goals, statusFilter = GoalStatusFilter.IN_PROGRESS).map { it.name }
        )
        assertEquals(
            listOf("Upgrade SSD Laptop"),
            filterAndSortGoals(goals, statusFilter = GoalStatusFilter.COMPLETED).map { it.name }
        )

        // Substring di tengah, tidak sensitif kapital, dan tidak mencocokkan field note ("laptop" di Dana Darurat)
        assertEquals(
            listOf("Upgrade SSD Laptop"),
            filterAndSortGoals(goals, searchQuery = "ssd").map { it.name }
        )
        assertEquals(
            listOf("Upgrade SSD Laptop"),
            filterAndSortGoals(goals, searchQuery = "lApToP").map { it.name }
        )
        assertTrue(filterAndSortGoals(goals, searchQuery = "Motor Baru").isEmpty())
    }

    @Test
    fun sortsByAllFiveSortOptions() {
        val goals = listOf(
            SavingsGoal(id = 1L, name = "Zebra Tabungan", targetAmount = 500_000L, currentAmount = 0L, createdAt = "2026-11-01T10:00:00Z"),
            SavingsGoal(id = 2L, name = "Alpha Dana", targetAmount = 2_000_000L, currentAmount = 0L, createdAt = "2026-11-03T10:00:00Z"),
            SavingsGoal(id = 3L, name = "Modal Usaha", targetAmount = 1_000_000L, currentAmount = 0L, createdAt = "2026-11-02T10:00:00Z")
        )

        assertEquals(
            listOf("Alpha Dana", "Modal Usaha", "Zebra Tabungan"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.ALPHABETICAL).map { it.name }
        )
        assertEquals(
            listOf("Zebra Tabungan", "Modal Usaha", "Alpha Dana"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.OLDEST).map { it.name }
        )
        assertEquals(
            listOf("Alpha Dana", "Modal Usaha", "Zebra Tabungan"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.NEWEST).map { it.name }
        )
        assertEquals(
            listOf("Alpha Dana", "Modal Usaha", "Zebra Tabungan"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.TARGET_DESC).map { it.name }
        )
        assertEquals(
            listOf("Zebra Tabungan", "Modal Usaha", "Alpha Dana"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.TARGET_ASC).map { it.name }
        )
    }

    @Test
    fun offlineGoalsAppearAtTopInNewestAndAfterSyncedInOldestAndDeadlineOrder() {
        val goals = listOf(
            SavingsGoal(id = 10L, name = "Synced Lama", targetAmount = 1_000_000L, currentAmount = 0L, deadline = "2027-06-01", createdAt = "2026-11-27T01:00:00.000000+00:00"),
            SavingsGoal(id = 11L, name = "Synced Baru", targetAmount = 1_000_000L, currentAmount = 0L, deadline = "2027-06-01", createdAt = "2026-11-27T01:05:00.000000+00:00"),
            SavingsGoal(id = -1L, name = "Offline Pertama", targetAmount = 1_000_000L, currentAmount = 0L, deadline = "2027-06-01", createdAt = "2026-11-27T00:30:00.000Z"),
            SavingsGoal(id = -2L, name = "Offline Kedua", targetAmount = 1_000_000L, currentAmount = 0L, deadline = "2027-06-01", createdAt = "2026-11-27T00:31:00.000Z")
        )

        assertEquals(
            listOf("Offline Kedua", "Offline Pertama", "Synced Baru", "Synced Lama"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.NEWEST).map { it.name }
        )
        assertEquals(
            listOf("Synced Lama", "Synced Baru", "Offline Pertama", "Offline Kedua"),
            filterAndSortGoals(goals, sortOption = GoalSortOption.OLDEST).map { it.name }
        )
        assertEquals(
            listOf("Synced Lama", "Synced Baru", "Offline Pertama", "Offline Kedua"),
            goals.sortedByDeadlineAndCreation().map { it.name }
        )
    }
}
