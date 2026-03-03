package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic wrapper that handles two backend response conventions:
 *
 * auth.py  →  { "status": "success"|"error", "data": {...}, "message": "...", "error_code": "CODE" }
 * user.py  →  { "success": true|false,         "data": {...}, "message": "...", "error": { "code": "CODE", "message": "..." } }
 */
data class ApiResponse<T>(
    // user.py style
    @SerializedName("success")    val success: Boolean? = null,
    // auth.py style
    @SerializedName("status")     val status: String? = null,
    @SerializedName("data")       val data: T? = null,
    @SerializedName("message")    val message: String? = null,
    // user.py nested error object
    @SerializedName("error")      val error: ApiError? = null,
    // auth.py flat error code
    @SerializedName("error_code") val errorCode: String? = null
) {
    /** True for both `success: true` and `status: "success"`. */
    val isSuccess: Boolean get() = success == true || status == "success"

    /** Unified error code — checks nested object first, then flat field. */
    val resolvedErrorCode: String? get() = error?.code ?: errorCode

    /** Unified error message — checks nested object first, then top-level message. */
    val resolvedErrorMessage: String? get() = error?.message ?: message
}

data class ApiError(
    @SerializedName("code")    val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: Map<String, Any>? = null
)
