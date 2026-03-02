package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SosUiState(
    val alertId: String? = null,
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val isCancelled: Boolean = false,
    val errorMessage: String? = null
)

class SosViewModel(
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState

    fun triggerSos(latitude: Double, longitude: Double, triggerType: String = "manual") {
        viewModelScope.launch {
            when (val result = sosRepository.triggerSos(triggerType, latitude, longitude)) {
                is NetworkResult.Success -> {
                    _uiState.value = SosUiState(alertId = result.data.alertId)
                }
                is NetworkResult.Error -> {
                    _uiState.value = SosUiState(errorMessage = result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun sendNow() {
        val alertId = _uiState.value.alertId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            when (sosRepository.sendSosNow(alertId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSending = false, isSent = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        errorMessage = "Failed to dispatch SOS"
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun cancelSos() {
        val alertId = _uiState.value.alertId ?: run {
            // No alertId means SOS wasn't triggered yet — just mark cancelled locally
            _uiState.value = SosUiState(isCancelled = true)
            return
        }
        viewModelScope.launch {
            when (sosRepository.cancelSos(alertId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCancelled = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = "Failed to cancel SOS")
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SosViewModel(AppServiceLocator.sosRepository) as T
            }
        }
    }
}
