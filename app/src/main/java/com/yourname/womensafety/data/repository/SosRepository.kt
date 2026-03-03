package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.SosApiService
import com.yourname.womensafety.data.network.dto.*

class SosRepository(
    private val sosApi: SosApiService
) : BaseRepository() {

    suspend fun triggerSos(
        triggerType: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null
    ): NetworkResult<SosAlertData> {
        return safeApiCall {
            sosApi.triggerSos(SosTriggerRequest(triggerType, latitude, longitude, accuracy))
        }
    }

    suspend fun sendSosNow(alertId: String): NetworkResult<SosAlertData> {
        return safeApiCall { sosApi.sendSosNow(SosSendNowRequest(alertId)) }
    }

    suspend fun cancelSos(alertId: String): NetworkResult<Unit> {
        return safeApiCall { sosApi.cancelSos(SosCancelRequest(alertId)) }
    }

    suspend fun markUserSafe(alertId: String): NetworkResult<Unit> {
        return safeApiCall { sosApi.markUserSafe(SosSafeRequest(alertId)) }
    }

    suspend fun getSosHistory(): NetworkResult<List<SosHistoryItem>> {
        return safeApiCall { sosApi.getSosHistory() }
    }
}
