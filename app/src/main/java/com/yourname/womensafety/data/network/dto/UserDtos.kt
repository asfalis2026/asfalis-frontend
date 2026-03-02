package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("user_id") val userId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("sos_message") val sosMessage: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("emergency_contact") val emergencyContact: String?,
    @SerializedName("trusted_contacts") val trustedContacts: List<TrustedContact> = emptyList(),
    @SerializedName("trusted_contacts_count") val trustedContactsCount: Int = 0,
    @SerializedName("member_since") val memberSince: String,
    @SerializedName("is_protection_active") val isProtectionActive: Boolean,
    @SerializedName("auth_provider") val authProvider: String
)

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("sos_message") val sosMessage: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)

data class UpdateSosMessageRequest(
    @SerializedName("sos_message") val sosMessage: String
)

data class SosMessageData(
    @SerializedName("sos_message") val sosMessage: String
)

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)
