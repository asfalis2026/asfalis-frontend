package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.yourname.womensafety.data.network.dto.ApiResponse
import retrofit2.Response

abstract class BaseRepository {

    /**
     * Safely execute a Retrofit API call and wrap the result in [NetworkResult].
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): NetworkResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    @Suppress("UNCHECKED_CAST")
                    val data = body.data ?: (Unit as T) // Unit endpoints have no data field
                    NetworkResult.Success(data, body.message)
                } else {
                    val errorCode = body?.resolvedErrorCode ?: "UNKNOWN"
                    val errorMsg  = body?.resolvedErrorMessage ?: "Unknown error"
                    NetworkResult.Error(errorCode, errorMsg)
                }
            } else {
                // 4xx / 5xx — parse error from the JSON body
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    Gson().fromJson(errorBody, ApiResponse::class.java)
                } catch (e: Exception) { null }

                NetworkResult.Error(
                    code    = apiError?.resolvedErrorCode ?: "HTTP_${response.code()}",
                    message = apiError?.resolvedErrorMessage ?: response.message()
                )
            }
        } catch (e: java.net.UnknownHostException) {
            NetworkResult.Error("NETWORK_ERROR", "No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Error("TIMEOUT", "Request timed out")
        } catch (e: Exception) {
            NetworkResult.Error("UNKNOWN", e.localizedMessage ?: "An unexpected error occurred")
        }
    }
}
