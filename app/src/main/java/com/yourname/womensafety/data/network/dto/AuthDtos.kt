package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

// ─── Requests ───────────────────────────────────────────────────────────────

/** POST /auth/register/phone */
data class PhoneRegisterRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("password") val password: String,
    @SerializedName("country") val country: String
)

/** POST /auth/verify-phone-otp */
data class VerifyPhoneOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String
)

/** POST /auth/login/phone */
data class PhoneLoginRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_imei") val deviceImei: String? = null,
    @SerializedName("confirm_handover") val confirmHandover: Boolean = false
)

data class HandsetChangeStatusRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("device_imei") val deviceImei: String
)

/** POST /auth/resend-otp  (phone_number key) */
data class ResendOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

/** POST /auth/forgot-password */
data class ForgotPasswordRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String
)

// ─── Responses ──────────────────────────────────────────────────────────────

/**
 * Returned by POST /auth/register/phone (step 1 — no tokens yet).
 * Twilio sends the OTP SMS directly to the user's phone.
 */
data class PhoneRegisterData(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("expires_in") val expiresIn: Int
)

/** Returned by POST /auth/resend-otp — Twilio re-sends the SMS; no code returned. */
data class ResendOtpData(
    @SerializedName("expires_in") val expiresIn: Int
)

/** Returned by POST /auth/forgot-password — Twilio sends reset OTP; no code returned. */
data class ForgotPasswordData(
    @SerializedName("expires_in") val expiresIn: Int? = null
)

/** POST /auth/reset-password — set new password after verifying the Twilio OTP. */
data class ResetPasswordRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("new_password") val newPassword: String
)

data class AuthData(
    @SerializedName("user_id") val userId: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("is_new_user") val isNewUser: Boolean = false,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("sos_token") val sosToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null
)

data class RefreshData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null
)

data class LogoutRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class ValidateData(
    @SerializedName("user_id") val userId: String,
    @SerializedName("is_valid") val isValid: Boolean
)

data class HandsetChangeStatusData(
    @SerializedName("has_pending_request") val hasPendingRequest: Boolean = false,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("eligible_at") val eligibleAt: String? = null,
    @SerializedName("remaining_seconds") val remainingSeconds: Int? = null,
    @SerializedName("confirm_handover_required") val confirmHandoverRequired: Boolean = false
)
