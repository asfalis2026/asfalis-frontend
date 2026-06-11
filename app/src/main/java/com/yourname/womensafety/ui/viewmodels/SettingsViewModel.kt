package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.data.network.api.ProtectionApiService
import com.yourname.womensafety.data.network.dto.ToggleProtectionRequest
import com.yourname.womensafety.data.network.dto.UpdateProfileRequest
import com.yourname.womensafety.data.network.dto.UpdateSettingsRequest
import com.yourname.womensafety.data.network.dto.UserSettings
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SettingsRepository
import com.yourname.womensafety.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val protectionApi: ProtectionApiService by lazy {
        RetrofitClient.createService(AppServiceLocator.tokenManager)
    }

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * One-shot event emitted ONLY after a confirmed successful server save.
     * This prevents the screen from dismissing before the network call completes
     * (the old bug: save was fired optimistically before any network call ran).
     */
    private val _saveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    /** Reflects the persisted auto_sos_enabled value from the backend. */
    private val _autoSosEnabled = MutableStateFlow(false)
    val autoSosEnabled: StateFlow<Boolean> = _autoSosEnabled

    fun loadSettings() {
        viewModelScope.launch {
            // Cache-first: show instantly from disk
            val cached = settingsRepository.getCachedSettings()
            if (cached != null) {
                _settings.value = cached
                _autoSosEnabled.value = cached.autoSosEnabled
            } else {
                _isLoading.value = true
            }

            // Background network refresh
            when (val result = settingsRepository.getSettings()) {
                is NetworkResult.Success -> {
                    _settings.value = result.data
                    _autoSosEnabled.value = result.data.autoSosEnabled
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    if (_settings.value == null) _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun saveSettings(request: UpdateSettingsRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Snapshot current state for rollback on failure
            val snapshot = _settings.value

            // Optimistic UI update so the user sees instant change
            if (snapshot != null) {
                _settings.value = snapshot.copy(
                    shakeSensitivity = request.shakeSensitivity ?: snapshot.shakeSensitivity,
                    autoSosEnabled   = request.autoSosEnabled   ?: snapshot.autoSosEnabled,
                    sosMessage       = request.sosMessage       ?: snapshot.sosMessage
                )
            }
            if (request.autoSosEnabled != null) {
                _autoSosEnabled.value = request.autoSosEnabled
            }

            // ─── Fire ALL network calls in parallel ──────────────────────────

            // 1. Save SOS message to BOTH endpoints:
            //    - PUT /user/sos-message (dedicated endpoint)
            //    - PUT /user/profile with sos_message (backend WhatsApp service reads from profile)
            val sosDeferred = if (!request.sosMessage.isNullOrBlank()) {
                async {
                    try {
                        userRepository.updateSosMessage(request.sosMessage)
                        userRepository.updateProfile(UpdateProfileRequest(sosMessage = request.sosMessage))
                    } catch (_: Exception) { /* best-effort */ }
                }
            } else null

            // 2. Sync protection armed/disarmed to match Auto SOS setting
            val protectionDeferred = if (request.autoSosEnabled != null) {
                async {
                    try {
                        protectionApi.toggleProtection(
                            ToggleProtectionRequest(request.autoSosEnabled)
                        )
                    } catch (_: Exception) { /* best-effort */ }
                }
            } else null

            // 3. Main settings save — this is the authoritative call we wait for
            val settingsDeferred = async { settingsRepository.updateSettings(request) }

            // Await only the authoritative settings call (fixes 3-4s lag).
            // The side-effect calls (sos, protection) continue silently in background.
            val settingsResult = settingsDeferred.await()

            // ─── React to result ─────────────────────────────────────────────
            when (settingsResult) {
                is NetworkResult.Success -> {
                    // Confirm with server's authoritative values
                    _settings.value = settingsResult.data
                    _autoSosEnabled.value = settingsResult.data.autoSosEnabled
                    _isLoading.value = false
                    // Emit ONLY now — screen dismisses AFTER save is confirmed on server
                    _saveSuccess.tryEmit(Unit)
                }
                is NetworkResult.Error -> {
                    // Revert optimistic update
                    if (snapshot != null) _settings.value = snapshot
                    if (snapshot?.autoSosEnabled != null) _autoSosEnabled.value = snapshot.autoSosEnabled
                    _errorMessage.value = "Save failed: ${settingsResult.message}"
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    AppServiceLocator.settingsRepository,
                    AppServiceLocator.userRepository
                ) as T
            }
        }
    }
}
