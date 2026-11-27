package com.example.piggybank.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import com.example.piggybank.util.nowIsoTimestampUtc
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

fun isTransientNetworkError(exception: Exception): Boolean {
    if (exception is CancellationException) throw exception
    if (exception is IOException) return true
    if (exception is HttpException) {
        val code = exception.code()
        return code == 408 || code == 429 || code >= 500
    }
    return false
}

fun isPermanentClientError(exception: Exception): Boolean {
    if (exception is CancellationException) throw exception
    if (exception is HttpException) {
        val code = exception.code()
        return code in 400..499 && code != 408 && code != 429
    }
    return false
}

enum class PendingOpType {
    CREATE,
    UPDATE,
    DELETE
}

data class PendingTransactionOp(
    val opId: String = UUID.randomUUID().toString(),
    val type: PendingOpType,
    val targetId: Long,
    val request: TransactionRequest? = null
)

data class PendingGoalOp(
    val opId: String = UUID.randomUUID().toString(),
    val type: PendingOpType,
    val targetId: Long,
    val request: GoalRequest? = null
)

interface KeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

class SharedPreferencesKeyValueStorage(context: Context) : KeyValueStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "piggybank_offline_store"
    }
}

/**
 * Local persistence cache and FIFO sync queue (outbox) for transactions and savings goals.
 * Unsynced local creations receive strictly negative IDs (< 0L) so they never collide with
 * server-assigned positive IDs and can be edited/deleted offline before syncing.
 */
class OfflineSyncStore(
    private val storage: KeyValueStorage,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> String = { nowIsoTimestampUtc() }
) {
    private val lock = Any()
    private val remappedTransactionIds = mutableMapOf<Long, Long>()
    private val remappedGoalIds = mutableMapOf<Long, Long>()

    fun resolveTransactionId(id: Long): Long = synchronized(lock) {
        var current = id
        val visited = mutableSetOf<Long>()
        while (remappedTransactionIds.containsKey(current) && visited.add(current)) {
            current = remappedTransactionIds[current] ?: break
        }
        current
    }

    fun resolveGoalId(id: Long): Long = synchronized(lock) {
        var current = id
        val visited = mutableSetOf<Long>()
        while (remappedGoalIds.containsKey(current) && visited.add(current)) {
            current = remappedGoalIds[current] ?: break
        }
        current
    }

    // =====================================================================
    // Transactions Cache & Outbox
    // =====================================================================

    fun getCachedTransactions(): List<Transaction> = synchronized(lock) {
        readList(KEY_TRANSACTIONS_CACHE, object : TypeToken<List<Transaction>>() {}.type)
    }

    fun getPendingTransactionOps(): List<PendingTransactionOp> = synchronized(lock) {
        readList(KEY_TRANSACTIONS_OUTBOX, object : TypeToken<List<PendingTransactionOp>>() {}.type)
    }

    fun enqueueCreateTransaction(request: TransactionRequest): List<Transaction> = synchronized(lock) {
        val localId = nextLocalIdLocked()
        val localTx = Transaction(
            id = localId,
            title = request.title,
            amount = request.amount,
            type = request.type,
            category = request.category,
            date = request.date,
            note = request.note,
            createdAt = nowProvider()
        )
        val updatedCache = listOf(localTx) + getCachedTransactions()
        saveList(KEY_TRANSACTIONS_CACHE, updatedCache)

        val updatedOps = getPendingTransactionOps() + PendingTransactionOp(
            type = PendingOpType.CREATE,
            targetId = localId,
            request = request
        )
        saveList(KEY_TRANSACTIONS_OUTBOX, updatedOps)
        updatedCache
    }

    fun enqueueUpdateTransaction(
        id: Long,
        request: TransactionRequest
    ): Pair<Transaction, List<Transaction>> = synchronized(lock) {
        val currentCache = getCachedTransactions()
        val existing = currentCache.find { it.id == id }
        val updatedTx = Transaction(
            id = id,
            title = request.title,
            amount = request.amount,
            type = request.type,
            category = request.category,
            date = request.date,
            note = request.note,
            createdAt = existing?.createdAt
        )
        val updatedCache = if (existing != null) {
            currentCache.map { if (it.id == id) updatedTx else it }
        } else {
            listOf(updatedTx) + currentCache
        }
        saveList(KEY_TRANSACTIONS_CACHE, updatedCache)

        val currentOps = getPendingTransactionOps().toMutableList()
        if (id < 0L) {
            val createIndex = currentOps.indexOfFirst { it.type == PendingOpType.CREATE && it.targetId == id }
            if (createIndex >= 0) {
                currentOps[createIndex] = currentOps[createIndex].copy(request = request)
            } else {
                currentOps.add(
                    PendingTransactionOp(type = PendingOpType.CREATE, targetId = id, request = request)
                )
            }
        } else {
            val updateIndex = currentOps.indexOfFirst { it.type == PendingOpType.UPDATE && it.targetId == id }
            if (updateIndex >= 0) {
                currentOps[updateIndex] = currentOps[updateIndex].copy(request = request)
            } else {
                currentOps.add(
                    PendingTransactionOp(type = PendingOpType.UPDATE, targetId = id, request = request)
                )
            }
        }
        saveList(KEY_TRANSACTIONS_OUTBOX, currentOps)
        updatedTx to updatedCache
    }

    fun enqueueDeleteTransaction(id: Long): List<Transaction> = synchronized(lock) {
        val updatedCache = getCachedTransactions().filterNot { it.id == id }
        saveList(KEY_TRANSACTIONS_CACHE, updatedCache)

        val currentOps = getPendingTransactionOps().toMutableList()
        if (id < 0L) {
            currentOps.removeAll { it.targetId == id }
        } else {
            currentOps.removeAll { it.type == PendingOpType.UPDATE && it.targetId == id }
            if (currentOps.none { it.type == PendingOpType.DELETE && it.targetId == id }) {
                currentOps.add(PendingTransactionOp(type = PendingOpType.DELETE, targetId = id))
            }
        }
        saveList(KEY_TRANSACTIONS_OUTBOX, currentOps)
        updatedCache
    }

    fun markTransactionCreateSynced(
        opId: String,
        localId: Long,
        createdRemote: Transaction
    ): List<Transaction> = synchronized(lock) {
        remappedTransactionIds[localId] = createdRemote.id
        val updatedCache = getCachedTransactions().let { list ->
            if (list.any { it.id == localId }) {
                list.map { if (it.id == localId) createdRemote else it }
            } else {
                listOf(createdRemote) + list.filterNot { it.id == createdRemote.id }
            }
        }
        saveList(KEY_TRANSACTIONS_CACHE, updatedCache)

        val updatedOps = getPendingTransactionOps()
            .filterNot { it.opId == opId }
            .map { op ->
                if (op.targetId == localId) op.copy(targetId = createdRemote.id) else op
            }
        saveList(KEY_TRANSACTIONS_OUTBOX, updatedOps)
        updatedCache
    }

    fun markTransactionUpdateSynced(opId: String, updatedRemote: Transaction): List<Transaction> = synchronized(lock) {
        val updatedCache = getCachedTransactions().map {
            if (it.id == updatedRemote.id) updatedRemote else it
        }
        saveList(KEY_TRANSACTIONS_CACHE, updatedCache)
        removeTransactionOpLocked(opId)
        updatedCache
    }

    fun removeTransactionOp(opId: String) = synchronized(lock) {
        removeTransactionOpLocked(opId)
    }

    fun saveRemoteTransactionsMerged(remoteTransactions: List<Transaction>): List<Transaction> = synchronized(lock) {
        val pendingOps = getPendingTransactionOps()
        val currentCache = getCachedTransactions()
        val merged = mergeTransactionsWithPendingOps(remoteTransactions, currentCache, pendingOps)
        saveList(KEY_TRANSACTIONS_CACHE, merged)
        merged
    }

    // =====================================================================
    // Savings Goals Cache & Outbox
    // =====================================================================

    fun getCachedGoals(): List<SavingsGoal> = synchronized(lock) {
        readList(KEY_GOALS_CACHE, object : TypeToken<List<SavingsGoal>>() {}.type)
    }

    fun getPendingGoalOps(): List<PendingGoalOp> = synchronized(lock) {
        readList(KEY_GOALS_OUTBOX, object : TypeToken<List<PendingGoalOp>>() {}.type)
    }

    fun enqueueCreateGoal(request: GoalRequest): List<SavingsGoal> = synchronized(lock) {
        val localId = nextLocalIdLocked()
        val localGoal = SavingsGoal(
            id = localId,
            name = request.name,
            targetAmount = request.targetAmount,
            currentAmount = request.currentAmount,
            deadline = request.deadline,
            note = request.note,
            createdAt = nowProvider()
        )
        val updatedCache = listOf(localGoal) + getCachedGoals()
        saveList(KEY_GOALS_CACHE, updatedCache)

        val updatedOps = getPendingGoalOps() + PendingGoalOp(
            type = PendingOpType.CREATE,
            targetId = localId,
            request = request
        )
        saveList(KEY_GOALS_OUTBOX, updatedOps)
        updatedCache
    }

    fun enqueueUpdateGoal(
        id: Long,
        request: GoalRequest
    ): Pair<SavingsGoal, List<SavingsGoal>> = synchronized(lock) {
        val currentCache = getCachedGoals()
        val existing = currentCache.find { it.id == id }
        val updatedGoal = SavingsGoal(
            id = id,
            name = request.name,
            targetAmount = request.targetAmount,
            currentAmount = request.currentAmount,
            deadline = request.deadline,
            note = request.note,
            createdAt = existing?.createdAt
        )
        val updatedCache = if (existing != null) {
            currentCache.map { if (it.id == id) updatedGoal else it }
        } else {
            listOf(updatedGoal) + currentCache
        }
        saveList(KEY_GOALS_CACHE, updatedCache)

        val currentOps = getPendingGoalOps().toMutableList()
        if (id < 0L) {
            val createIndex = currentOps.indexOfFirst { it.type == PendingOpType.CREATE && it.targetId == id }
            if (createIndex >= 0) {
                currentOps[createIndex] = currentOps[createIndex].copy(request = request)
            } else {
                currentOps.add(
                    PendingGoalOp(type = PendingOpType.CREATE, targetId = id, request = request)
                )
            }
        } else {
            val updateIndex = currentOps.indexOfFirst { it.type == PendingOpType.UPDATE && it.targetId == id }
            if (updateIndex >= 0) {
                currentOps[updateIndex] = currentOps[updateIndex].copy(request = request)
            } else {
                currentOps.add(
                    PendingGoalOp(type = PendingOpType.UPDATE, targetId = id, request = request)
                )
            }
        }
        saveList(KEY_GOALS_OUTBOX, currentOps)
        updatedGoal to updatedCache
    }

    fun enqueueDeleteGoal(id: Long): List<SavingsGoal> = synchronized(lock) {
        val updatedCache = getCachedGoals().filterNot { it.id == id }
        saveList(KEY_GOALS_CACHE, updatedCache)

        val currentOps = getPendingGoalOps().toMutableList()
        if (id < 0L) {
            currentOps.removeAll { it.targetId == id }
        } else {
            currentOps.removeAll { it.type == PendingOpType.UPDATE && it.targetId == id }
            if (currentOps.none { it.type == PendingOpType.DELETE && it.targetId == id }) {
                currentOps.add(PendingGoalOp(type = PendingOpType.DELETE, targetId = id))
            }
        }
        saveList(KEY_GOALS_OUTBOX, currentOps)
        updatedCache
    }

    fun markGoalCreateSynced(
        opId: String,
        localId: Long,
        createdRemote: SavingsGoal
    ): List<SavingsGoal> = synchronized(lock) {
        remappedGoalIds[localId] = createdRemote.id
        val updatedCache = getCachedGoals().let { list ->
            if (list.any { it.id == localId }) {
                list.map { if (it.id == localId) createdRemote else it }
            } else {
                listOf(createdRemote) + list.filterNot { it.id == createdRemote.id }
            }
        }
        saveList(KEY_GOALS_CACHE, updatedCache)

        val updatedOps = getPendingGoalOps()
            .filterNot { it.opId == opId }
            .map { op ->
                if (op.targetId == localId) op.copy(targetId = createdRemote.id) else op
            }
        saveList(KEY_GOALS_OUTBOX, updatedOps)
        updatedCache
    }

    fun markGoalUpdateSynced(opId: String, updatedRemote: SavingsGoal): List<SavingsGoal> = synchronized(lock) {
        val updatedCache = getCachedGoals().map {
            if (it.id == updatedRemote.id) updatedRemote else it
        }
        saveList(KEY_GOALS_CACHE, updatedCache)
        removeGoalOpLocked(opId)
        updatedCache
    }

    fun removeGoalOp(opId: String) = synchronized(lock) {
        removeGoalOpLocked(opId)
    }

    fun saveRemoteGoalsMerged(remoteGoals: List<SavingsGoal>): List<SavingsGoal> = synchronized(lock) {
        val pendingOps = getPendingGoalOps()
        val currentCache = getCachedGoals()
        val merged = mergeGoalsWithPendingOps(remoteGoals, currentCache, pendingOps)
        saveList(KEY_GOALS_CACHE, merged)
        merged
    }

    // =====================================================================
    // Merge & Internal Helpers
    // =====================================================================

    private fun mergeTransactionsWithPendingOps(
        remoteTransactions: List<Transaction>,
        currentCache: List<Transaction>,
        pendingOps: List<PendingTransactionOp>
    ): List<Transaction> {
        if (pendingOps.isEmpty()) return remoteTransactions

        val result = remoteTransactions.toMutableList()
        pendingOps.forEach { op ->
            when (op.type) {
                PendingOpType.DELETE -> {
                    result.removeAll { it.id == op.targetId }
                }
                PendingOpType.UPDATE -> {
                    val req = op.request ?: return@forEach
                    val index = result.indexOfFirst { it.id == op.targetId }
                    if (index >= 0) {
                        result[index] = result[index].copy(
                            title = req.title,
                            amount = req.amount,
                            type = req.type,
                            category = req.category,
                            date = req.date,
                            note = req.note
                        )
                    }
                }
                PendingOpType.CREATE -> {
                    val req = op.request ?: return@forEach
                    val localCopy = currentCache.find { it.id == op.targetId } ?: Transaction(
                        id = op.targetId,
                        title = req.title,
                        amount = req.amount,
                        type = req.type,
                        category = req.category,
                        date = req.date,
                        note = req.note,
                        createdAt = nowProvider()
                    )
                    if (result.none { it.id == op.targetId }) {
                        result.add(0, localCopy)
                    }
                }
            }
        }
        return result
    }

    private fun mergeGoalsWithPendingOps(
        remoteGoals: List<SavingsGoal>,
        currentCache: List<SavingsGoal>,
        pendingOps: List<PendingGoalOp>
    ): List<SavingsGoal> {
        if (pendingOps.isEmpty()) return remoteGoals

        val result = remoteGoals.toMutableList()
        pendingOps.forEach { op ->
            when (op.type) {
                PendingOpType.DELETE -> {
                    result.removeAll { it.id == op.targetId }
                }
                PendingOpType.UPDATE -> {
                    val req = op.request ?: return@forEach
                    val index = result.indexOfFirst { it.id == op.targetId }
                    if (index >= 0) {
                        result[index] = result[index].copy(
                            name = req.name,
                            targetAmount = req.targetAmount,
                            currentAmount = req.currentAmount,
                            deadline = req.deadline,
                            note = req.note
                        )
                    }
                }
                PendingOpType.CREATE -> {
                    val req = op.request ?: return@forEach
                    val localCopy = currentCache.find { it.id == op.targetId } ?: SavingsGoal(
                        id = op.targetId,
                        name = req.name,
                        targetAmount = req.targetAmount,
                        currentAmount = req.currentAmount,
                        deadline = req.deadline,
                        note = req.note,
                        createdAt = nowProvider()
                    )
                    if (result.none { it.id == op.targetId }) {
                        result.add(0, localCopy)
                    }
                }
            }
        }
        return result
    }

    private fun removeTransactionOpLocked(opId: String) {
        val updated = getPendingTransactionOps().filterNot { it.opId == opId }
        saveList(KEY_TRANSACTIONS_OUTBOX, updated)
    }

    private fun removeGoalOpLocked(opId: String) {
        val updated = getPendingGoalOps().filterNot { it.opId == opId }
        saveList(KEY_GOALS_OUTBOX, updated)
    }

    private fun nextLocalIdLocked(): Long {
        val current = storage.getString(KEY_NEXT_LOCAL_ID)?.toLongOrNull() ?: -1L
        val allocated = if (current < 0L) current else -1L
        storage.putString(KEY_NEXT_LOCAL_ID, (allocated - 1L).toString())
        return allocated
    }

    private fun <T> readList(key: String, type: java.lang.reflect.Type): List<T> {
        val raw = storage.getString(key) ?: return emptyList()
        return try {
            gson.fromJson<List<T>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun <T> saveList(key: String, items: List<T>) {
        storage.putString(key, gson.toJson(items))
    }

    companion object {
        private const val KEY_TRANSACTIONS_CACHE = "transactions_cache"
        private const val KEY_TRANSACTIONS_OUTBOX = "transactions_pending_ops"
        private const val KEY_GOALS_CACHE = "goals_cache"
        private const val KEY_GOALS_OUTBOX = "goals_pending_ops"
        private const val KEY_NEXT_LOCAL_ID = "next_local_id"

        @Volatile
        private var sharedInstance: OfflineSyncStore? = null

        fun getInstance(context: Context): OfflineSyncStore =
            sharedInstance ?: synchronized(this) {
                sharedInstance ?: OfflineSyncStore(
                    SharedPreferencesKeyValueStorage(context.applicationContext)
                ).also { sharedInstance = it }
            }
    }
}

/**
 * Observes default network connectivity changes so ViewModels can automatically
 * flush pending offline operations when internet access returns.
 */
class ConnectivityObserver(
    context: Context,
    private val onNetworkAvailable: () -> Unit
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var wasOffline = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wasOffline = false
            onNetworkAvailable()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (hasInternet && wasOffline) {
                wasOffline = false
                onNetworkAvailable()
            }
        }

        override fun onLost(network: Network) {
            wasOffline = true
        }
    }

    fun isOnline(): Boolean {
        val manager = connectivityManager ?: return true
        return try {
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    fun register(): Boolean {
        val manager = connectivityManager ?: return false
        return try {
            val activeNetwork = manager.activeNetwork
            val caps = manager.getNetworkCapabilities(activeNetwork)
            wasOffline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
            manager.registerDefaultNetworkCallback(callback)
            true
        } catch (e: Exception) {
            Log.w("ConnectivityObserver", "Failed to register network callback", e)
            false
        }
    }

    fun unregister() {
        val manager = connectivityManager ?: return
        try {
            manager.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // Ignore already unregistered callbacks
        }
    }
}
