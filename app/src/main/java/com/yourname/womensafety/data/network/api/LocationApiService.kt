package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface LocationApiService {

    @POST("location/update")
    suspend fun updateLocation(
        @Body request: LocationUpdateRequest
    ): Response<ApiResponse<Unit>>

    @GET("location/current")
    suspend fun getCurrentLocation(): Response<ApiResponse<LocationData>>

    @POST("location/share/start")
    suspend fun startSharing(): Response<ApiResponse<SharingData>>

    @POST("location/share/stop")
    suspend fun stopSharing(): Response<ApiResponse<Unit>>
}
