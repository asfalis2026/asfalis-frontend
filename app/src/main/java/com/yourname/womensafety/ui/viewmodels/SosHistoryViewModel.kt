package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.SosHistoryItem
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SosRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class SosHistoryUiState {
    data object Loading : SosHistoryUiState()
    data class Success(val items: List<SosHistoryItem>) : SosHistoryUiState()
    data class Error(val message: String, val isAuthError: Boolean = false) : SosHistoryUiState()
}

class SosHistoryViewModel(
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosHistoryUiState>(SosHistoryUiState.Loading)
    val uiState: StateFlow<SosHistoryUiState> = _uiState

    /** Load history immediately on ViewModel creation — eliminates infinite spinner. */
    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = SosHistoryUiState.Loading
            try {
                val result = withTimeout(15_000L) { sosRepository.getSosHistory() }
                when (result) {
                    is NetworkResult.Success -> _uiState.value = SosHistoryUiState.Success(result.data)
                    is NetworkResult.Error   -> _uiState.value = classifyError(result.code, result.message)
                    is NetworkResult.Loading -> Unit
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = SosHistoryUiState.Error("Server took too long to respond. Tap Retry.")
            } catch (e: Exception) {
                _uiState.value = SosHistoryUiState.Error("Could not load history. Tap Retry.")
            }
        }
    }

    /** Remove a single alert from the local list (UI-only, no backend call). */
    fun deleteItem(item: SosHistoryItem) {
        val current = _uiState.value
        if (current is SosHistoryUiState.Success) {
            _uiState.value = SosHistoryUiState.Success(current.items - item)
        }
    }

    /** Clear all locally visible history entries (UI-only, no backend call). */
    fun clearAll() {
        val current = _uiState.value
        if (current is SosHistoryUiState.Success) {
            _uiState.value = SosHistoryUiState.Success(emptyList())
        }
    }


    /**
     * Maps error codes to user-friendly messages.
     * Critically: only marks isAuthError=true for genuine authentication failures —
     * NOT for backend DB timeouts, server errors, or network issues.
     */
    private fun classifyError(code: String, fallbackMessage: String): SosHistoryUiState.Error {
        return when (code) {
            "UNAUTHORIZED", "TOKEN_EXPIRED", "TOKEN_INVALID", "REFRESH_TOKEN_EXPIRED" ->
                SosHistoryUiState.Error(
                    "Session expired. Please log in again.",
                    isAuthError = true
                )
            "NETWORK_ERROR" ->
                SosHistoryUiState.Error("No internet connection. Check your network.")
            "TIMEOUT" ->
                SosHistoryUiState.Error("Server is starting up. Tap Retry in a moment.")
            else ->
                // Covers HTTP_500, HTTP_503, INTERNAL_ERROR, DB timeouts from backend bug #1, etc.
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
