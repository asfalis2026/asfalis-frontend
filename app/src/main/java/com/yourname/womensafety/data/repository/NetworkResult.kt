package com.yourname.womensafety.data.repository

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : NetworkResult<T>()
    data class Error(val code: String, val message: String) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
