package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.UpdateSettingsRequest
import com.yourname.womensafety.data.network.dto.ToggleProtectionRequest
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.data.network.api.ProtectionApiService
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SettingsRepository
import com.yourname.womensafety.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val protectionApi: ProtectionApiService by lazy {
        RetrofitClient.createService(AppServiceLocator.tokenManager)
    }

    private val userRepository: UserRepository = AppServiceLocator.userRepository
    private val settingsRepository: SettingsRepository = AppServiceLocator.settingsRepository

    private val _isProtectionActive = MutableStateFlow(false)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    /** "low" | "medium" | "high" — read from settings, passed to AutoSosManager. */
    private val _shakeSensitivity = MutableStateFlow("medium")
    val shakeSensitivity: StateFlow<String> = _shakeSensitivity

    /** True when protection is armed — sensors run whenever the shield is armed. */
    private val _autoSosMonitoring = MutableStateFlow(false)
    val autoSosMonitoring: StateFlow<Boolean> = _autoSosMonitoring

    init {
        // _autoSosMonitoring mirrors _isProtectionActive directly.
        // The backend guards /predict with auto_sos_enabled, and we sync that flag
        // via PUT /api/settings every time the shield is toggled.
        viewModelScope.launch {
            _isProtectionActive.collect { active ->
                _autoSosMonitoring.value = active
            }
        }
    }

    fun loadProtectionStatus() {
        viewModelScope.launch {
            try {
                val response = protectionApi.getProtectionStatus()
                if (response.isSuccessful && response.body()?.success == true) {
                    _isProtectionActive.value = response.body()?.data?.isActive ?: _isProtectionActive.value
                }
            } catch (e: Exception) {
                // Ignore — use local state
            }
        }
        // Load shake_sensitivity for AutoSosManager threshold
        viewModelScope.launch {
            when (val result = settingsRepository.getSettings()) {
                is NetworkResult.Success -> _shakeSensitivity.value = result.data.shakeSensitivity
                else -> Unit
            }
        }
    }

    fun toggleProtection(isActive: Boolean) {
        viewModelScope.launch {
            try {
                val response = protectionApi.toggleProtection(ToggleProtectionRequest(isActive))
                if (response.isSuccessful && response.body()?.success == true) {
                    _isProtectionActive.value = response.body()?.data?.isActive ?: isActive
                } else {
                    _isProtectionActive.value = isActive
                }
            } catch (e: Exception) {
                _isProtectionActive.value = isActive
            }
            // Sync auto_sos_enabled to backend so POST /predict requests are accepted.
            // Best-effort — the backend also guards the endpoint, so a failure here
            // just means the ML prediction will return the "not enabled" soft response.
            try {
                settingsRepository.updateSettings(
                    UpdateSettingsRequest(autoSosEnabled = _isProtectionActive.value)
                )
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun loadGreeting() {
        viewModelScope.launch {
            when (val result = userRepository.getProfile()) {
                is NetworkResult.Success -> {
                    _userName.value = result.data.fullName.split(" ").firstOrNull()
                }
                else -> Unit
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel() as T
            }
        }
    }
}
