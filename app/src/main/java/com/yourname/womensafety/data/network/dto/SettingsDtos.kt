package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class UserSettings(
    @SerializedName("emergency_number") val emergencyNumber: String,
    @SerializedName("sos_message") val sosMessage: String,
    @SerializedName("shake_sensitivity") val shakeSensitivity: String,
    @SerializedName("battery_optimization") val batteryOptimization: Boolean,
    @SerializedName("haptic_feedback") val hapticFeedback: Boolean,
    @SerializedName("auto_sos_enabled") val autoSosEnabled: Boolean = false
)

data class UpdateSettingsRequest(
    @SerializedName("emergency_number") val emergencyNumber: String? = null,
    @SerializedName("sos_message") val sosMessage: String? = null,
    @SerializedName("shake_sensitivity") val shakeSensitivity: String? = null,
    @SerializedName("battery_optimization") val batteryOptimization: Boolean? = null,
    @SerializedName("haptic_feedback") val hapticFeedback: Boolean? = null,
    @SerializedName("auto_sos_enabled") val autoSosEnabled: Boolean? = null
)
