package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SettingsApiService {

    @GET("settings")
    suspend fun getSettings(): Response<ApiResponse<UserSettings>>

    @PUT("settings")
    suspend fun updateSettings(
        @Body request: UpdateSettingsRequest
    ): Response<ApiResponse<UserSettings>>
}
