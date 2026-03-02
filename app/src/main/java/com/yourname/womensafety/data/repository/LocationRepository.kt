package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.LocationApiService
import com.yourname.womensafety.data.network.dto.*

class LocationRepository(
    private val locationApi: LocationApiService
) : BaseRepository() {

    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null,
        isSharing: Boolean = false
    ): NetworkResult<Unit> {
        return safeApiCall {
            locationApi.updateLocation(LocationUpdateRequest(latitude, longitude, accuracy, isSharing))
        }
    }

    suspend fun getCurrentLocation(): NetworkResult<LocationData> {
        return safeApiCall { locationApi.getCurrentLocation() }
    }

    suspend fun startSharing(): NetworkResult<SharingData> {
        return safeApiCall { locationApi.startSharing() }
    }

    suspend fun stopSharing(): NetworkResult<Unit> {
        return safeApiCall { locationApi.stopSharing() }
    }
}
