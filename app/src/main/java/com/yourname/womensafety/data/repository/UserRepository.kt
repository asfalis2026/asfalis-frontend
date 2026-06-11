package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.network.api.UserApiService
import com.yourname.womensafety.data.network.dto.*

class UserRepository(
    private val userApi: UserApiService,
    private val appCache: AppCache
) : BaseRepository() {

    /** In-memory profile cache for the current session (avoids re-parsing JSON). */
    private var cachedProfile: UserProfile? = null

    suspend fun getProfile(forceRefresh: Boolean = false): NetworkResult<UserProfile> {
        // 1. Return in-memory cache if available and not forced
        if (!forceRefresh && cachedProfile != null) {
            return NetworkResult.Success(cachedProfile!!)
        }
        
        // 2. Return disk cache for instant UI on fresh launch (if not forced)
        if (!forceRefresh) {
            val diskCacheStr = appCache.getUserProfile()
            if (diskCacheStr != null) {
                try {
                    val profile = com.google.gson.Gson().fromJson(diskCacheStr, UserProfile::class.java)
                    cachedProfile = profile
                    return NetworkResult.Success(profile)
                } catch (e: Exception) {
                    // Ignore parse error, fetch from network
                }
            }
        }

        // 3. Fetch from network
        val result = safeApiCall { userApi.getProfile() }
        if (result is NetworkResult.Success) {
            cachedProfile = result.data
            // Persist full profile to AppCache
            appCache.saveUserProfile(com.google.gson.Gson().toJson(result.data))
        }
        return result
    }

    /** Returns the cached first name from AppCache (persisted across app restarts). */
    suspend fun getCachedUserName(): String? = appCache.getUserName()

    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfile> {
        val result = safeApiCall { userApi.updateProfile(request) }
        if (result is NetworkResult.Success) {
            return getProfile(forceRefresh = true)
        }
        @Suppress("UNCHECKED_CAST")
        return result as NetworkResult<UserProfile>
    }

    suspend fun updateSosMessage(sosMessage: String): NetworkResult<SosMessageData> {
        return safeApiCall { userApi.updateSosMessage(UpdateSosMessageRequest(sosMessage)) }
    }

    suspend fun updateFcmToken(fcmToken: String): NetworkResult<Unit> {
        return safeApiCall { userApi.updateFcmToken(FcmTokenRequest(fcmToken)) }
    }

    suspend fun getSecurityPolicy(): NetworkResult<SecurityPolicyResponse> {
        return safeApiCall { userApi.getSecurityPolicy() }
    }

    suspend fun deleteAccount(): NetworkResult<Unit> {
        return safeApiCall { userApi.deleteAccount() }
    }

    fun clearCache() {
        cachedProfile = null
    }
}
