package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.SecurityPolicyManager
import com.yourname.womensafety.data.network.dto.HandsetChangeStatusData
import com.yourname.womensafety.data.repository.AuthRepository
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HandsetTransferUiState(
    val phoneNumber: String,
    val requestId: String? = null,
    val eligibleAt: String? = null,
    val remainingSeconds: Int? = null,
    val confirmHandoverRequired: Boolean = false
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    /** Set after Step 1 register — signals to navigate to OTP screen. */
    val registeredPhone: String? = null,
    /** Returned on PHONE_NOT_VERIFIED during login — navigate to OTP screen. */
    val unverifiedPhone: String? = null,
    /** Set after forgotPassword succeeds — navigate to reset-password screen. */
    val forgotPasswordSent: String? = null,
    /** True after resendOtp succeeds — Twilio re-sent the SMS; show a toast. */
    val otpResent: Boolean = false,
    val handsetTransfer: HandsetTransferUiState? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ─── Login with Phone ────────────────────────────────────────────────────
    fun loginWithPhone(phoneNumber: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.loginWithPhone(phoneNumber, password)) {
                is NetworkResult.Success -> {
                    fetchSecurityPolicy()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    // If phone not verified, surface the phone number so UI navigates to OTP
                    if (result.code == "PHONE_NOT_VERIFIED") {
                        _uiState.value = AuthUiState(unverifiedPhone = phoneNumber)
                    } else if (
                        result.code == "HANDSET_CHANGE_PENDING" ||
                        result.code == "HANDSET_CHANGE_CONFIRMATION_REQUIRED"
                    ) {
                        val transferState = loadHandsetTransferState(phoneNumber, pendingCode = result.code)
                        _uiState.value = AuthUiState(handsetTransfer = transferState)
                    } else {
                        val friendlyMessage = when (result.code) {
                            "PHONE_NOT_VERIFIED" -> "Please verify your phone number first."
                            "UNAUTHORIZED"       -> "Invalid phone number or password."
                            "RATE_LIMITED"       -> "Too many attempts. Try again later."
                            else                 -> result.message
                        }
                        _uiState.value = AuthUiState(errorMessage = friendlyMessage)
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ─── Register with Phone (Step 1) ────────────────────────────────────────
    fun registerWithPhone(name: String, phoneNumber: String, password: String, country: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.registerWithPhone(name, phoneNumber, password, country)) {
                is NetworkResult.Success -> {
                    // Twilio has sent the OTP SMS — navigate to OTP screen
                    _uiState.value = AuthUiState(registeredPhone = result.data.phoneNumber)
                }
                is NetworkResult.Error -> {
                    val friendlyMessage = when (result.code) {
                        "CONFLICT"          -> "This phone number is already registered. Try logging in."
                        "VALIDATION_ERROR"  -> "Please check your details and try again."
                        "RATE_LIMITED"      -> "Too many attempts. Try again later."
                        else                 -> result.message
                    }
                    _uiState.value = AuthUiState(errorMessage = friendlyMessage)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ─── Verify Phone OTP (Step 2) ───────────────────────────────────────────
    fun verifyPhoneOtp(phoneNumber: String, otpCode: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.verifyPhoneOtp(phoneNumber, otpCode)) {
                is NetworkResult.Success -> {
                    fetchSecurityPolicy()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    val friendlyMessage = when (result.code) {
                        "OTP_INVALID"      -> "Incorrect or expired OTP. Try again."
                        "ALREADY_VERIFIED" -> "Your number is already verified. Please log in."
                        else               -> result.message
                    }
                    _uiState.value = AuthUiState(errorMessage = friendlyMessage)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ─── Resend OTP ──────────────────────────────────────────────────────────
    fun resendOtp(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, otpResent = false)
            when (val result = authRepository.resendOtp(phoneNumber)) {
                is NetworkResult.Success -> {
                    // Twilio re-sent the SMS — just signal the UI to show a toast
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpResent = true
                    )
                }
                is NetworkResult.Error -> {
                    val friendlyMessage = when (result.code) {
                        "RATE_LIMITED"      -> "Too many attempts. Please wait and try again."
                        "ALREADY_VERIFIED"  -> "Your number is already verified. Please log in."
                        else                -> result.message
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyMessage)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ─── Forgot Password ─────────────────────────────────────────────────────
    fun forgotPassword(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.forgotPassword(phoneNumber)) {
                is NetworkResult.Success -> {
                    // Twilio sent the reset OTP — navigate to reset-password screen
                    _uiState.value = AuthUiState(forgotPasswordSent = phoneNumber)
                }
                is NetworkResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ─── Reset Password ─────────────────────────────────────────────────────────
    fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.resetPassword(phoneNumber, otpCode, newPassword)) {
                is NetworkResult.Success -> _uiState.value = AuthUiState(isSuccess = true)
                is NetworkResult.Error -> {
                    val friendlyMessage = when (result.code) {
                        "OTP_INVALID"      -> "Incorrect or expired code. Try again."
                        "NOT_FOUND"        -> "Phone number not found."
                        "VALIDATION_ERROR" -> "Password is too weak. Use at least 8 characters."
                        else               -> result.message
                    }
                    _uiState.value = AuthUiState(errorMessage = friendlyMessage)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isSuccess = false,
            registeredPhone = null,
            unverifiedPhone = null,
            forgotPasswordSent = null,
            otpResent = false
        )
    }

    fun clearHandsetTransferState() {
        _uiState.value = _uiState.value.copy(handsetTransfer = null)
    }

    fun confirmHandsetTransfer(phoneNumber: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.loginWithPhone(phoneNumber, password, confirmHandover = true)) {
                is NetworkResult.Success -> {
                    fetchSecurityPolicy()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    if (result.code == "HANDSET_CHANGE_PENDING" || result.code == "HANDSET_CHANGE_CONFIRMATION_REQUIRED") {
                        val transferState = loadHandsetTransferState(phoneNumber, pendingCode = result.code)
                        _uiState.value = AuthUiState(handsetTransfer = transferState)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadHandsetTransferState(
        phoneNumber: String,
        pendingCode: String
    ): HandsetTransferUiState {
        val statusResult = authRepository.getHandsetChangeStatus(phoneNumber)
        return if (statusResult is NetworkResult.Success) {
            val data = statusResult.data
            HandsetTransferUiState(
                phoneNumber = phoneNumber,
                requestId = data.requestId,
                eligibleAt = data.eligibleAt,
                remainingSeconds = data.remainingSeconds,
                confirmHandoverRequired = data.confirmHandoverRequired || pendingCode == "HANDSET_CHANGE_CONFIRMATION_REQUIRED"
            )
        } else {
            HandsetTransferUiState(
                phoneNumber = phoneNumber,
                confirmHandoverRequired = pendingCode == "HANDSET_CHANGE_CONFIRMATION_REQUIRED"
            )
        }
    }

    private suspend fun fetchSecurityPolicy() {
        when (val result = userRepository.getSecurityPolicy()) {
            is NetworkResult.Success -> {
                SecurityPolicyManager.update(
                    enabled = result.data.screenshotProtection.enabled,
                    protectedScreens = result.data.screenshotProtection.protectedScreens
                )
            }
            else -> Unit
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(
                    authRepository = AppServiceLocator.authRepository,
                    userRepository = AppServiceLocator.userRepository
                ) as T
            }
        }
    }
}
