package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.SosHistoryItem
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class SosHistoryUiState {
    data object Loading : SosHistoryUiState()
    data class Success(val items: List<SosHistoryItem>) : SosHistoryUiState()
    data class Error(val message: String, val isAuthError: Boolean = false) : SosHistoryUiState()
}

data class ClearAllUndoEvent(val previousItems: List<SosHistoryItem>)

class SosHistoryViewModel(
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosHistoryUiState>(SosHistoryUiState.Loading)
    val uiState: StateFlow<SosHistoryUiState> = _uiState

    private val _clearAllUndoEvent = MutableSharedFlow<ClearAllUndoEvent>(extraBufferCapacity = 1)
    val clearAllUndoEvent: SharedFlow<ClearAllUndoEvent> = _clearAllUndoEvent.asSharedFlow()

    private val _undoCountdown = MutableStateFlow<Int?>(null)
    val undoCountdown: StateFlow<Int?> = _undoCountdown

    private var clearAllCommitJob: Job? = null
    private var preClearItems: List<SosHistoryItem>? = null

    init { loadHistory() }

    fun loadHistory() {
        viewModelScope.launch {
            // Step 1: Show cached (filtered) items immediately
            val cached = sosRepository.getCachedSosHistory()
            if (!cached.isNullOrEmpty()) {
                _uiState.value = SosHistoryUiState.Success(cached)
            } else {
                _uiState.value = SosHistoryUiState.Loading
            }

            // Step 2: Network refresh — blacklist filtering happens inside getSosHistory()
            try {
                val result = withTimeout(15_000L) { sosRepository.getSosHistory() }
                when (result) {
                    is NetworkResult.Success ->
                        _uiState.value = SosHistoryUiState.Success(result.data)
                    is NetworkResult.Error ->
                        if (cached.isNullOrEmpty())
                            _uiState.value = classifyError(result.code, result.message)
                    is NetworkResult.Loading -> Unit
                }
            } catch (e: TimeoutCancellationException) {
                if (cached.isNullOrEmpty())
                    _uiState.value = SosHistoryUiState.Error("Server took too long. Tap Retry.")
            } catch (e: Exception) {
                if (cached.isNullOrEmpty())
                    _uiState.value = SosHistoryUiState.Error("Could not load history. Tap Retry.")
            }
        }
    }

    /**
     * Delete a single item:
     * 1. Remove from UI immediately (optimistic)
     * 2. Add to persistent blacklist — survives restarts & network refreshes forever
     */
    fun deleteItem(item: SosHistoryItem) {
        val current = _uiState.value
        if (current is SosHistoryUiState.Success) {
            _uiState.value = SosHistoryUiState.Success(current.items - item)
            viewModelScope.launch {
                sosRepository.deleteItemFromCache(item)
            }
        }
    }

    /**
     * Clear All:
     * 1. Wipe UI immediately
     * 2. Emit undo event so screen shows 10-second snackbar
     * 3. After 10s, commit permanently to blacklist
     */
    fun clearAll() {
        val current = _uiState.value
        if (current is SosHistoryUiState.Success && current.items.isNotEmpty()) {
            preClearItems = current.items
            _uiState.value = SosHistoryUiState.Success(emptyList())
            _clearAllUndoEvent.tryEmit(ClearAllUndoEvent(current.items))

            clearAllCommitJob?.cancel()
            clearAllCommitJob = viewModelScope.launch {
                for (i in 10 downTo 1) {
                    _undoCountdown.value = i
                    delay(1_000L)
                }
                _undoCountdown.value = null
                // Commit: add all IDs to the permanent blacklist
                preClearItems?.let { sosRepository.markAllAsDeleted(it) }
                preClearItems = null
                sosRepository.clearHistoryCache()
            }
        }
    }

    /**
     * Undo Clear All — cancels the commit, restores the previous list to UI and cache.
     */
    fun undoClearAll() {
        clearAllCommitJob?.cancel()
        clearAllCommitJob = null
        _undoCountdown.value = null
        val restored = preClearItems ?: return
        preClearItems = null
        _uiState.value = SosHistoryUiState.Success(restored)
        viewModelScope.launch {
            sosRepository.undoClearAll(restored)
        }
    }

    private fun classifyError(code: String, fallbackMessage: String): SosHistoryUiState.Error {
        return when (code) {
            "UNAUTHORIZED", "TOKEN_EXPIRED", "TOKEN_INVALID", "REFRESH_TOKEN_EXPIRED" ->
                SosHistoryUiState.Error("Session expired. Please log in again.", isAuthError = true)
            "NETWORK_ERROR" ->
                SosHistoryUiState.Error("No internet connection. Check your network.")
            "TIMEOUT" ->
                SosHistoryUiState.Error("Server is starting up. Tap Retry in a moment.")
            else ->
                SosHistoryUiState.Error("Could not load history. Tap Retry.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SosHistoryViewModel(AppServiceLocator.sosRepository) as T
            }
        }
    }
}
