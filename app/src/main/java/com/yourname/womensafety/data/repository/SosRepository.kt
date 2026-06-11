package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.network.api.SosApiService
import com.yourname.womensafety.data.network.dto.*

class SosRepository(
    private val sosApi: SosApiService,
    private val appCache: AppCache
) : BaseRepository() {

    private val gson = Gson()
    private val historyListType = object : TypeToken<List<SosHistoryItem>>() {}.type

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /**
     * Returns the cached SOS history list filtered through the deleted-IDs blacklist,
     * or null if cache is empty.
     */
    suspend fun getCachedSosHistory(): List<SosHistoryItem>? {
        val json = appCache.getSosHistoryJson() ?: return null
        val all = try { gson.fromJson<List<SosHistoryItem>>(json, historyListType) } catch (e: Exception) { return null }
        val deleted = appCache.getDeletedSosIds()
        return if (deleted.isEmpty()) all else all.filter { it.displayId !in deleted }
    }

    /** Permanently adds an alert to the deletion blacklist so it never resurfaces. */
    suspend fun deleteItemFromCache(item: SosHistoryItem) {
        val id = item.displayId
        if (id.isNotEmpty()) appCache.addDeletedSosId(id)
    }

    /**
     * Marks all provided items as deleted in the blacklist.
     * Used by Clear All — survives network refreshes and app restarts.
     */
    suspend fun markAllAsDeleted(items: List<SosHistoryItem>) {
        val ids = items.map { it.displayId }.filter { it.isNotEmpty() }
        if (ids.isNotEmpty()) appCache.markAllSosDeleted(ids)
    }

    /**
     * Removes the deletion blacklist entirely (used for undo Clear All).
     * Also restores the cached JSON to the pre-clear list.
     */
    suspend fun undoClearAll(previousItems: List<SosHistoryItem>) {
        appCache.clearDeletedSosIds()
        appCache.saveSosHistory(gson.toJson(previousItems))
    }

    /** Writes an empty list to cache (legacy helper, kept for safety). */
    suspend fun clearHistoryCache() {
        appCache.clearSosHistoryCache()
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    suspend fun triggerSos(
        triggerType: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null,
        sosMessage: String? = null
    ): NetworkResult<SosAlertData> {
        return safeApiCall {
            sosApi.triggerSos(SosTriggerRequest(triggerType, latitude, longitude, accuracy, sosMessage))
        }
    }

    suspend fun sendSosNow(alertId: String): NetworkResult<SosSendNowData> {
        return safeApiCall { sosApi.sendSosNow(SosSendNowRequest(alertId)) }
    }

    /**
     * Cancel an active SOS alert.
     * [alertId] may be null — the backend looks up the caller's most-recent active countdown.
     */
    suspend fun cancelSos(alertId: String? = null): NetworkResult<Unit> {
        return safeApiCall { sosApi.cancelSos(SosCancelRequest(alertId)) }
    }

    suspend fun markUserSafe(alertId: String): NetworkResult<SosSafeData> {
        return safeApiCall { sosApi.markUserSafe(SosSafeRequest(alertId)) }
    }

    /**
     * Fetches SOS history from the network, filters out deleted IDs,
     * updates the cache, and returns the filtered list.
     */
    suspend fun getSosHistory(): NetworkResult<List<SosHistoryItem>> {
        val result = safeApiCall { sosApi.getSosHistory() }
        if (result is NetworkResult.Success) {
            val deleted = appCache.getDeletedSosIds()
            val filtered = if (deleted.isEmpty()) result.data
                           else result.data.filter { it.displayId !in deleted }
            // Save only the filtered list to cache
            appCache.saveSosHistory(gson.toJson(filtered))
            return NetworkResult.Success(filtered)
        }
        return result
    }

    suspend fun getSosCountdown(alertId: String): NetworkResult<SosCountdownData> {
        return safeApiCall { sosApi.getSosCountdown(alertId) }
    }

    suspend fun testWhatsApp(toNumber: String, message: String): NetworkResult<Unit> {
        return safeApiCall { sosApi.testWhatsApp(TestWhatsAppRequest(toNumber, message)) }
    }
}
