package com.yourname.womensafety.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Separate DataStore instance from TokenManager's "asfalis_auth"
private val Context.cacheStore: DataStore<Preferences> by preferencesDataStore(name = "asfalis_cache")

/**
 * AppCache — DataStore-backed stale-while-revalidate cache for frequently read screen data.
 *
 * Stores serialized JSON + a timestamp so callers can decide whether the data is fresh.
 * Uses a separate DataStore from [TokenManager] to keep auth and app data cleanly separated.
 *
 * TTLs:
 *   contacts       → 5 minutes   (changes when user adds/removes contacts)
 *   settings       → 5 minutes   (sensitivity, auto-SOS toggle)
 *   sos_history    → 60 minutes  (historical data, rarely changes in real time)
 *   faqs           → 30 minutes  (static support content)
 *   user_name      → no TTL      (updated on every successful profile fetch)
 */
class AppCache(private val context: Context) {

    companion object {
        // ── DataStore Keys ────────────────────────────────────────────────────
        private val CONTACTS_JSON       = stringPreferencesKey("contacts_json")
        private val CONTACTS_CACHED_AT  = longPreferencesKey("contacts_cached_at")

        private val SOS_HISTORY_JSON      = stringPreferencesKey("sos_history_json")
        private val SOS_HISTORY_CACHED_AT = longPreferencesKey("sos_history_cached_at")

        private val SETTINGS_JSON       = stringPreferencesKey("settings_json")
        private val SETTINGS_CACHED_AT  = longPreferencesKey("settings_cached_at")

        private val FAQS_JSON           = stringPreferencesKey("faqs_json")
        private val FAQS_CACHED_AT      = longPreferencesKey("faqs_cached_at")

        private val USER_NAME           = stringPreferencesKey("user_name")
        private val USER_PROFILE        = stringPreferencesKey("user_profile")

        /** Comma-separated set of alert IDs the user has explicitly deleted locally.
         *  Network refreshes filter these out so deleted items never return. */
        private val SOS_DELETED_IDS     = stringPreferencesKey("sos_deleted_ids")

        // ── Language ─────────────────────────────────────────────────────────────
        private val LANGUAGE_CODE       = stringPreferencesKey("language_code")

        // ── App Lock ──────────────────────────────────────────────────────────
        private val APP_LOCK_ENABLED    = booleanPreferencesKey("app_lock_enabled")
        private val APP_LOCK_PIN        = stringPreferencesKey("app_lock_pin")

        // ── App Tour ──────────────────────────────────────────────────────────
        private val TOUR_COMPLETED      = booleanPreferencesKey("has_completed_tour")

        // ── TTLs ─────────────────────────────────────────────────────────────
        const val TTL_CONTACTS_MS    =  5L * 60 * 1_000   //  5 minutes
        const val TTL_SETTINGS_MS    =  5L * 60 * 1_000   //  5 minutes
        const val TTL_SOS_HISTORY_MS = 60L * 60 * 1_000   // 60 minutes
        const val TTL_FAQS_MS        = 30L * 60 * 1_000   // 30 minutes

        /**
         * Returns true if the cached entry is still within its TTL.
         * @param cachedAt  Epoch millis when the data was stored (0 = never cached).
         * @param ttlMs     Time-to-live in milliseconds.
         */
        fun isFresh(cachedAt: Long, ttlMs: Long): Boolean =
            cachedAt > 0L && (System.currentTimeMillis() - cachedAt) < ttlMs
    }

    // ── Contacts ─────────────────────────────────────────────────────────────

    suspend fun getContactsJson(): String? =
        context.cacheStore.data.map { it[CONTACTS_JSON] }.first()

    suspend fun getContactsCachedAt(): Long =
        context.cacheStore.data.map { it[CONTACTS_CACHED_AT] ?: 0L }.first()

    suspend fun saveContacts(json: String) {
        context.cacheStore.edit {
            it[CONTACTS_JSON]      = json
            it[CONTACTS_CACHED_AT] = System.currentTimeMillis()
        }
    }

    // ── SOS History ───────────────────────────────────────────────────────────

    suspend fun getSosHistoryJson(): String? =
        context.cacheStore.data.map { it[SOS_HISTORY_JSON] }.first()

    suspend fun getSosHistoryCachedAt(): Long =
        context.cacheStore.data.map { it[SOS_HISTORY_CACHED_AT] ?: 0L }.first()

    suspend fun saveSosHistory(json: String) {
        context.cacheStore.edit {
            it[SOS_HISTORY_JSON]      = json
            it[SOS_HISTORY_CACHED_AT] = System.currentTimeMillis()
        }
    }

    /**
     * Removes a single SOS history item from the cache by its alertId.
     * The cached JSON list is read, filtered, and written back atomically.
     */
    suspend fun deleteSosHistoryItem(alertId: String) {
        val currentJson = getSosHistoryJson() ?: return
        try {
            val arr = org.json.JSONArray(currentJson)
            val filtered = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("alert_id").ifEmpty { obj.optString("id") }
                if (id != alertId) filtered.put(obj)
            }
            context.cacheStore.edit {
                it[SOS_HISTORY_JSON]      = filtered.toString()
                it[SOS_HISTORY_CACHED_AT] = System.currentTimeMillis()
            }
        } catch (_: Exception) { /* Corrupt cache — leave untouched */ }
    }

    /**
     * Writes an empty SOS history list to the cache.
     * This makes deletions permanent across navigation and app restarts.
     */
    suspend fun clearSosHistoryCache() {
        context.cacheStore.edit {
            it[SOS_HISTORY_JSON]      = "[]"
            it[SOS_HISTORY_CACHED_AT] = System.currentTimeMillis()
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    suspend fun getSettingsJson(): String? =
        context.cacheStore.data.map { it[SETTINGS_JSON] }.first()

    suspend fun getSettingsCachedAt(): Long =
        context.cacheStore.data.map { it[SETTINGS_CACHED_AT] ?: 0L }.first()

    suspend fun saveSettings(json: String) {
        context.cacheStore.edit {
            it[SETTINGS_JSON]      = json
            it[SETTINGS_CACHED_AT] = System.currentTimeMillis()
        }
    }

    // ── FAQs ─────────────────────────────────────────────────────────────────

    suspend fun getFaqsJson(): String? =
        context.cacheStore.data.map { it[FAQS_JSON] }.first()

    suspend fun getFaqsCachedAt(): Long =
        context.cacheStore.data.map { it[FAQS_CACHED_AT] ?: 0L }.first()

    suspend fun saveFaqs(json: String) {
        context.cacheStore.edit {
            it[FAQS_JSON]      = json
            it[FAQS_CACHED_AT] = System.currentTimeMillis()
        }
    }

    // ── User Name ─────────────────────────────────────────────────────────────

    /** Returns the cached first name used in the Dashboard greeting, or null if never set. */
    suspend fun getUserName(): String? =
        context.cacheStore.data.map { it[USER_NAME] }.first()

    /** Persist the user's first name. Call this after a successful profile fetch. */
    suspend fun saveUserName(name: String) {
        context.cacheStore.edit { it[USER_NAME] = name }
    }

    // ── Full User Profile ─────────────────────────────────────────────────────

    suspend fun getUserProfile(): String? =
        context.cacheStore.data.map { it[USER_PROFILE] }.first()

    suspend fun saveUserProfile(json: String) {
        context.cacheStore.edit { it[USER_PROFILE] = json }
    }

    // ── Cache Invalidation ────────────────────────────────────────────────────

    /** Clears all cached data. Call on logout so a new user sees fresh data. */
    suspend fun clearAll() {
        context.cacheStore.edit { it.clear() }
    }

    // ── SOS Deleted-IDs Blacklist ─────────────────────────────────────────────
    // Persists a set of alertIds the user has deleted. The SosRepository filters
    // network responses through this list so deleted items never return on reload.

    /** Returns the set of permanently-deleted alert IDs. */
    suspend fun getDeletedSosIds(): Set<String> {
        val raw = context.cacheStore.data.map { it[SOS_DELETED_IDS] }.first() ?: return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    /** Adds a single alertId to the permanent deletion blacklist. */
    suspend fun addDeletedSosId(alertId: String) {
        val current = getDeletedSosIds().toMutableSet()
        current.add(alertId)
        context.cacheStore.edit { it[SOS_DELETED_IDS] = current.joinToString(",") }
    }

    /** Marks ALL current history items as deleted (used by Clear All). */
    suspend fun markAllSosDeleted(alertIds: List<String>) {
        val current = getDeletedSosIds().toMutableSet()
        current.addAll(alertIds)
        context.cacheStore.edit { it[SOS_DELETED_IDS] = current.joinToString(",") }
    }

    /** Clears the deletion blacklist (used when the user undoes a Clear All). */
    suspend fun clearDeletedSosIds() {
        context.cacheStore.edit { it.remove(SOS_DELETED_IDS) }
    }

    // ── Language Preference ───────────────────────────────────────────────────

    /**
     * Returns the locally-saved frontend language code ("en", "hi", "bn").
     * Used as an offline fallback before the API response is available.
     */
    suspend fun getLanguageCode(): String =
        context.cacheStore.data.map { it[LANGUAGE_CODE] ?: "en" }.first()

    /** Persists the chosen language code locally. */
    suspend fun saveLanguageCode(code: String) {
        context.cacheStore.edit { it[LANGUAGE_CODE] = code }
    }

    // ── App Lock ──────────────────────────────────────────────────────────────

    suspend fun isAppLockEnabled(): Boolean =
        context.cacheStore.data.map { it[APP_LOCK_ENABLED] ?: false }.first()

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.cacheStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun getAppLockPin(): String? =
        context.cacheStore.data.map { it[APP_LOCK_PIN] }.first()

    suspend fun setAppLockPin(pin: String) {
        context.cacheStore.edit { it[APP_LOCK_PIN] = pin }
    }

    // ── App Tour ──────────────────────────────────────────────────────────────

    /**
     * Returns true if the user has already completed (or permanently skipped)
     * the in-app Quick Tour. Used by [AppNavGraph] to avoid showing it again.
     */
    suspend fun getTourCompleted(): Boolean =
        context.cacheStore.data.map { it[TOUR_COMPLETED] ?: false }.first()

    /**
     * Persist whether the tour has been completed. Pass [true] when the user
     * finishes or skips the tour; pass [false] to reset (for testing / replay).
     */
    suspend fun setTourCompleted(done: Boolean) {
        context.cacheStore.edit { it[TOUR_COMPLETED] = done }
    }

}
