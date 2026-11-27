package com.example.piggybank

import com.example.piggybank.data.DeviceIdProvider
import com.example.piggybank.data.KeyValueStorage
import com.example.piggybank.data.OfflineSyncStore
import com.example.piggybank.data.PendingOpType
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.GoalSortOption
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.filterAndSortGoals
import com.example.piggybank.model.filterAndSortTransactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryKeyValueStorage : KeyValueStorage {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}

class OfflineSyncStoreTest {

    @Test
    fun offlineCreatedTransactionPersistsAcrossRestartAndSurvivesRemoteMergeBeforeSync() {
        val storage = InMemoryKeyValueStorage()
        val store1 = OfflineSyncStore(storage)

        val req = TransactionRequest(
            title = "Kopi Offline",
            amount = 18_000L,
            type = "expense",
            category = "Makanan & Minuman",
            date = "2026-11-26",
            note = "Dibuat tanpa internet"
        )
        val cached = store1.enqueueCreateTransaction(req)
        assertEquals(1, cached.size)
        assertTrue(cached[0].id < 0L)

        // Simulate app restart reading the same storage
        val store2 = OfflineSyncStore(storage)
        assertEquals(1, store2.getCachedTransactions().size)
        assertEquals(1, store2.getPendingTransactionOps().size)
        assertEquals(PendingOpType.CREATE, store2.getPendingTransactionOps()[0].type)

        // Even if remote fetch returns existing remote list before op finishes, pending local create stays
        val existingRemote = Transaction(10L, "Gaji", 2_000_000L, "income", "Gaji & Uang Saku", "2026-11-01")
        val mergedWhilePending = store2.saveRemoteTransactionsMerged(listOf(existingRemote))
        assertEquals(2, mergedWhilePending.size)
        assertTrue(mergedWhilePending.any { it.title == "Kopi Offline" })

        // Sync create op completes and maps negative local ID -> remote ID
        val op = store2.getPendingTransactionOps().first()
        val remoteCreated = Transaction(25L, req.title, req.amount, req.type, req.category, req.date, req.note)
        val afterSync = store2.markTransactionCreateSynced(op.opId, op.targetId, remoteCreated)

        assertEquals(0, store2.getPendingTransactionOps().size)
        assertEquals(25L, store2.resolveTransactionId(op.targetId))
        assertTrue(afterSync.any { it.id == 25L && it.title == "Kopi Offline" })
    }

    @Test
    fun offlineEditAndDeleteCollapseCleanlyForUnsyncedAndSyncedTransactions() {
        val store = OfflineSyncStore(InMemoryKeyValueStorage())
        val synced = Transaction(50L, "Lama", 10_000L, "expense", "Lainnya", "2026-11-20")
        store.saveRemoteTransactionsMerged(listOf(synced))

        // Create offline item and edit it before syncing -> single CREATE op with updated data
        val localList = store.enqueueCreateTransaction(
            TransactionRequest("Awal", 5_000L, "expense", "Belanja", "2026-11-26")
        )
        val localId = localList.first().id
        store.enqueueUpdateTransaction(
            localId,
            TransactionRequest("Direvisi", 7_500L, "expense", "Belanja", "2026-11-26")
        )
        assertEquals(1, store.getPendingTransactionOps().size)
        assertEquals("Direvisi", store.getPendingTransactionOps().first().request?.title)
        assertEquals(7_500L, store.getCachedTransactions().first { it.id == localId }.amount)

        // Delete unsynced item -> removed from both cache and outbox completely
        store.enqueueDeleteTransaction(localId)
        assertEquals(0, store.getPendingTransactionOps().size)

        // Update synced item offline, then delete it -> single DELETE op
        store.enqueueUpdateTransaction(
            50L,
            TransactionRequest("Edited", 12_000L, "expense", "Lainnya", "2026-11-20")
        )
        store.enqueueDeleteTransaction(50L)
        assertEquals(1, store.getPendingTransactionOps().size)
        assertEquals(PendingOpType.DELETE, store.getPendingTransactionOps().first().type)
        assertEquals(0, store.getCachedTransactions().size)
    }

    @Test
    fun offlineGoalCreateAndDepositPersistAndResolveAfterSync() {
        val store = OfflineSyncStore(InMemoryKeyValueStorage())

        val createdList = store.enqueueCreateGoal(
            GoalRequest("Laptop Baru", 5_000_000L, 500_000L, "2027-01-01", "")
        )
        val localGoalId = createdList.first().id
        assertTrue(localGoalId < 0L)

        // Deposit while still offline
        store.enqueueUpdateGoal(
            localGoalId,
            GoalRequest("Laptop Baru", 5_000_000L, 1_500_000L, "2027-01-01", "")
        )
        val ops = store.getPendingGoalOps()
        assertEquals(1, ops.size)
        assertEquals(1_500_000L, ops.first().request?.currentAmount)

        val remoteCreated = SavingsGoal(99L, "Laptop Baru", 5_000_000L, 1_500_000L, "2027-01-01", "")
        val syncedList = store.markGoalCreateSynced(ops.first().opId, localGoalId, remoteCreated)
        assertEquals(0, store.getPendingGoalOps().size)
        assertEquals(99L, store.resolveGoalId(localGoalId))
        assertEquals(1_500_000L, syncedList.first { it.id == 99L }.currentAmount)
    }

    @Test
    fun deviceIdProviderGeneratesOnceAndPersistsAcrossRestarts() {
        val storage = InMemoryKeyValueStorage()
        var counter = 0
        val provider1 = DeviceIdProvider(storage) { "device-uuid-${++counter}" }

        val first = provider1.getDeviceId()
        val second = provider1.getDeviceId()
        assertEquals("device-uuid-1", first)
        assertEquals(first, second)

        val provider2 = DeviceIdProvider(storage) { "device-uuid-${++counter}" }
        assertEquals("device-uuid-1", provider2.getDeviceId())
        assertEquals(1, counter)
    }

    @Test
    fun offlineCreatedTransactionsAndGoalsKeepIdenticalOrderBeforeAndAfterFifoSync() {
        val store = OfflineSyncStore(InMemoryKeyValueStorage())

        // Existing synced transactions on the same date
        val syncedTx1 = Transaction(10L, "Sarapan", 15_000L, "expense", "Makanan", "2026-11-27", createdAt = "2026-11-27T01:00:00.000000+00:00")
        val syncedTx2 = Transaction(11L, "Kopi", 20_000L, "expense", "Makanan", "2026-11-27", createdAt = "2026-11-27T02:00:00.000000+00:00")
        store.saveRemoteTransactionsMerged(listOf(syncedTx2, syncedTx1))

        // Add two offline transactions on the same date
        store.enqueueCreateTransaction(TransactionRequest("Makan Siang", 25_000L, "expense", "Makanan", "2026-11-27"))
        val cachedBeforeSync = store.enqueueCreateTransaction(TransactionRequest("Jajan Sore", 12_000L, "expense", "Makanan", "2026-11-27"))

        assertTrue(cachedBeforeSync.filter { it.id < 0L }.all { !it.createdAt.isNullOrBlank() })

        val expectedOrder = listOf("Jajan Sore", "Makan Siang", "Kopi", "Sarapan")
        assertEquals(
            expectedOrder,
            filterAndSortTransactions(cachedBeforeSync, sortOption = TransactionSortOption.NEWEST).map { it.title }
        )

        // Flush FIFO pending ops -> server assigns positive increasing IDs & timestamps
        val txOps = store.getPendingTransactionOps()
        assertEquals(2, txOps.size)
        store.markTransactionCreateSynced(
            txOps[0].opId,
            txOps[0].targetId,
            Transaction(12L, "Makan Siang", 25_000L, "expense", "Makanan", "2026-11-27", createdAt = "2026-11-27T05:00:00.000000+00:00")
        )
        val cachedAfterSync = store.markTransactionCreateSynced(
            txOps[1].opId,
            txOps[1].targetId,
            Transaction(13L, "Jajan Sore", 12_000L, "expense", "Makanan", "2026-11-27", createdAt = "2026-11-27T05:00:01.000000+00:00")
        )
        assertEquals(
            expectedOrder,
            filterAndSortTransactions(cachedAfterSync, sortOption = TransactionSortOption.NEWEST).map { it.title }
        )

        // Same verification for SavingsGoal
        val syncedGoal = SavingsGoal(20L, "Dana Darurat", 3_000_000L, 500_000L, createdAt = "2026-11-27T01:00:00.000000+00:00")
        store.saveRemoteGoalsMerged(listOf(syncedGoal))
        store.enqueueCreateGoal(GoalRequest("Goal Offline 1", 1_000_000L, 0L, "", ""))
        val goalsBeforeSync = store.enqueueCreateGoal(GoalRequest("Goal Offline 2", 2_000_000L, 0L, "", ""))

        assertFalse(goalsBeforeSync.first { it.name == "Goal Offline 2" }.createdAt.isNullOrBlank())
        val expectedGoalOrder = listOf("Goal Offline 2", "Goal Offline 1", "Dana Darurat")
        assertEquals(
            expectedGoalOrder,
            filterAndSortGoals(goalsBeforeSync, sortOption = GoalSortOption.NEWEST).map { it.name }
        )

        val goalOps = store.getPendingGoalOps()
        store.markGoalCreateSynced(
            goalOps[0].opId,
            goalOps[0].targetId,
            SavingsGoal(21L, "Goal Offline 1", 1_000_000L, 0L, createdAt = "2026-11-27T05:00:00.000000+00:00")
        )
        val goalsAfterSync = store.markGoalCreateSynced(
            goalOps[1].opId,
            goalOps[1].targetId,
            SavingsGoal(22L, "Goal Offline 2", 2_000_000L, 0L, createdAt = "2026-11-27T05:00:01.000000+00:00")
        )
        assertEquals(
            expectedGoalOrder,
            filterAndSortGoals(goalsAfterSync, sortOption = GoalSortOption.NEWEST).map { it.name }
        )
    }
}
