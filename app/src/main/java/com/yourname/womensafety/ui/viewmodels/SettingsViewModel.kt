package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.UpdateSettingsRequest
import com.yourname.womensafety.data.network.dto.UserSettings
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = settingsRepository.getSettings()) {
                is NetworkResult.Success -> {
                    _settings.value = result.data
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun saveSettings(request: UpdateSettingsRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = settingsRepository.updateSettings(request)) {
                is NetworkResult.Success -> {
                    _settings.value = result.data
                    _saveSuccess.value = true
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSaveSuccess() { _saveSuccess.value = false }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(AppServiceLocator.settingsRepository) as T
            }
        }
    }
}
