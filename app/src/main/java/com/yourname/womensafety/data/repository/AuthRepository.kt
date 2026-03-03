package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.api.AuthApiService
import com.yourname.womensafety.data.network.dto.*
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) : BaseRepository() {

    suspend fun loginWithPhone(
        phoneNumber: String,
        password: String,
        confirmHandover: Boolean = false
    ): NetworkResult<AuthData> {
        val deviceImei = tokenManager.getOrCreateDeviceId()
        val result = safeApiCall {
            authApi.loginWithPhone(
                PhoneLoginRequest(
                    phoneNumber = phoneNumber,
                    password = password,
                    deviceImei = deviceImei,
                    confirmHandover = confirmHandover
                )
            )
        }
        if (result is NetworkResult.Success) {
            tokenManager.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken,
                userId = result.data.userId,
                sosToken = result.data.sosToken,
                expiresIn = result.data.expiresIn
            )
        }
        return result
    }

    suspend fun getHandsetChangeStatus(phoneNumber: String): NetworkResult<HandsetChangeStatusData> {
        val deviceImei = tokenManager.getOrCreateDeviceId()
        return safeApiCall {
            authApi.handsetChangeStatus(
                HandsetChangeStatusRequest(
                    phoneNumber = phoneNumber,
                    deviceImei = deviceImei
                )
            )
        }
    }

    /**
     * Step 1: Register with phone. Returns { phone_number, expires_in }.
     * Twilio sends the OTP directly to the user's phone via SMS — the frontend does NOT send SMS.
     */
    suspend fun registerWithPhone(
        name: String, phoneNumber: String, password: String, country: String
    ): NetworkResult<PhoneRegisterData> {
        return safeApiCall {
            authApi.registerWithPhone(PhoneRegisterRequest(name, phoneNumber, password, country))
        }
    }

    /**
     * Step 2: Verify the OTP that was sent by the app. Returns JWT tokens on success.
     */
    suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): NetworkResult<AuthData> {
        val result = safeApiCall {
            authApi.verifyPhoneOtp(VerifyPhoneOtpRequest(phoneNumber, otpCode))
        }
        if (result is NetworkResult.Success) {
            tokenManager.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken,
                userId = result.data.userId,
                sosToken = result.data.sosToken,
                expiresIn = result.data.expiresIn
            )
        }
        return result
    }

    /** Resend OTP — Twilio re-sends the SMS to the user. Rate-limited 3×/15 min. */
    suspend fun resendOtp(phoneNumber: String): NetworkResult<ResendOtpData> {
        return safeApiCall { authApi.resendOtp(ResendOtpRequest(phoneNumber)) }
    }

    /** Forgot password — Twilio sends OTP to phone; response contains no code. */
    suspend fun forgotPassword(phoneNumber: String): NetworkResult<ForgotPasswordData> {
        return safeApiCall { authApi.forgotPassword(ForgotPasswordRequest(phoneNumber)) }
    }

    /** Reset password — submit Twilio OTP + new password. */
    suspend fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String): NetworkResult<Unit> {
        return safeApiCall { authApi.resetPassword(ResetPasswordRequest(phoneNumber, otpCode, newPassword)) }
    }

    suspend fun logout(): NetworkResult<Unit> {
        val refreshToken = tokenManager.getRefreshToken().first() ?: ""
        val result = safeApiCall { authApi.logout(LogoutRequest(refreshToken)) }
        tokenManager.clearTokens()
        return result
    }

    suspend fun validateToken(): NetworkResult<ValidateData> {
        return safeApiCall { authApi.validateToken() }
    }
}
