package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class LocationUpdateRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float? = null,
    @SerializedName("is_sharing") val isSharing: Boolean = false
)

data class LocationData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String?,
    @SerializedName("accuracy") val accuracy: String?,
    @SerializedName("is_sharing") val isSharing: Boolean,
    @SerializedName("recorded_at") val recordedAt: String?
)

data class SharingData(
    @SerializedName("sharing_session_id") val sharingSessionId: String,
    @SerializedName("shared_with") val sharedWith: List<SharedContact>,
    @SerializedName("tracking_url") val trackingUrl: String?
)

data class SharedContact(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)
