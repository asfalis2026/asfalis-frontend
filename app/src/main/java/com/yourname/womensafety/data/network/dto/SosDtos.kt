package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class SosTriggerRequest(
    @SerializedName("trigger_type") val triggerType: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float? = null
)

data class SosSendNowRequest(
    @SerializedName("alert_id") val alertId: String
)

data class SosCancelRequest(
    @SerializedName("alert_id") val alertId: String
)

data class SosSafeRequest(
    @SerializedName("alert_id") val alertId: String
)

data class SosAlertData(
    @SerializedName("alert_id") val alertId: String,
    @SerializedName("status") val status: String,
    @SerializedName("countdown_seconds") val countdownSeconds: Int? = null,
    @SerializedName("contacts_to_notify") val contactsToNotify: Int? = null
)

data class SosHistoryItem(
    @SerializedName("alert_id") val alertId: String,
    @SerializedName("trigger_type") val triggerType: String,
    @SerializedName("address") val address: String?,
    @SerializedName("status") val status: String,
    @SerializedName("triggered_at") val triggeredAt: String,
    @SerializedName("resolved_at") val resolvedAt: String?
)
