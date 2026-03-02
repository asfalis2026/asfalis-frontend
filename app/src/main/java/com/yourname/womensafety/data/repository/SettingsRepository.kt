package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.SettingsApiService
import com.yourname.womensafety.data.network.dto.*

class SettingsRepository(
    private val settingsApi: SettingsApiService
) : BaseRepository() {

    suspend fun getSettings(): NetworkResult<UserSettings> {
        return safeApiCall { settingsApi.getSettings() }
    }

    suspend fun updateSettings(request: UpdateSettingsRequest): NetworkResult<UserSettings> {
        return safeApiCall { settingsApi.updateSettings(request) }
    }
}
