package com.example.piggybank.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.piggybank.data.ConnectivityObserver
import com.example.piggybank.data.OfflineSyncStore
import com.example.piggybank.data.PendingOpType
import com.example.piggybank.data.isPermanentClientError
import com.example.piggybank.data.isTransientNetworkError
import com.example.piggybank.model.GoalRequest
import com.example.piggybank.model.GoalSortOption
import com.example.piggybank.model.GoalStatusFilter
import com.example.piggybank.model.SavingsGoal
import com.example.piggybank.model.filterAndSortGoals
import com.example.piggybank.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val errorMessage: String? = null
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OfflineSyncStore.getInstance(application)
    private val syncMutex = Mutex()

    private val _uiState = MutableStateFlow(GoalUiState(isLoading = true))
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private val connectivityObserver = ConnectivityObserver(application) {
        fetchGoals()
    }

    init {
        ApiClient.initialize(application)
        val cached = store.getCachedGoals()
        if (cached.isNotEmpty()) {
            applyGoals(cached)
        }
        connectivityObserver.register()
        fetchGoals()
    }

    override fun onCleared() {
        connectivityObserver.unregister()
        super.onCleared()
    }

    fun resolveGoalId(id: Long): Long = store.resolveGoalId(id)

    fun fetchGoals() {
        viewModelScope.launch {
            if (_uiState.value.goals.isEmpty() && connectivityObserver.isOnline()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            syncAndRefreshInternal()
        }
    }

    fun fetchGoalDetail(id: Long) {
        viewModelScope.launch {
            val resolvedId = store.resolveGoalId(id)
            val cached = _uiState.value.goals.find { it.id == resolvedId }
                ?: store.getCachedGoals().find { it.id == resolvedId }
            _uiState.update {
                it.copy(
                    selectedGoal = cached,
                    isDetailLoading = resolvedId > 0L && cached == null && connectivityObserver.isOnline(),
                    errorMessage = null
                )
            }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                _uiState.update { it.copy(isDetailLoading = false) }
                return@launch
            }

            if (store.getPendingGoalOps().any { it.targetId == resolvedId }) {
                _uiState.update { it.copy(isDetailLoading = false) }
                return@launch
            }

            try {
                val remote = ApiClient.apiService.getGoalById(resolvedId)
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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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

            if (!connectivityObserver.isOnline()) {
                applyLocalCreateGoal(request, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingGoalOpsLocked()
                    val created = ApiClient.apiService.createGoal(request)
                    val updatedList = listOf(created) + store.getCachedGoals().filterNot { it.id == created.id }
                    val merged = store.saveRemoteGoalsMerged(updatedList)
                    applyGoals(merged)
                    _uiState.update { it.copy(isSubmitting = false) }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalCreateGoal(request, onSuccess)
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
    }

    fun updateGoal(
        id: Long,
        request: GoalRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val resolvedId = store.resolveGoalId(id)
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                applyLocalUpdateGoal(resolvedId, request, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingGoalOpsLocked()
                    val postFlushId = store.resolveGoalId(resolvedId)
                    val updated = ApiClient.apiService.updateGoal(postFlushId, request)
                    val updatedList = store.getCachedGoals().map { if (it.id == postFlushId) updated else it }
                    val merged = store.saveRemoteGoalsMerged(updatedList)
                    applyGoals(merged)
                    _uiState.update {
                        it.copy(
                            selectedGoal = updated,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalUpdateGoal(store.resolveGoalId(resolvedId), request, onSuccess)
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
            val resolvedId = store.resolveGoalId(id)
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            if (resolvedId < 0L || !connectivityObserver.isOnline()) {
                applyLocalDeleteGoal(resolvedId, onSuccess)
                return@launch
            }

            syncMutex.withLock {
                try {
                    flushPendingGoalOpsLocked()
                    val postFlushId = store.resolveGoalId(resolvedId)
                    ApiClient.apiService.deleteGoal(postFlushId)
                    val remaining = store.getCachedGoals().filterNot { it.id == postFlushId }
                    val merged = store.saveRemoteGoalsMerged(remaining)
                    applyGoals(merged)
                    _uiState.update {
                        it.copy(
                            selectedGoal = null,
                            isSubmitting = false
                        )
                    }
                    onSuccess()
                    refreshFromRemoteLocked()
                } catch (e: Exception) {
                    if (isTransientNetworkError(e)) {
                        applyLocalDeleteGoal(store.resolveGoalId(resolvedId), onSuccess)
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

    private suspend fun syncAndRefreshInternal() {
        syncMutex.withLock {
            if (!connectivityObserver.isOnline()) {
                applyGoals(store.getCachedGoals())
                return
            }
            try {
                flushPendingGoalOpsLocked()
                val remoteGoals = ApiClient.apiService.getGoals()
                val merged = store.saveRemoteGoalsMerged(remoteGoals)
                applyGoals(merged)
            } catch (e: Exception) {
                if (isTransientNetworkError(e)) {
                    applyGoals(store.getCachedGoals())
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun refreshFromRemoteLocked() {
        try {
            val remoteGoals = ApiClient.apiService.getGoals()
            val merged = store.saveRemoteGoalsMerged(remoteGoals)
            applyGoals(merged)
        } catch (e: Exception) {
            if (!isTransientNetworkError(e)) throw e
        }
    }

    private suspend fun flushPendingGoalOpsLocked() {
        val pendingOps = store.getPendingGoalOps()
        for (op in pendingOps) {
            val targetId = store.resolveGoalId(op.targetId)
            when (op.type) {
                PendingOpType.CREATE -> {
                    val req = op.request ?: run {
                        store.removeGoalOp(op.opId)
                        continue
                    }
                    try {
                        val created = ApiClient.apiService.createGoal(req)
                        val updatedCache = store.markGoalCreateSynced(op.opId, targetId, created)
                        if (_uiState.value.selectedGoal?.id == targetId) {
                            _uiState.update { it.copy(selectedGoal = created) }
                        }
                        applyGoals(updatedCache)
                    } catch (e: Exception) {
                        if (isPermanentClientError(e)) store.removeGoalOp(op.opId) else throw e
                    }
                }
                PendingOpType.UPDATE -> {
                    val req = op.request ?: run {
                        store.removeGoalOp(op.opId)
                        continue
                    }
                    if (targetId < 0L) {
                        try {
                            val created = ApiClient.apiService.createGoal(req)
                            store.markGoalCreateSynced(op.opId, targetId, created)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeGoalOp(op.opId) else throw e
                        }
                    } else {
                        try {
                            val updated = ApiClient.apiService.updateGoal(targetId, req)
                            store.markGoalUpdateSynced(op.opId, updated)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeGoalOp(op.opId) else throw e
                        }
                    }
                }
                PendingOpType.DELETE -> {
                    if (targetId < 0L) {
                        store.removeGoalOp(op.opId)
                    } else {
                        try {
                            ApiClient.apiService.deleteGoal(targetId)
                            store.removeGoalOp(op.opId)
                        } catch (e: Exception) {
                            if (isPermanentClientError(e)) store.removeGoalOp(op.opId) else throw e
                        }
                    }
                }
            }
        }
    }

    private fun applyLocalCreateGoal(
        request: GoalRequest,
        onSuccess: () -> Unit
    ) {
        val updatedCache = store.enqueueCreateGoal(request)
        applyGoals(updatedCache)
        _uiState.update { it.copy(isSubmitting = false, errorMessage = null) }
        onSuccess()
    }

    private fun applyLocalUpdateGoal(
        id: Long,
        request: GoalRequest,
        onSuccess: () -> Unit
    ) {
        val (updatedGoal, updatedCache) = store.enqueueUpdateGoal(id, request)
        applyGoals(updatedCache)
        _uiState.update {
            it.copy(
                selectedGoal = updatedGoal,
                isSubmitting = false,
                errorMessage = null
            )
        }
        onSuccess()
    }

    private fun applyLocalDeleteGoal(
        id: Long,
        onSuccess: () -> Unit
    ) {
        val remaining = store.enqueueDeleteGoal(id)
        applyGoals(remaining)
        _uiState.update {
            it.copy(
                selectedGoal = null,
                isSubmitting = false,
                errorMessage = null
            )
        }
        onSuccess()
    }

    private fun applyGoals(goals: List<SavingsGoal>) {
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
                errorMessage = null
            )
        }
    }
}
