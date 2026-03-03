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
    @SerializedName("email") val email: String? = null,
    @SerializedName("relationship") val relationship: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("verified_at") val verifiedAt: String? = null,
    @SerializedName("whatsapp_join_info") val whatsappJoinInfo: WhatsAppJoinInfo? = null,
    @SerializedName("invite_message") val inviteMessage: String? = null
)

/** Body for POST /api/contacts */
data class AddContactRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("relationship") val relationship: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

data class UpdateContactRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("relationship") val relationship: String? = null
)

/** Response from POST /api/contacts (Step 1: Send OTP) */
data class AddContactResponse(
    @SerializedName("contact_id") val contactId: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("otp_sent") val otpSent: Boolean,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int
)

/** Request for POST /api/contacts/verify-otp (Step 2: Verify OTP) */
data class VerifyContactOtpRequest(
    @SerializedName("contact_id") val contactId: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

/** Request for POST /api/contacts/resend-otp */
data class ResendContactOtpRequest(
    @SerializedName("contact_id") val contactId: String
)

/** Response for resend OTP */
data class ResendContactOtpResponse(
    @SerializedName("contact_id") val contactId: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int
)
