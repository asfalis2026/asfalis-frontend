package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ProtectionApiService {

    @POST("protection/toggle")
    suspend fun toggleProtection(
        @Body request: ToggleProtectionRequest
    ): Response<ApiResponse<ProtectionStatus>>

    @GET("protection/status")
    suspend fun getProtectionStatus(): Response<ApiResponse<ProtectionStatus>>

    @POST("protection/sensor-data")
    suspend fun sendSensorData(
        @Body request: SensorDataRequest
    ): Response<ApiResponse<SensorAnalysisResult>>

    /** ML danger prediction from a raw sensor window (40 [x,y,z] readings). */
    @POST("protection/predict")
    suspend fun predict(
        @Body request: SensorWindowRequest
    ): Response<ApiResponse<PredictionResult>>

    @POST("protection/collect")
    suspend fun collectData(
        @Body request: SensorTrainingRequest
    ): Response<ApiResponse<Unit>>
}
