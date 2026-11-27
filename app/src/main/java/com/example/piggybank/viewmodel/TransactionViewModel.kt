package com.example.piggybank.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.piggybank.data.ConnectivityObserver
import com.example.piggybank.data.OfflineSyncStore
import com.example.piggybank.data.PendingOpType
import com.example.piggybank.data.isPermanentClientError
import com.example.piggybank.data.isTransientNetworkError
import com.example.piggybank.model.DailyTransactionGroup
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.filterAndSortTransactions
import com.example.piggybank.model.groupTransactionsForDisplay
import com.example.piggybank.network.ApiClient
import com.example.piggybank.util.YearMonthSelection
import com.example.piggybank.util.currentYearMonthSelection
import com.example.piggybank.util.parseYearMonthSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TransactionFilter(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

data class TransactionUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val groupedTransactions: List<DailyTransactionGroup> = emptyList(),
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedMonth: YearMonthSelection = currentYearMonthSelection(),
    val monthChangeDirection: Int = 0,
    val searchQuery: String = "",
    val selectedSortOption: TransactionSortOption = TransactionSortOption.NEWEST,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTransaction: Transaction? = null,
    val isDetailLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val actionErrorMessage: String? = null
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OfflineSyncStore.getInstance(application)
    private val syncMutex = Mutex()

    private val _uiState = MutableStateFlow(TransactionUiState(isLoading = true))
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val connectivityObserver = ConnectivityObserver(application) {
        fetchTransactions()
    }

    init {
        ApiClient.initialize(application)
        val cached = store.getCachedTransactions()
        if (cached.isNotEmpty()) {
            applyTransactions(cached)
        }
        connectivityObserver.register()
        fetchTransactions()
    }

    override fun onCleared() {
        connectivityObserver.unregister()
        super.onCleared()
    }

    fun resolveTransactionId(id: Long): Long = store.resolveTransactionId(id)

    fun fetchTransactions() {
        viewModelScope.launch {
            if (_uiState.value.allTransactions.isEmpty() && connectivityObserver.isOnline()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            syncAndRefreshInternal()
        }
    }

    fun fetchTransactionDetail(id: Long) {
        viewModelScope.launch {
            val resolvedId = store.resolveTransactionId(id)
            val cached = _uiState.value.allTransactions.find { it.id == resolvedId }
                ?: store.getCachedTransactions().find { it.id == resolvedId }
            _uiState.update {
                it.copy(
                    selectedTransaction = cached,
                    isDetailLoading = resolvedId > 0L && cached == null && connectivityObserver.isOnline(),
                    actionErrorMessage = null
                )
            }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                _uiState.update { it.copy(isDetailLoading = false) }
                return@launch
            }

            // Do not overwrite uncommitted local edits with stale remote data
            if (store.getPendingTransactionOps().any { it.targetId == resolvedId }) {
                _uiState.update { it.copy(isDetailLoading = false) }
                return@launch
            }

            try {
                val remote = ApiClient.apiService.getTransactionById(resolvedId)
                _uiState.update { current ->
                    val updatedList = if (current.allTransactions.any { it.id == remote.id }) {
                        current.allTransactions.map { if (it.id == remote.id) remote else it }
                    } else {
                        current.allTransactions
                    }
                    val filtered = computeFilteredTransactions(
                        transactions = updatedList,
                        filter = current.selectedFilter,
                        searchQuery = current.searchQuery,
                        sortOption = current.selectedSortOption,
                        month = current.selectedMonth
                    )
                    current.copy(
                        selectedTransaction = remote,
                        allTransactions = updatedList,
                        filteredTransactions = filtered,
                        groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption),
                        isDetailLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { current ->
                    current.copy(
                        selectedTransaction = current.selectedTransaction ?: cached,
                        isDetailLoading = false
                    )
                }
            }
        }
    }

    fun createTransaction(
        request: TransactionRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null) }

            if (!connectivityObserver.isOnline()) {
                applyLocalCreateTransaction(request, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingTransactionOpsLocked()
                    val created = ApiClient.apiService.createTransaction(request)
                    parseYearMonthSelection(created.date)?.let { targetMonth ->
                        _uiState.update { it.copy(selectedMonth = targetMonth, monthChangeDirection = 0) }
                    }
                    val updatedList = listOf(created) + store.getCachedTransactions().filterNot { it.id == created.id }
                    val merged = store.saveRemoteTransactionsMerged(updatedList)
                    applyTransactions(merged)
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalCreateTransaction(request, onSuccess)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                actionErrorMessage = e.message ?: "Gagal menyimpan transaksi."
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateTransaction(
        id: Long,
        request: TransactionRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val resolvedId = store.resolveTransactionId(id)
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null) }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                applyLocalUpdateTransaction(resolvedId, request, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingTransactionOpsLocked()
                    val postFlushId = store.resolveTransactionId(resolvedId)
                    val updated = ApiClient.apiService.updateTransaction(postFlushId, request)
                    parseYearMonthSelection(updated.date)?.let { targetMonth ->
                        _uiState.update { it.copy(selectedMonth = targetMonth, monthChangeDirection = 0) }
                    }
                    val updatedList = store.getCachedTransactions().map {
                        if (it.id == postFlushId) updated else it
                    }
                    val merged = store.saveRemoteTransactionsMerged(updatedList)
                    applyTransactions(merged)
                    _uiState.update {
                        it.copy(
                            selectedTransaction = updated,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalUpdateTransaction(store.resolveTransactionId(resolvedId), request, onSuccess)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                actionErrorMessage = e.message ?: "Gagal memperbarui transaksi."
                            )
                        }
                    }
                }
            }
        }
    }

    fun deleteTransaction(
        id: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val resolvedId = store.resolveTransactionId(id)
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null) }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                applyLocalDeleteTransaction(resolvedId, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingTransactionOpsLocked()
                    val postFlushId = store.resolveTransactionId(resolvedId)
                    ApiClient.apiService.deleteTransaction(postFlushId)
                    val remaining = store.getCachedTransactions().filterNot { it.id == postFlushId }
                    val merged = store.saveRemoteTransactionsMerged(remaining)
                    applyTransactions(merged)
                    _uiState.update {
                        it.copy(
                            selectedTransaction = null,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalDeleteTransaction(store.resolveTransactionId(resolvedId), onSuccess)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                actionErrorMessage = e.message ?: "Gagal menghapus transaksi."
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionErrorMessage = null) }
    }

    fun setFilter(filter: TransactionFilter) {
        _uiState.update { current ->
            val filtered = computeFilteredTransactions(
                transactions = current.allTransactions,
                filter = filter,
                searchQuery = current.searchQuery,
                sortOption = current.selectedSortOption,
                month = current.selectedMonth
            )
            current.copy(
                selectedFilter = filter,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = computeFilteredTransactions(
                transactions = current.allTransactions,
                filter = current.selectedFilter,
                searchQuery = query,
                sortOption = current.selectedSortOption,
                month = current.selectedMonth
            )
            current.copy(
                searchQuery = query,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption)
            )
        }
    }

    fun setSortOption(sortOption: TransactionSortOption) {
        _uiState.update { current ->
            val filtered = computeFilteredTransactions(
                transactions = current.allTransactions,
                filter = current.selectedFilter,
                searchQuery = current.searchQuery,
                sortOption = sortOption,
                month = current.selectedMonth
            )
            current.copy(
                selectedSortOption = sortOption,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, sortOption)
            )
        }
    }

    fun selectMonth(year: Int, month: Int) {
        val target = YearMonthSelection(year = year, month = month.coerceIn(1, 12))
        updateSelectedMonth(target, direction = 0)
    }

    fun goToPreviousMonth() {
        updateSelectedMonth(_uiState.value.selectedMonth.previousMonth(), direction = -1)
    }

    fun goToNextMonth() {
        updateSelectedMonth(_uiState.value.selectedMonth.nextMonth(), direction = 1)
    }

    private suspend fun syncAndRefreshInternal() {
        syncMutex.withLock {
            if (!connectivityObserver.isOnline()) {
                applyTransactions(store.getCachedTransactions())
                return
            }
            try {
                flushPendingTransactionOpsLocked()
                val remote = ApiClient.apiService.getTransactions()
                val merged = store.saveRemoteTransactionsMerged(remote)
                applyTransactions(merged)
            } catch (e: Exception) {
                if (isTransientNetworkError(e)) {
                    applyTransactions(store.getCachedTransactions())
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun refreshFromRemoteLocked() {
        try {
            val remote = ApiClient.apiService.getTransactions()
            val merged = store.saveRemoteTransactionsMerged(remote)
            applyTransactions(merged)
        } catch (e: Exception) {
            if (!isTransientNetworkError(e)) throw e
        }
    }

    private suspend fun flushPendingTransactionOpsLocked() {
        val pendingOps = store.getPendingTransactionOps()
        for (op in pendingOps) {
            val targetId = store.resolveTransactionId(op.targetId)
            when (op.type) {
                PendingOpType.CREATE -> {
                    val req = op.request ?: run {
                        store.removeTransactionOp(op.opId)
                        continue
                    }
                    try {
                        val created = ApiClient.apiService.createTransaction(req)
                        val updatedCache = store.markTransactionCreateSynced(op.opId, targetId, created)
                        if (_uiState.value.selectedTransaction?.id == targetId) {
                            _uiState.update { it.copy(selectedTransaction = created) }
                        }
                        applyTransactions(updatedCache)
                    } catch (e: Exception) {
                        if (isPermanentClientError(e)) {
                            store.removeTransactionOp(op.opId)
                        } else {
                            throw e
                        }
                    }
                }
                PendingOpType.UPDATE -> {
                    val req = op.request ?: run {
                        store.removeTransactionOp(op.opId)
                        continue
                    }
                    if (targetId < 0L) {
                        try {
                            val created = ApiClient.apiService.createTransaction(req)
                            store.markTransactionCreateSynced(op.opId, targetId, created)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeTransactionOp(op.opId) else throw e
                        }
                    } else {
                        try {
                            val updated = ApiClient.apiService.updateTransaction(targetId, req)
                            store.markTransactionUpdateSynced(op.opId, updated)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeTransactionOp(op.opId) else throw e
                        }
                    }
                }
                PendingOpType.DELETE -> {
                    if (targetId < 0L) {
                        store.removeTransactionOp(op.opId)
                    } else {
                        try {
                            ApiClient.apiService.deleteTransaction(targetId)
                            store.removeTransactionOp(op.opId)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeTransactionOp(op.opId) else throw e
                        }
                    }
                }
            }
        }
    }

    private fun applyLocalCreateTransaction(
        request: TransactionRequest,
        onSuccess: () -> Unit
    ) {
        val updatedCache = store.enqueueCreateTransaction(request)
        parseYearMonthSelection(request.date)?.let { targetMonth ->
            _uiState.update { it.copy(selectedMonth = targetMonth, monthChangeDirection = 0) }
        }
        applyTransactions(updatedCache)
        _uiState.update { it.copy(isSubmitting = false, actionErrorMessage = null) }
        onSuccess()
    }

    private fun applyLocalUpdateTransaction(
        id: Long,
        request: TransactionRequest,
        onSuccess: () -> Unit
    ) {
        val (updatedTx, updatedCache) = store.enqueueUpdateTransaction(id, request)
        parseYearMonthSelection(request.date)?.let { targetMonth ->
            _uiState.update { it.copy(selectedMonth = targetMonth, monthChangeDirection = 0) }
        }
        applyTransactions(updatedCache)
        _uiState.update {
            it.copy(
                selectedTransaction = updatedTx,
                isSubmitting = false,
                actionErrorMessage = null
            )
        }
        onSuccess()
    }

    private fun applyLocalDeleteTransaction(
        id: Long,
        onSuccess: () -> Unit
    ) {
        val remaining = store.enqueueDeleteTransaction(id)
        applyTransactions(remaining)
        _uiState.update {
            it.copy(
                selectedTransaction = null,
                isSubmitting = false,
                actionErrorMessage = null
            )
        }
        onSuccess()
    }

    private fun updateSelectedMonth(targetMonth: YearMonthSelection, direction: Int) {
        _uiState.update { current ->
            val (monthIncome, monthExpense) = calculateMonthlyTotals(
                transactions = current.allTransactions,
                month = targetMonth
            )
            val filtered = computeFilteredTransactions(
                transactions = current.allTransactions,
                filter = current.selectedFilter,
                searchQuery = current.searchQuery,
                sortOption = current.selectedSortOption,
                month = targetMonth
            )
            current.copy(
                selectedMonth = targetMonth,
                monthChangeDirection = direction,
                totalIncome = monthIncome,
                totalExpense = monthExpense,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption)
            )
        }
    }

    private fun applyTransactions(transactions: List<Transaction>) {
        val allIncome = transactions
            .filter { it.type.equals("income", ignoreCase = true) }
            .sumOf { it.amount }
        val allExpense = transactions
            .filter { it.type.equals("expense", ignoreCase = true) }
            .sumOf { it.amount }
        val netBalance = allIncome - allExpense

        _uiState.update { current ->
            val (monthIncome, monthExpense) = calculateMonthlyTotals(
                transactions = transactions,
                month = current.selectedMonth
            )
            val filtered = computeFilteredTransactions(
                transactions = transactions,
                filter = current.selectedFilter,
                searchQuery = current.searchQuery,
                sortOption = current.selectedSortOption,
                month = current.selectedMonth
            )
            current.copy(
                allTransactions = transactions,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption),
                totalIncome = monthIncome,
                totalExpense = monthExpense,
                netBalance = netBalance,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun calculateMonthlyTotals(
        transactions: List<Transaction>,
        month: YearMonthSelection
    ): Pair<Long, Long> {
        val prefix = month.toYearMonthPrefix()
        val monthTxs = transactions.filter { it.date.trim().startsWith(prefix) }
        val income = monthTxs
            .filter { it.type.equals("income", ignoreCase = true) }
            .sumOf { it.amount }
        val expense = monthTxs
            .filter { it.type.equals("expense", ignoreCase = true) }
            .sumOf { it.amount }
        return income to expense
    }

    private fun computeFilteredTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter,
        searchQuery: String,
        sortOption: TransactionSortOption,
        month: YearMonthSelection
    ): List<Transaction> {
        val typeFilter = when (filter) {
            TransactionFilter.ALL -> null
            TransactionFilter.INCOME -> "income"
            TransactionFilter.EXPENSE -> "expense"
        }
        return filterAndSortTransactions(
            transactions = transactions,
            typeFilter = typeFilter,
            searchQuery = searchQuery,
            sortOption = sortOption,
            monthPrefix = month.toYearMonthPrefix()
        )
    }
}
