package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.SosHistoryItem
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SosHistoryUiState {
    data object Loading : SosHistoryUiState()
    data class Success(val items: List<SosHistoryItem>) : SosHistoryUiState()
    data class Error(val message: String) : SosHistoryUiState()
}

class SosHistoryViewModel(
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosHistoryUiState>(SosHistoryUiState.Loading)
    val uiState: StateFlow<SosHistoryUiState> = _uiState

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = SosHistoryUiState.Loading
            when (val result = sosRepository.getSosHistory()) {
                is NetworkResult.Success -> _uiState.value = SosHistoryUiState.Success(result.data)
                is NetworkResult.Error -> _uiState.value = SosHistoryUiState.Error(result.message)
                is NetworkResult.Loading -> Unit
            }
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
