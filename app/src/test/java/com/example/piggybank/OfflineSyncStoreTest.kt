package com.example.piggybank

import com.example.piggybank.data.DeviceIdProvider
import com.example.piggybank.data.KeyValueStorage
import com.example.piggybank.data.OfflineSyncStore
import com.example.piggybank.data.PendingOpType
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import org.junit.Assert.assertEquals
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
}
