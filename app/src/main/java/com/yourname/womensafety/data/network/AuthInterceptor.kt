package com.yourname.womensafety.data.network

import android.os.Build
import com.google.gson.Gson
import com.yourname.womensafety.data.SessionManager
import com.yourname.womensafety.data.local.TokenManager
import com.yourname.womensafety.data.network.dto.ApiResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    /** Only one coroutine may refresh the token at a time. */
    private val refreshMutex = Mutex()

    private val publicPaths = listOf(
        "auth/login", "auth/register",
        "auth/resend-otp", "auth/forgot-password", "auth/reset-password",
        "auth/google", "auth/refresh",
        "auth/verify-phone-otp", "auth/login/phone",
        "health", "device/alert", "protection/train-model"
    )

    /**
     * 401 error codes where refresh should NOT be attempted.
     * Covers tampered/stolen tokens — force logout immediately.
     */
    private val noRefreshCodes = setOf(
        "TOKEN_INVALID", "REFRESH_TOKEN_EXPIRED",
        "REFRESH_TOKEN_REUSED", "REFRESH_TOKEN_INVALID", "UNAUTHORIZED"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val deviceId = runBlocking { tokenManager.getOrCreateDeviceId() }
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        val baseRequest = originalRequest.newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Device-Model", deviceModel)
            .build()

        val isPublic = publicPaths.any { baseRequest.url.encodedPath.contains(it) }
        if (isPublic) return chain.proceed(baseRequest)

        // SOS trigger uses the long-lived sos_token, not the access_token
        val isSosTrigger = baseRequest.url.encodedPath.contains("sos/trigger")

        // --- Proactive refresh: if access token expires within 60s, refresh now ---
        if (!isSosTrigger) {
            runBlocking {
                val expiresAt = tokenManager.getTokenExpiresAt().first()
                if (expiresAt > 0L && System.currentTimeMillis() + 60_000 >= expiresAt) {
                    refreshMutex.withLock {
                        // Re-check inside the lock in case a parallel call already refreshed
                        val currentExpiry = tokenManager.getTokenExpiresAt().first()
                        if (System.currentTimeMillis() + 60_000 >= currentExpiry) {
                            val refreshToken = tokenManager.getRefreshToken().first()
                            if (refreshToken != null) tokenManager.refreshAccessToken(refreshToken)
                        }
                    }
                }
            }
        }

        val token = runBlocking {
            if (isSosTrigger) {
                // Prefer sos_token; fall back to access_token if sos_token not stored yet
                tokenManager.getSosToken().first() ?: tokenManager.getAccessToken().first()
            } else {
                tokenManager.getAccessToken().first()
            }
        }

        val authenticatedRequest = if (token != null) {
            baseRequest.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            baseRequest
        }

        val response = chain.proceed(authenticatedRequest)

        if (response.code != 401) return response

        // --- Handle 401 ---

        // For SOS trigger: the sos_token itself is expired (30-day) → force re-login
        if (isSosTrigger) {
            runBlocking { tokenManager.clearTokens() }
            SessionManager.onSessionExpired()
            return response
        }

        // Parse the error code to decide whether to attempt a refresh
        val errorBodyStr = response.peekBody(Long.MAX_VALUE).string()
        val errorCode = try {
            Gson().fromJson(errorBodyStr, ApiResponse::class.java)?.resolvedErrorCode ?: "TOKEN_EXPIRED"
        } catch (e: Exception) { "TOKEN_EXPIRED" }

        if (errorCode in noRefreshCodes) {
            // Tampered or already-rotated token — do NOT refresh, force logout
            runBlocking { tokenManager.clearTokens() }
            SessionManager.onSessionExpired()
            return response
        }

        // TOKEN_EXPIRED (or unknown) — attempt a silent refresh, guarded by Mutex
        response.close()
        val oldToken = token
        val newToken = runBlocking {
            refreshMutex.withLock {
                // If a parallel call already refreshed, the stored token has changed — reuse it
                val latestToken = tokenManager.getAccessToken().first()
                if (latestToken != null && latestToken != oldToken) {
                    latestToken
                } else {
                    val refreshToken = tokenManager.getRefreshToken().first()
                    if (refreshToken != null) tokenManager.refreshAccessToken(refreshToken)
                    else null
                }
            }
        }

        return if (newToken != null) {
            chain.proceed(
                baseRequest.newBuilder().header("Authorization", "Bearer $newToken").build()
            )
        } else {
            runBlocking { tokenManager.clearTokens() }
            SessionManager.onSessionExpired()
            // Return a synthetic 401 so the caller knows the request failed
            chain.proceed(baseRequest)
        }
    }
}
