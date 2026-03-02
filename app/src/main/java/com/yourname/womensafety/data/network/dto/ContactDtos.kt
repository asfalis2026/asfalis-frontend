package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class WhatsAppJoinInfo(
    @SerializedName("twilio_number") val twilioNumber: String,
    @SerializedName("sandbox_code") val sandboxCode: String,
    @SerializedName("whatsapp_link") val whatsappLink: String
)

data class TrustedContact(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("relationship") val relationship: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("whatsapp_join_info") val whatsappJoinInfo: WhatsAppJoinInfo? = null,
    @SerializedName("invite_message") val inviteMessage: String? = null
)

/** Body for POST /api/contacts — no email field. */
data class AddContactRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("relationship") val relationship: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

data class UpdateContactRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("relationship") val relationship: String? = null
)
