package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.yourname.womensafety.data.network.dto.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response
import android.util.Log

abstract class BaseRepository {

    companion object {
        private const val TAG = "BaseRepository"
        /** Max automatic retries for server-side transient errors (503, timeout). */
        private const val MAX_RETRIES = 3
        /** Delays between retries: 5s, 10s, 20s — gives Render time to spin up. */
        private val RETRY_DELAYS_MS = listOf(5_000L, 10_000L, 20_000L)
        /** HTTP codes that are safe to retry (server not ready yet). */
        private val RETRYABLE_HTTP_CODES = setOf(502, 503, 504)
    }

    /**
     * Safely executes a Retrofit API call, wrapping the result in [NetworkResult].
     *
     * Automatically retries up to [MAX_RETRIES] times for:
     *  - HTTP 502/503/504 (server not ready — Render cold-start)
     *  - SocketTimeoutException (connection timed out before server responded)
     *
     * Exponential back-off (5s → 10s → 20s) gives Render's free-tier server time to
     * spin up without blocking the UI thread.
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {

        var lastException: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            // Wait before each retry (not before the first attempt)
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS_MS.getOrElse(attempt - 1) { 20_000L }
                Log.d(TAG, "Retry attempt $attempt/$MAX_RETRIES — waiting ${delayMs}ms")
                delay(delayMs)
            }

            return@withContext try {
                val response = apiCall()

                if (response.code() in RETRYABLE_HTTP_CODES) {
                    Log.w(TAG, "HTTP ${response.code()} — server not ready (attempt $attempt)")
                    if (attempt < MAX_RETRIES) continue
                    // All retries exhausted
                    return@withContext NetworkResult.Error(
                        "SERVER_UNAVAILABLE",
                        "Server is unavailable. Please try again in a moment."
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.isSuccess) {
                        @Suppress("UNCHECKED_CAST")
                        val data = body.data ?: (Unit as T)
                        NetworkResult.Success(data, body.message)
                    } else {
                        val errorCode = body?.resolvedErrorCode ?: "UNKNOWN"
                        val errorMsg  = body?.resolvedErrorMessage ?: "Unknown error"
                        Log.e(TAG, "API Error: $errorCode — $errorMsg")
                        NetworkResult.Error(errorCode, errorMsg)
                    }
                } else {
                    // 4xx / 5xx — parse error body
                    val errorBodyStr = response.errorBody()?.string()
                    val apiError = try {
                        Gson().fromJson(errorBodyStr, ApiResponse::class.java)
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
                // All retries exhausted — surface a clean error
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
                Log.e(TAG, "Unexpected exception: ${e.javaClass.simpleName} — ${e.message}")
                return@withContext NetworkResult.Error("UNKNOWN", e.localizedMessage ?: "An unexpected error occurred.")
            }
        }

        // Should never reach here, but safe fallback
        NetworkResult.Error("UNKNOWN", lastException?.localizedMessage ?: "Request failed after retries.")
    }
}
