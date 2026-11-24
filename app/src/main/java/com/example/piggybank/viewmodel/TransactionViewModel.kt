package com.example.piggybank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piggybank.model.DailyTransactionGroup
import com.example.piggybank.model.Transaction
import com.example.piggybank.model.TransactionRequest
import com.example.piggybank.model.TransactionSortOption
import com.example.piggybank.model.filterAndSortTransactions
import com.example.piggybank.model.groupTransactionsForDisplay
import com.example.piggybank.model.sampleTransactions
import com.example.piggybank.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

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
    val searchQuery: String = "",
    val selectedSortOption: TransactionSortOption = TransactionSortOption.NEWEST,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUsingMockData: Boolean = false,
    val selectedTransaction: Transaction? = null,
    val isDetailLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val actionErrorMessage: String? = null
)

class TransactionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState(isLoading = true))
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        fetchTransactions()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transactions = ApiClient.apiService.getTransactions()
                applyTransactions(
                    transactions = transactions,
                    isUsingMockData = false,
                    errorMessage = null
                )
            } catch (_: Exception) {
                val fallback = _uiState.value.allTransactions.ifEmpty { sampleTransactions() }
                applyTransactions(
                    transactions = fallback,
                    isUsingMockData = true,
                    errorMessage = "Server belum terhubung. Menampilkan data contoh."
                )
            }
        }
    }

    fun fetchTransactionDetail(id: Long) {
        viewModelScope.launch {
            val cached = _uiState.value.allTransactions.find { it.id == id }
                ?: sampleTransactions().find { it.id == id }
            _uiState.update {
                it.copy(
                    selectedTransaction = cached,
                    isDetailLoading = true,
                    actionErrorMessage = null
                )
            }

            try {
                val remote = ApiClient.apiService.getTransactionById(id)
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
                        sortOption = current.selectedSortOption
                    )
                    current.copy(
                        selectedTransaction = remote,
                        allTransactions = updatedList,
                        filteredTransactions = filtered,
                        groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption),
                        isDetailLoading = false
                    )
                }
            } catch (_: Exception) {
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
            try {
                val created = ApiClient.apiService.createTransaction(request)
                val updatedList = listOf(created) + _uiState.value.allTransactions.filterNot { it.id == created.id }
                applyTransactions(
                    transactions = updatedList,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update { it.copy(isSubmitting = false) }
                fetchTransactions()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val localTransaction = Transaction(
                        id = System.currentTimeMillis(),
                        title = request.title,
                        amount = request.amount,
                        type = request.type,
                        category = request.category,
                        date = request.date,
                        note = request.note
                    )
                    val baseList = _uiState.value.allTransactions.ifEmpty { sampleTransactions() }
                    applyTransactions(
                        transactions = listOf(localTransaction) + baseList,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess()
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

    fun updateTransaction(
        id: Long,
        request: TransactionRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null) }
            try {
                val updated = ApiClient.apiService.updateTransaction(id, request)
                val updatedList = _uiState.value.allTransactions.map {
                    if (it.id == id) updated else it
                }
                applyTransactions(
                    transactions = updatedList,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update {
                    it.copy(
                        selectedTransaction = updated,
                        isSubmitting = false
                    )
                }
                fetchTransactions()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val existing = _uiState.value.allTransactions.find { it.id == id }
                        ?: _uiState.value.selectedTransaction
                    val updatedLocal = Transaction(
                        id = id,
                        title = request.title,
                        amount = request.amount,
                        type = request.type,
                        category = request.category,
                        date = request.date,
                        note = request.note,
                        createdAt = existing?.createdAt
                    )
                    val baseList = _uiState.value.allTransactions.ifEmpty { sampleTransactions() }
                    val updatedList = if (baseList.any { it.id == id }) {
                        baseList.map { if (it.id == id) updatedLocal else it }
                    } else {
                        listOf(updatedLocal) + baseList
                    }
                    applyTransactions(
                        transactions = updatedList,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update {
                        it.copy(
                            selectedTransaction = updatedLocal,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
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

    fun deleteTransaction(
        id: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null) }
            try {
                ApiClient.apiService.deleteTransaction(id)
                val remaining = _uiState.value.allTransactions.filterNot { it.id == id }
                applyTransactions(
                    transactions = remaining,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update {
                    it.copy(
                        selectedTransaction = null,
                        isSubmitting = false
                    )
                }
                fetchTransactions()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val remaining = _uiState.value.allTransactions.filterNot { it.id == id }
                    applyTransactions(
                        transactions = remaining,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update {
                        it.copy(
                            selectedTransaction = null,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
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

    fun clearActionError() {
        _uiState.update { it.copy(actionErrorMessage = null) }
    }

    fun setFilter(filter: TransactionFilter) {
        _uiState.update { current ->
            val filtered = computeFilteredTransactions(
                transactions = current.allTransactions,
                filter = filter,
                searchQuery = current.searchQuery,
                sortOption = current.selectedSortOption
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
                sortOption = current.selectedSortOption
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
                sortOption = sortOption
            )
            current.copy(
                selectedSortOption = sortOption,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, sortOption)
            )
        }
    }

    private fun shouldFallbackLocally(exception: Exception): Boolean {
        if (exception is IOException) return true
        if (exception is HttpException && (exception.code() == 404 || exception.code() >= 500)) {
            return true
        }
        return _uiState.value.isUsingMockData
    }

    private fun applyTransactions(
        transactions: List<Transaction>,
        isUsingMockData: Boolean,
        errorMessage: String?
    ) {
        val totalIncome = transactions
            .filter { it.type.equals("income", ignoreCase = true) }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { it.type.equals("expense", ignoreCase = true) }
            .sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        _uiState.update { current ->
            val filtered = computeFilteredTransactions(
                transactions = transactions,
                filter = current.selectedFilter,
                searchQuery = current.searchQuery,
                sortOption = current.selectedSortOption
            )
            current.copy(
                allTransactions = transactions,
                filteredTransactions = filtered,
                groupedTransactions = groupTransactionsForDisplay(filtered, current.selectedSortOption),
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = netBalance,
                isLoading = false,
                isUsingMockData = isUsingMockData,
                errorMessage = errorMessage
            )
        }
    }

    private fun computeFilteredTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter,
        searchQuery: String,
        sortOption: TransactionSortOption
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
            sortOption = sortOption
        )
    }
}
