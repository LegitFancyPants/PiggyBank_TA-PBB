package com.example.piggybank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.GoalSortOption
import com.example.piggybank.model.GoalStatusFilter
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.filterAndSortGoals
import com.example.piggybank.model.sampleSavingsGoals
import com.example.piggybank.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class GoalUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val displayedGoals: List<SavingsGoal> = emptyList(),
    val selectedStatusFilter: GoalStatusFilter = GoalStatusFilter.ALL,
    val searchQuery: String = "",
    val selectedSortOption: GoalSortOption = GoalSortOption.NEWEST,
    val totalActiveGoals: Int = 0,
    val totalSavedAllGoals: Long = 0L,
    val selectedGoal: SavingsGoal? = null,
    val isLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isUsingMockData: Boolean = false
)

class GoalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState(isLoading = true))
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        fetchGoals()
    }

    fun fetchGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val remoteGoals = ApiClient.apiService.getGoals()
                applyGoals(
                    goals = remoteGoals,
                    isUsingMockData = false,
                    errorMessage = null
                )
            } catch (_: Exception) {
                val fallback = _uiState.value.goals.ifEmpty { sampleSavingsGoals() }
                applyGoals(
                    goals = fallback,
                    isUsingMockData = true,
                    errorMessage = "Server belum terhubung. Menampilkan data celengan contoh."
                )
            }
        }
    }

    fun fetchGoalDetail(id: Long) {
        viewModelScope.launch {
            val cached = _uiState.value.goals.find { it.id == id }
                ?: sampleSavingsGoals().find { it.id == id }
            _uiState.update {
                it.copy(
                    selectedGoal = cached,
                    isDetailLoading = true,
                    errorMessage = null
                )
            }

            try {
                val remote = ApiClient.apiService.getGoalById(id)
                val updatedList = if (_uiState.value.goals.any { it.id == remote.id }) {
                    _uiState.value.goals.map { if (it.id == remote.id) remote else it }
                } else {
                    _uiState.value.goals
                }
                _uiState.update { current ->
                    current.copy(
                        goals = updatedList,
                        displayedGoals = filterAndSortGoals(
                            goals = updatedList,
                            statusFilter = current.selectedStatusFilter,
                            searchQuery = current.searchQuery,
                            sortOption = current.selectedSortOption
                        ),
                        totalActiveGoals = updatedList.size,
                        totalSavedAllGoals = updatedList.sumOf { goal -> goal.currentAmount },
                        selectedGoal = remote,
                        isDetailLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        selectedGoal = current.selectedGoal ?: cached,
                        isDetailLoading = false
                    )
                }
            }
        }
    }

    fun createGoal(
        request: GoalRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val created = ApiClient.apiService.createGoal(request)
                val updatedList = listOf(created) + _uiState.value.goals.filterNot { it.id == created.id }
                applyGoals(
                    goals = updatedList,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update { it.copy(isSubmitting = false) }
                fetchGoals()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val baseList = _uiState.value.goals.ifEmpty { sampleSavingsGoals() }
                    val nextId = maxOf(
                        System.currentTimeMillis(),
                        (baseList.maxOfOrNull { it.id } ?: 0L) + 1L
                    )
                    val localGoal = SavingsGoal(
                        id = nextId,
                        name = request.name,
                        targetAmount = request.targetAmount,
                        currentAmount = request.currentAmount,
                        deadline = request.deadline,
                        note = request.note
                    )
                    applyGoals(
                        goals = baseList + localGoal,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = e.message ?: "Gagal menyimpan target tabungan."
                        )
                    }
                }
            }
        }
    }

    fun updateGoal(
        id: Long,
        request: GoalRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val updated = ApiClient.apiService.updateGoal(id, request)
                val updatedList = _uiState.value.goals.map { if (it.id == id) updated else it }
                applyGoals(
                    goals = updatedList,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update {
                    it.copy(
                        selectedGoal = updated,
                        isSubmitting = false
                    )
                }
                fetchGoals()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val existing = _uiState.value.goals.find { it.id == id }
                        ?: _uiState.value.selectedGoal
                    val updatedLocal = SavingsGoal(
                        id = id,
                        name = request.name,
                        targetAmount = request.targetAmount,
                        currentAmount = request.currentAmount,
                        deadline = request.deadline,
                        note = request.note,
                        createdAt = existing?.createdAt
                    )
                    val baseList = _uiState.value.goals.ifEmpty { sampleSavingsGoals() }
                    val updatedList = if (baseList.any { it.id == id }) {
                        baseList.map { if (it.id == id) updatedLocal else it }
                    } else {
                        listOf(updatedLocal) + baseList
                    }
                    applyGoals(
                        goals = updatedList,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update {
                        it.copy(
                            selectedGoal = updatedLocal,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = e.message ?: "Gagal memperbarui target tabungan."
                        )
                    }
                }
            }
        }
    }

    fun addDeposit(
        goal: SavingsGoal,
        depositAmount: Long,
        onSuccess: () -> Unit
    ) {
        val newCurrentAmount = goal.currentAmount + depositAmount
        val request = GoalRequest(
            name = goal.name,
            targetAmount = goal.targetAmount,
            currentAmount = newCurrentAmount,
            deadline = goal.deadline,
            note = goal.note
        )
        updateGoal(goal.id, request, onSuccess)
    }

    fun deleteGoal(
        id: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                ApiClient.apiService.deleteGoal(id)
                val remaining = _uiState.value.goals.filterNot { it.id == id }
                applyGoals(
                    goals = remaining,
                    isUsingMockData = false,
                    errorMessage = null
                )
                _uiState.update {
                    it.copy(
                        selectedGoal = null,
                        isSubmitting = false
                    )
                }
                fetchGoals()
                onSuccess()
            } catch (e: Exception) {
                if (shouldFallbackLocally(e)) {
                    val remaining = _uiState.value.goals.filterNot { it.id == id }
                    applyGoals(
                        goals = remaining,
                        isUsingMockData = true,
                        errorMessage = "Server belum terhubung. Perubahan disimpan sementara secara lokal."
                    )
                    _uiState.update {
                        it.copy(
                            selectedGoal = null,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = e.message ?: "Gagal menghapus target tabungan."
                        )
                    }
                }
            }
        }
    }

    fun setStatusFilter(filter: GoalStatusFilter) {
        _uiState.update { current ->
            current.copy(
                selectedStatusFilter = filter,
                displayedGoals = filterAndSortGoals(
                    goals = current.goals,
                    statusFilter = filter,
                    searchQuery = current.searchQuery,
                    sortOption = current.selectedSortOption
                )
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                displayedGoals = filterAndSortGoals(
                    goals = current.goals,
                    statusFilter = current.selectedStatusFilter,
                    searchQuery = query,
                    sortOption = current.selectedSortOption
                )
            )
        }
    }

    fun setSortOption(sortOption: GoalSortOption) {
        _uiState.update { current ->
            current.copy(
                selectedSortOption = sortOption,
                displayedGoals = filterAndSortGoals(
                    goals = current.goals,
                    statusFilter = current.selectedStatusFilter,
                    searchQuery = current.searchQuery,
                    sortOption = sortOption
                )
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun shouldFallbackLocally(exception: Exception): Boolean {
        if (exception is IOException) return true
        if (exception is HttpException && (exception.code() == 404 || exception.code() >= 500)) {
            return true
        }
        return _uiState.value.isUsingMockData
    }

    private fun applyGoals(
        goals: List<SavingsGoal>,
        isUsingMockData: Boolean,
        errorMessage: String?
    ) {
        _uiState.update { current ->
            current.copy(
                goals = goals,
                displayedGoals = filterAndSortGoals(
                    goals = goals,
                    statusFilter = current.selectedStatusFilter,
                    searchQuery = current.searchQuery,
                    sortOption = current.selectedSortOption
                ),
                totalActiveGoals = goals.size,
                totalSavedAllGoals = goals.sumOf { it.currentAmount },
                isLoading = false,
                isUsingMockData = isUsingMockData,
                errorMessage = errorMessage
            )
        }
    }
}
