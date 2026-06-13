package com.yourname.womensafety.data.local

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourname.womensafety.data.network.dto.RefreshRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "asfalis_auth")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val SOS_TOKEN = stringPreferencesKey("sos_token")
        private val TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        private val USER_ID = stringPreferencesKey("user_id")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val IS_LANGUAGE_SELECTED = booleanPreferencesKey("is_language_selected")
        private val PERMISSIONS_GRANTED = booleanPreferencesKey("permissions_granted")
        private val DEVICE_ID = stringPreferencesKey("device_id")
    }

    // --- ACCESS TOKEN ---
    fun getAccessToken(): Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    // --- REFRESH TOKEN ---
    fun getRefreshToken(): Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { it[REFRESH_TOKEN] = token }
    }

    // --- SOS TOKEN ---
    fun getSosToken(): Flow<String?> = context.dataStore.data.map { it[SOS_TOKEN] }

    // --- TOKEN EXPIRY ---
    fun getTokenExpiresAt(): Flow<Long> = context.dataStore.data.map { it[TOKEN_EXPIRES_AT] ?: 0L }

    // --- SAVE ALL TOKENS (after login / OTP verify) ---
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        sosToken: String? = null,
        expiresIn: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USER_ID] = userId
            prefs[IS_LOGGED_IN] = true
            if (sosToken != null) prefs[SOS_TOKEN] = sosToken
            if (expiresIn != null)
                prefs[TOKEN_EXPIRES_AT] = System.currentTimeMillis() + (expiresIn * 1000L)
        }
    }

    // --- LOGIN STATE ---
    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    fun getUserId(): Flow<String?> = context.dataStore.data.map { it[USER_ID] }

    // --- ONBOARDING STATE ---
    fun isLanguageSelected(): Flow<Boolean> =
        context.dataStore.data.map { it[IS_LANGUAGE_SELECTED] ?: false }

    suspend fun setLanguageSelected() {
        context.dataStore.edit { it[IS_LANGUAGE_SELECTED] = true }
    }

    fun isOnboardingComplete(): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    // --- PERMISSIONS STATE ---
    fun arePermissionsGranted(): Flow<Boolean> =
        context.dataStore.data.map { it[PERMISSIONS_GRANTED] ?: false }

    suspend fun setPermissionsGranted() {
        context.dataStore.edit { it[PERMISSIONS_GRANTED] = true }
    }

    // --- LOGOUT ---
    /**
     * Clears auth tokens from DataStore.
     *
     * @param isFullLogout If true (explicit user-initiated logout), also clears the
     *   onboarding and permissions flags so the next login correctly goes through
     *   the full setup flow. If false (token refresh failure), only auth tokens are
     *   cleared — the user keeps their onboarding state.
     */
    suspend fun clearTokens(isFullLogout: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(SOS_TOKEN)
            prefs.remove(TOKEN_EXPIRES_AT)
            prefs.remove(USER_ID)
            prefs[IS_LOGGED_IN] = false
            // Only wipe onboarding/permissions on explicit logout.
            // Do NOT clear these on token refresh failure — we don't want to
            // force the user through onboarding again just because their token expired.
            if (isFullLogout) {
                prefs.remove(ONBOARDING_COMPLETE)
                prefs.remove(PERMISSIONS_GRANTED)
            }
        }
    }

    // --- DEVICE ID (frontend identifier for device-binding workflows) ---
    fun getDeviceId(): Flow<String?> = context.dataStore.data.map { it[DEVICE_ID] }

    suspend fun getOrCreateDeviceId(): String {
        val existing = getDeviceId().first()
        if (!existing.isNullOrBlank()) return existing

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val generated = if (!androidId.isNullOrBlank()) {
            "android-$androidId"
        } else {
            "generated-${UUID.randomUUID()}"
        }

        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID] = generated
        }
        return generated
    }

    // --- REFRESH ACCESS TOKEN (called by AuthInterceptor) ---
    suspend fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val refreshService = com.yourname.womensafety.data.network.RetrofitClient.createRefreshService()
            val response = refreshService.refreshToken(RefreshRequest(refreshToken))
            val body = response.body()
            // body is ApiResponse<RefreshData> — extract the nested data object
            val refreshData = body?.data
            if (response.isSuccessful && refreshData != null) {
                context.dataStore.edit { prefs ->
                    prefs[ACCESS_TOKEN] = refreshData.accessToken
                    if (refreshData.refreshToken != null) prefs[REFRESH_TOKEN] = refreshData.refreshToken
                    if (refreshData.expiresIn != null)
                        prefs[TOKEN_EXPIRES_AT] = System.currentTimeMillis() + (refreshData.expiresIn * 1000L)
                }
                refreshData.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
