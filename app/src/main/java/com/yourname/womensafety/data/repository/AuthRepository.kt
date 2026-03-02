package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.api.AuthApiService
import com.yourname.womensafety.data.network.dto.*
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) : BaseRepository() {

    suspend fun loginWithPhone(phoneNumber: String, password: String): NetworkResult<AuthData> {
        val result = safeApiCall {
            authApi.loginWithPhone(PhoneLoginRequest(phoneNumber, password))
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

    /**
     * Step 1: Register with phone. Returns { phone_number, otp_code, expires_in }.
     * The caller must send the SMS via SmsManager using the returned [PhoneRegisterData.otpCode].
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

    /** Resend OTP — returns [ResendOtpData] with the new otp_code; caller sends the SMS. */
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
