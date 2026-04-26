package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.api.AuthApiService
import com.yourname.womensafety.data.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.util.Log

class AuthRepository(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) : BaseRepository() {

    companion object {
        private const val TAG = "AuthRepository"
        private val RETRYABLE_CODES = setOf(502, 503, 504)
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS_MS = listOf(5_000L, 10_000L, 20_000L)
    }

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
        return safeAuthApiCall {
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
     * Twilio sends the OTP directly to the user's phone via SMS.
     */
    suspend fun registerWithPhone(
        name: String, phoneNumber: String, password: String, country: String
    ): NetworkResult<PhoneRegisterData> {
        return safeAuthApiCall {
            authApi.registerWithPhone(PhoneRegisterRequest(name, phoneNumber, password, country))
        }
    }

    /**
     * Step 2: Verify the OTP. Returns JWT tokens on success.
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

    /** Resend OTP — rate-limited 3x/15 min. */
    suspend fun resendOtp(phoneNumber: String): NetworkResult<ResendOtpData> {
        return safeAuthApiCall { authApi.resendOtp(ResendOtpRequest(phoneNumber)) }
    }

    /** Forgot password — sends OTP to phone. */
    suspend fun forgotPassword(phoneNumber: String): NetworkResult<ForgotPasswordData> {
        return safeAuthApiCall { authApi.forgotPassword(ForgotPasswordRequest(phoneNumber)) }
    }

    /** Reset password — submit OTP + new password. */
    suspend fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String): NetworkResult<Unit> {
        return safeApiCall { authApi.resetPassword(ResetPasswordRequest(phoneNumber, otpCode, newPassword)) }
    }

    suspend fun logout(): NetworkResult<Unit> {
        val refreshToken = tokenManager.getRefreshToken().first() ?: ""
        val result = safeApiCall { authApi.logout(LogoutRequest(refreshToken)) }
        // Full logout — also clear onboarding/permissions so the next install starts fresh
        tokenManager.clearTokens(isFullLogout = true)
        return result
    }

    suspend fun validateToken(): NetworkResult<ValidateData> {
        return safeAuthApiCall { authApi.validateToken() }
    }

    /**
     * Direct API executor for unwrapped endpoints (auth responses not in ApiResponse wrapper).
     * Retries on 502/503/504 and SocketTimeoutException with exponential back-off.
     */
    protected suspend fun <T> safeAuthApiCall(
        apiCall: suspend () -> retrofit2.Response<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {

        var lastException: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS_MS.getOrElse(attempt - 1) { 20_000L }
                Log.d(TAG, "safeAuthApiCall retry $attempt/$MAX_RETRIES — waiting ${delayMs}ms")
                delay(delayMs)
            }

            return@withContext try {
                val response = apiCall()

                if (response.code() in RETRYABLE_CODES) {
                    Log.w(TAG, "HTTP ${response.code()} — server not ready (attempt $attempt)")
                    if (attempt < MAX_RETRIES) continue
                    return@withContext NetworkResult.Error("SERVER_UNAVAILABLE", "Server is unavailable. Please try again.")
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        NetworkResult.Success(body)
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        NetworkResult.Success(Unit as T)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val apiError = try {
                        com.google.gson.Gson().fromJson(errorBody, ApiResponse::class.java)
                    } catch (_: Exception) { null }
                    val code    = apiError?.resolvedErrorCode    ?: "HTTP_${response.code()}"
                    val message = apiError?.resolvedErrorMessage  ?: response.message()
                    Log.e(TAG, "HTTP Error: $code — $message")
                    NetworkResult.Error(code, message)
                }

            } catch (e: java.net.SocketTimeoutException) {
                Log.w(TAG, "SocketTimeout (attempt $attempt): ${e.message}")
                lastException = e
                if (attempt < MAX_RETRIES) continue
                NetworkResult.Error("NETWORK_ERROR", "Connection timed out. Please check your internet and try again.")

            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "No network: ${e.message}")
                return@withContext NetworkResult.Error("NETWORK_ERROR", "No internet connection. Please check your network.")

            } catch (e: java.net.ConnectException) {
                Log.w(TAG, "ConnectException (attempt $attempt): ${e.message}")
                lastException = e
                if (attempt < MAX_RETRIES) continue
                NetworkResult.Error("NETWORK_ERROR", "Cannot connect to server. Please try again.")

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected: ${e.javaClass.simpleName} — ${e.message}")
                return@withContext NetworkResult.Error("UNKNOWN", e.localizedMessage ?: "An unexpected error occurred.")
            }
        }

        NetworkResult.Error("UNKNOWN", lastException?.localizedMessage ?: "Request failed after retries.")
    }
}
