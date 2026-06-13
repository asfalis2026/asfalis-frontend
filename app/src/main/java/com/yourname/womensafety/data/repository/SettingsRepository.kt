package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.network.api.SettingsApiService
import com.yourname.womensafety.data.network.dto.*

class SettingsRepository(
    private val settingsApi: SettingsApiService,
    private val appCache: AppCache
) : BaseRepository() {

    private val gson = Gson()

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /** Returns the cached [UserSettings], or null if cache is empty. */
    suspend fun getCachedSettings(): UserSettings? {
        val json = appCache.getSettingsJson() ?: return null
        return try { gson.fromJson(json, UserSettings::class.java) } catch (e: Exception) { null }
    }

    suspend fun cacheIsFresh(): Boolean =
        AppCache.isFresh(appCache.getSettingsCachedAt(), AppCache.TTL_SETTINGS_MS)

    // ── API calls ─────────────────────────────────────────────────────────────

    /**
     * Fetches settings from the network and updates the cache.
     * For instant load call [getCachedSettings] first, then this in background.
     */
    suspend fun getSettings(): NetworkResult<UserSettings> {
        val result = safeApiCall { settingsApi.getSettings() }
        if (result is NetworkResult.Success) {
            appCache.saveSettings(gson.toJson(result.data))
        }
        return result
    }

    suspend fun updateSettings(request: UpdateSettingsRequest): NetworkResult<UserSettings> {
        val result = safeApiCall { settingsApi.updateSettings(request) }
        // Refresh cache after update
        if (result is NetworkResult.Success) {
            appCache.saveSettings(gson.toJson(result.data))
        }
        return result
    }
}
