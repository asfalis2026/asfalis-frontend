package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.ProtectionStatus
import com.yourname.womensafety.data.network.dto.ToggleProtectionRequest
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.data.network.api.ProtectionApiService
import com.yourname.womensafety.data.repository.BaseRepository
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val protectionApi: ProtectionApiService by lazy {
        RetrofitClient.createService(AppServiceLocator.tokenManager)
    }

    private val userRepository: UserRepository = AppServiceLocator.userRepository

    private val _isProtectionActive = MutableStateFlow(false)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

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
    }

    fun toggleProtection(isActive: Boolean) {
        viewModelScope.launch {
            try {
                val response = protectionApi.toggleProtection(ToggleProtectionRequest(isActive))
                if (response.isSuccessful && response.body()?.success == true) {
                    _isProtectionActive.value = response.body()?.data?.isActive ?: isActive
                } else {
                    // Update locally even if API fails (offline resilience)
                    _isProtectionActive.value = isActive
                }
            } catch (e: Exception) {
                _isProtectionActive.value = isActive
            }
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
