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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging

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

    /** Whether auto_sos_enabled is true in the user's settings. */
    private val _autoSosEnabled = MutableStateFlow(true)
    val autoSosEnabled: StateFlow<Boolean> = _autoSosEnabled

    /** True when protection is armed AND autoSos is enabled. */
    private val _autoSosMonitoring = MutableStateFlow(false)
    val autoSosMonitoring: StateFlow<Boolean> = _autoSosMonitoring

    init {
        // Pre-fill greeting name from DataStore immediately so the UI renders
        // Pre-fill greeting name from DataStore on construction so the first
        // rendered frame already has the real name — eliminates the flash of
        // "Good evening Asfalis" that occurred before LaunchedEffect ran.
        viewModelScope.launch {
            val cached = userRepository.getCachedUserName()
            if (!cached.isNullOrEmpty() && _userName.value == null)
                _userName.value = cached
        }

        // Sync FCM token on app launch (best-effort, non-blocking)
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrEmpty()) {
                        viewModelScope.launch {
                            try { userRepository.updateFcmToken(token) } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Loads protection status and settings.
     * Uses cache-first for instant load: reads cached settings immediately,
     * then fires both API calls in parallel as a background refresh.
     */
    fun loadProtectionStatus() {
        viewModelScope.launch {
            // ── Step 1: Apply cached settings immediately (instant, <10ms) ──
            settingsRepository.getCachedSettings()?.let { cached ->
                _shakeSensitivity.value = cached.shakeSensitivity
                _autoSosEnabled.value   = cached.autoSosEnabled
            }

            // ── Step 2: Fire both network calls IN PARALLEL ───────────────
            val protectionDeferred = async {
                try {
                    val response = protectionApi.getProtectionStatus()
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.isActive
                    } else null
                } catch (e: Exception) { null }
            }
            val settingsDeferred = async {
                try {
                    when (val result = settingsRepository.getSettings()) {
                        is NetworkResult.Success -> result.data
                        else -> null
                    }
                } catch (e: Exception) { null }
            }

            // ── Step 3: Apply results as they resolve ─────────────────────
            val activeFromNetwork = protectionDeferred.await()
            if (activeFromNetwork != null) {
                _isProtectionActive.value = activeFromNetwork
            }

            val settingsFromNetwork = settingsDeferred.await()
            if (settingsFromNetwork != null) {
                _shakeSensitivity.value = settingsFromNetwork.shakeSensitivity
                _autoSosEnabled.value   = settingsFromNetwork.autoSosEnabled
            }

            // Compute combined monitoring flag with latest values
            _autoSosMonitoring.value =
                _isProtectionActive.value && _autoSosEnabled.value
        }
    }

    fun toggleProtection(isActive: Boolean) {
        // OPTIMISTIC UPDATE — flip state immediately so the UI feels instant.
        // The network call runs in background and only corrects the value if the
        // server disagrees (which should be rare).
        _isProtectionActive.value = isActive
        _autoSosMonitoring.value  = isActive && _autoSosEnabled.value

        viewModelScope.launch {
            try {
                val response = protectionApi.toggleProtection(ToggleProtectionRequest(isActive))
                if (response.isSuccessful && response.body()?.success == true) {
                    val confirmed = response.body()?.data?.isActive ?: isActive
                    if (confirmed != _isProtectionActive.value) {   // server disagreed
                        _isProtectionActive.value = confirmed
                        _autoSosMonitoring.value  = confirmed && _autoSosEnabled.value
                    }
                }
            } catch (_: Exception) { /* keep optimistic value on network failure */ }
        }
    }

    /** Reload settings from cache (e.g. when resuming from SettingsScreen) */
    fun refreshSettings() {
        viewModelScope.launch {
            settingsRepository.getCachedSettings()?.let { cached ->
                _shakeSensitivity.value = cached.shakeSensitivity
                _autoSosEnabled.value   = cached.autoSosEnabled
                _autoSosMonitoring.value = _isProtectionActive.value && _autoSosEnabled.value
            }
        }
    }

    /**
     * Loads the greeting name.
     * Cache-first: reads persisted user name from DataStore immediately (no flicker),
     * then refreshes from the network in the background.
     */
    fun loadGreeting() {
        viewModelScope.launch {
            // Step 1: Read cached name instantly (persisted across app restarts)
            val cachedName = userRepository.getCachedUserName()
            if (!cachedName.isNullOrEmpty()) {
                _userName.value = cachedName
            }

            // Step 2: Background refresh from network
            when (val result = userRepository.getProfile()) {
                is NetworkResult.Success -> {
                    _userName.value = result.data.fullName.split(" ").firstOrNull()
                }
                else -> Unit  // Keep cached value if network fails
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
