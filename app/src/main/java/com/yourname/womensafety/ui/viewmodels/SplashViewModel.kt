package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.repository.AuthRepository
import com.yourname.womensafety.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SplashDestination(
    val route: String? = null  // null = still loading
)

class SplashViewModel(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination())
    val destination: StateFlow<SplashDestination> = _destination

    fun resolveStartDestination() {
        viewModelScope.launch {
            val onboardingDone = tokenManager.isOnboardingComplete().first()
            val permissionsGranted = tokenManager.arePermissionsGranted().first()
            val loggedIn = tokenManager.isLoggedIn().first()

            val route = when {
                !onboardingDone -> "onboarding"
                !permissionsGranted -> "permissions"
                loggedIn -> {
                    // Optionally validate the token is still fresh
                    try {
                        val result = authRepository.validateToken()
                        if (result is NetworkResult.Error && result.code == "UNAUTHORIZED") {
                            tokenManager.clearTokens()
                            "login"
                        } else {
                            "dashboard"
                        }
                    } catch (e: Exception) {
                        // Network error — keep user logged in for offline support
                        "dashboard"
                    }
                }
                else -> "login"
            }
            _destination.value = SplashDestination(route)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SplashViewModel(
                    AppServiceLocator.tokenManager,
                    AppServiceLocator.authRepository
                ) as T
            }
        }
    }
}
