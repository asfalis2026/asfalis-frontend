package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {

    @GET("user/profile")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    @PUT("user/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<Unit>>

    @PUT("user/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest
    ): Response<ApiResponse<Unit>>

    /** Dedicated SOS message update. Body: { sos_message } — max 500 chars. */
    @PUT("user/sos-message")
    suspend fun updateSosMessage(
        @Body request: UpdateSosMessageRequest
    ): Response<ApiResponse<SosMessageData>>

    @DELETE("user/account")
    suspend fun deleteAccount(): Response<ApiResponse<Unit>>

    @DELETE("user/{userId}")
    suspend fun deleteUserById(
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>
}
