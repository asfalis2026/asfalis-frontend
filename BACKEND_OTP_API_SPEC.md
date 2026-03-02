# 📋 Backend OTP API Specification — Asfalis Phone Auth

> **Architecture note:**  
> OTP SMS delivery is handled entirely by **Twilio Verify** on the backend.  
> The Android frontend **does not** send SMS — it only calls the API and lets the user enter the code they received on their phone.  
> The `otp_code` is **never** returned to the frontend.

---

## How the OTP flow works

```
User clicks "Get Verification Code"
    │
    ▼
Frontend → POST /api/auth/register/phone
    │
    ◄── Backend calls Twilio Verify → Twilio sends SMS to user's phone
    │
Backend responds with { phone_number, expires_in }   ← NO otp_code
    │
User receives SMS from Twilio on their real phone
    │
User types the 6-digit code into the app
    │
Frontend → POST /api/auth/verify-phone-otp  { phone_number, otp_code }
    │
Backend verifies OTP via Twilio → returns JWT tokens ✅
```

---

## Auth Endpoints

---

### 1. `POST /api/auth/register/phone` — Phone Registration (Step 1)

Registers the user and triggers a Twilio Verify SMS. No tokens returned yet.

#### Request Body
```json
{
  "full_name": "Jane Doe",
  "phone_number": "+919876543210",
  "password": "SecurePass123",
  "country": "India"
}
```

#### ✅ Success Response — `201 Created`
```json
{
  "status": "success",
  "message": "OTP sent to your phone via SMS",
  "data": {
    "phone_number": "+919876543210",
    "expires_in": 300
  }
}
```

> ⚠️ Do **not** include `otp_code` in the response. Twilio delivers it directly.

#### Error Responses
| HTTP | `error_code` | Meaning |
|---|---|---|
| `409` | `CONFLICT` | Phone already registered |
| `400` | `VALIDATION_ERROR` | Missing/invalid fields |
| `429` | `RATE_LIMITED` | Too many registration attempts |

---

### 2. `POST /api/auth/verify-phone-otp` — OTP Verification (Step 2)

User submits the code received via Twilio SMS. Returns JWT tokens.

#### Request Body
```json
{
  "phone_number": "+919876543210",
  "otp_code": "839201"
}
```

#### ✅ Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Phone verified successfully",
  "data": {
    "user_id": "uuid",
    "full_name": "Jane Doe",
    "phone_number": "+919876543210",
    "is_new_user": true,
    "access_token": "<JWT>",
    "refresh_token": "<JWT>",
    "sos_token": "<token>",
    "expires_in": 900
  }
}
```

#### Error Responses
| HTTP | `error_code` | Meaning |
|---|---|---|
| `422` | `OTP_INVALID` | Wrong or expired OTP |
| `404` | `NOT_FOUND` | Phone not registered |

---

### 3. `POST /api/auth/login/phone` — Login

#### Request Body
```json
{
  "phone_number": "+919876543210",
  "password": "SecurePass123"
}
```

#### ✅ Success Response — `200 OK`
Same `data` shape as Verify OTP response above.

#### Error Responses
| HTTP | `error_code` | Meaning |
|---|---|---|
| `401` | `UNAUTHORIZED` | Wrong password |
| `403` | `PHONE_NOT_VERIFIED` | Account exists but OTP not yet verified — frontend redirects user to OTP screen |
| `429` | `RATE_LIMITED` | Too many attempts |

> ⚠️ The `error_code` value `"PHONE_NOT_VERIFIED"` must be exact — the frontend matches it by string equality to trigger navigation.

---

### 4. `POST /api/auth/resend-otp` — Resend OTP

Triggers Twilio Verify to resend the SMS. Rate-limited 3×/15 min.

#### Request Body
```json
{ "phone_number": "+919876543210" }
```

#### ✅ Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "OTP resent via SMS",
  "data": { "expires_in": 300 }
}
```

> ⚠️ Do **not** include `otp_code`. Twilio re-sends directly.

#### Error Responses
| HTTP | `error_code` | Meaning |
|---|---|---|
| `400` | `ALREADY_VERIFIED` | Phone already verified |
| `429` | `RATE_LIMITED` | Resend limit reached |

---

### 5. `POST /api/auth/forgot-password` — Forgot Password

Triggers Twilio Verify to send a reset OTP.

#### Request Body
```json
{ "phone_number": "+919876543210" }
```

#### ✅ Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Password reset OTP sent via SMS",
  "data": { "expires_in": 300 }
}
```

> Always return `200` even if the phone is not registered (don't reveal registration status).

---

### 6. `POST /api/auth/reset-password` — Reset Password

Verify the Twilio OTP and set a new password. **No auth token required** (public endpoint).

#### Request Body
```json
{
  "phone_number": "+919876543210",
  "otp_code": "204891",
  "new_password": "NewSecurePass123!"
}
```

#### ✅ Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Password reset successfully."
}
```

#### Error Responses
| HTTP | `error_code` | Meaning |
|---|---|---|
| `422` | `OTP_INVALID` | Wrong or expired OTP |
| `404` | `NOT_FOUND` | Phone not registered |
| `400` | `VALIDATION_ERROR` | Password too weak |

---

## Response Envelope (ALL endpoints)

```json
{
  "status": "success" | "error",
  "message": "Human readable string",
  "error_code": "MACHINE_CODE",   ← top-level, present only on errors
  "data": { ... }                  ← present only on success
}
```

> The Android `BaseRepository.safeApiCall()` reads `body.status == "success"` and `body.errorCode` (mapped from `error_code`) to populate `NetworkResult.Success` / `NetworkResult.Error`.

---

## Quick Checklist for Backend

- [ ] `POST /api/auth/register/phone` — **no** `otp_code` in response; Twilio sends SMS
- [ ] `POST /api/auth/resend-otp` — **no** `otp_code` in response; Twilio re-sends
- [ ] `POST /api/auth/forgot-password` — **no** `otp_code` in response; Twilio sends
- [ ] `POST /api/auth/reset-password` — public (no JWT required)
- [ ] `POST /api/auth/login/phone` error body uses `"error_code": "PHONE_NOT_VERIFIED"` (exact string)
- [ ] All error responses include `"error_code"` as a **top-level** JSON field
- [ ] OTP verification is handled entirely by Twilio Verify — no manual OTP storage needed


> **Why this doc exists:**  
> The Android frontend is responsible for delivering OTP codes to the user via SMS (using Android's `SmsManager`).  
> For this to work, **every endpoint that generates an OTP must return the raw OTP code in its JSON response**.  
> If the `otp_code` field is missing or `null`, the frontend cannot send the SMS and the user is stuck.

---

## How the OTP flow works

```
User clicks "Get Verification Code"
    │
    ▼
Frontend → POST /api/auth/register/phone
    │
    ◄── Backend responds with { otp_code: "839201", expires_in: 300 }
    │
Frontend reads otp_code from response
    │
Frontend calls SmsManager.sendTextMessage(userPhone, "Your OTP is: 839201 ...")
    │
    ▼
User receives SMS on their real phone ✅
```

The backend **must NOT** send the SMS itself. The frontend handles SMS delivery.

---

## Endpoints that must return `otp_code`

---

### 1. `POST /api/auth/register/phone` — Phone Registration (Step 1)

Registers a new user account. Does **not** return tokens yet.  
Returns the OTP that the app will SMS to the user.

#### Request Body
```json
{
  "full_name": "Jane Doe",
  "phone_number": "+919876543210",
  "password": "SecurePass123",
  "country": "India"
}
```

#### ✅ Required Success Response — `201 Created`
```json
{
  "status": "success",
  "message": "OTP sent to phone",
  "data": {
    "phone_number": "+919876543210",
    "otp_code": "839201",
    "expires_in": 300
  }
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `phone_number` | `string` | ✅ | Echo back the number that was registered |
| `otp_code` | `string` | ✅ **MUST be present** | 6-digit numeric OTP. Frontend uses this to send SMS |
| `expires_in` | `int` | ✅ | Seconds until OTP expires (e.g. `300` = 5 minutes) |

#### Error Responses
| HTTP Code | `error_code` field | Meaning |
|---|---|---|
| `409` | `CONFLICT` | Phone number already registered |
| `400` | `VALIDATION_ERROR` | Missing/invalid fields |
| `429` | `RATE_LIMITED` | Too many registration attempts |

```json
{
  "status": "error",
  "error_code": "CONFLICT",
  "message": "This phone number is already registered."
}
```

---

### 2. `POST /api/auth/verify-phone-otp` — OTP Verification (Step 2)

User submits the 6-digit code they received via SMS. Returns JWT tokens.

#### Request Body
```json
{
  "phone_number": "+919876543210",
  "otp_code": "839201"
}
```

#### ✅ Required Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Phone verified successfully",
  "data": {
    "user_id": "usr_abc123",
    "full_name": "Jane Doe",
    "phone_number": "+919876543210",
    "is_new_user": true,
    "access_token": "<JWT>",
    "refresh_token": "<JWT>",
    "sos_token": "<token>",
    "expires_in": 3600
  }
}
```

#### Error Responses
| HTTP Code | `error_code` field | Meaning |
|---|---|---|
| `422` | `OTP_INVALID` | Wrong or expired OTP |
| `400` | `ALREADY_VERIFIED` | Phone was already verified |

---

### 3. `POST /api/auth/login/phone` — Login

#### Request Body
```json
{
  "phone_number": "+919876543210",
  "password": "SecurePass123"
}
```

#### ✅ Required Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "user_id": "usr_abc123",
    "full_name": "Jane Doe",
    "phone_number": "+919876543210",
    "is_new_user": false,
    "access_token": "<JWT>",
    "refresh_token": "<JWT>",
    "sos_token": "<token>",
    "expires_in": 3600
  }
}
```

#### Error Responses
| HTTP Code | `error_code` field | Meaning |
|---|---|---|
| `401` | `UNAUTHORIZED` | Wrong password |
| `403` | `PHONE_NOT_VERIFIED` | Account exists but OTP was never verified — frontend will redirect user to the OTP screen |
| `429` | `RATE_LIMITED` | Too many login attempts |

> ⚠️ **Critical:** The `PHONE_NOT_VERIFIED` error code is what the frontend uses to send the user back to the OTP verification screen. The `error_code` field in the JSON body **must** be exactly `"PHONE_NOT_VERIFIED"` — not any other string.

---

### 4. `POST /api/auth/resend-otp` — Resend OTP

User requests a new OTP (e.g. the first one expired). Frontend will SMS the new code.

#### Request Body
```json
{
  "phone_number": "+919876543210"
}
```

#### ✅ Required Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "OTP resent",
  "data": {
    "otp_code": "512047",
    "expires_in": 300
  }
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `otp_code` | `string` | ✅ **MUST be present** | New 6-digit OTP. Frontend sends this via SMS |
| `expires_in` | `int` | ✅ | Seconds until new OTP expires |

#### Error Responses
| HTTP Code | `error_code` field | Meaning |
|---|---|---|
| `409` | `ALREADY_VERIFIED` | Phone is already verified — no OTP needed |
| `429` | `RATE_LIMITED` | Resend limit hit (suggest: max 3 times per 15 minutes) |

---

### 5. `POST /api/auth/forgot-password` — Forgot Password

User forgot password; a reset OTP is sent. Frontend will SMS it.

#### Request Body
```json
{
  "phone_number": "+919876543210"
}
```

#### ✅ Required Success Response — `200 OK`
```json
{
  "status": "success",
  "message": "Password reset OTP sent",
  "data": {
    "otp_code": "204891",
    "expires_in": 300
  }
}
```

> If the phone number is not found, return a `200 OK` with `otp_code: null` (so the app doesn't reveal whether the number exists):
> ```json
> { "status": "success", "message": "If this number exists, an OTP was sent.", "data": { "otp_code": null } }
> ```

---

## Wrapper Response Format (ALL endpoints)

Every response must be wrapped in this envelope:

```json
{
  "status": "success" | "error",
  "message": "Human readable string",
  "error_code": "MACHINE_READABLE_CODE",   ← present only on errors
  "data": { ... }                           ← present only on success
}
```

> The Android `BaseRepository.safeApiCall()` reads `error_code` from the JSON body and maps it to the `NetworkResult.Error(code, message)` sealed class. If `error_code` is missing on an error response, the frontend falls through to a generic error message.

---

## OTP Requirements

| Requirement | Value |
|---|---|
| Length | 6 digits |
| Format | Numeric string (e.g. `"839201"`, not `839201`) |
| Expiry | 300 seconds (5 minutes) recommended |
| Storage | Hash + salt before storing in DB |
| Invalidation | OTP must be deleted/invalidated after one successful use |
| Rate limit | Max 3 resend requests per phone per 15 minutes |

---

## Quick Checklist for Backend

- [ ] `POST /api/auth/register/phone` → `data.otp_code` is **always a non-null string** on `200`
- [ ] `POST /api/auth/resend-otp` → `data.otp_code` is **always a non-null string** on `200`
- [ ] `POST /api/auth/forgot-password` → `data.otp_code` is present (may be `null` if phone not found)
- [ ] `POST /api/auth/login/phone` error body has `"error_code": "PHONE_NOT_VERIFIED"` (exact string) when account is unverified
- [ ] All error responses include `"error_code"` as a top-level field in the JSON body
- [ ] OTP is **never sent via Twilio/SMS from the backend** — only the raw code is returned to the frontend
- [ ] OTP is a 6-digit numeric string
- [ ] OTP expires after 5 minutes
- [ ] OTP is single-use (invalidated after successful verification)
