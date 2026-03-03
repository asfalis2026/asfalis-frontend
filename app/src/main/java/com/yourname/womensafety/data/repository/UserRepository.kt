package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.UserApiService
import com.yourname.womensafety.data.network.dto.*

class UserRepository(
    private val userApi: UserApiService
) : BaseRepository() {

    private var cachedProfile: UserProfile? = null

    suspend fun getProfile(forceRefresh: Boolean = false): NetworkResult<UserProfile> {
        if (!forceRefresh && cachedProfile != null) {
            return NetworkResult.Success(cachedProfile!!)
        }
        val result = safeApiCall { userApi.getProfile() }
        if (result is NetworkResult.Success) cachedProfile = result.data
        return result
    }

    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfile> {
        val result = safeApiCall { userApi.updateProfile(request) }
        if (result is NetworkResult.Success) {
            // Backend returns no data on update — force a fresh profile fetch
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

    fun clearCache() { cachedProfile = null }
}
