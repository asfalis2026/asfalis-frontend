# 🛡️ ASFALIS — Women Safety Android App

> A comprehensive personal safety Android application built with Jetpack Compose, designed to empower women with real-time SOS alerts, live location sharing, trusted contact management, and shake-triggered emergency dispatch.

---

## 📱 Screenshots

> _Add screenshots here_

---

## ✨ Features

| Feature | Description |
|---|---|
| 🆘 **SOS Alert** | One-tap emergency alert with a countdown cancel window. Dispatches SMS + FCM push to all trusted contacts with live location |
| 📳 **Shake to SOS** | Configurable shake sensitivity triggers SOS automatically when shaking the device |
| 🗺️ **Live Map** | Real-time GPS location display with optional live-sharing to trusted contacts via WebSocket (Socket.IO) |
| 👥 **Trusted Contacts** | Add, manage, and set primary emergency contacts. Primary contacts receive SOS alerts first |
| 📜 **SOS History** | Full log of all past SOS events with status, timestamp, and location |
| 👤 **Profile & Account** | View and update profile details, custom SOS message, and account settings |
| ⚙️ **Settings** | Control shake sensitivity, SOS message, and app preferences |
| 🔔 **FCM Push Notifications** | Receive SOS alerts from trusted contacts via Firebase Cloud Messaging |
| 🔑 **Phone OTP Auth** | Secure phone number registration and login with Twilio SMS OTP verification |
| 🔒 **JWT Session Management** | Silent token refresh with proactive expiry detection and session-expired dialog |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│           Jetpack Compose UI            │
│   Screens → ViewModels → Repositories  │
└────────────────┬────────────────────────┘
                 │
     ┌───────────┴───────────┐
     │                       │
┌────▼─────┐         ┌───────▼──────┐
│ Retrofit │         │  TokenManager │
│ (REST)   │         │  (DataStore)  │
└────┬─────┘         └───────────────┘
     │  HTTPS / WS
     ▼
┌──────────────────────────────┐
│     Flask Backend (Docker)   │
│  PostgreSQL · Twilio · FCM   │
└──────────────────────────────┘
```

- **UI Layer** — Jetpack Compose screens with `StateFlow`-driven state
- **ViewModel Layer** — Coroutine-based, lifecycle-aware, one ViewModel per screen
- **Repository Layer** — Single source of truth; wraps all API calls in `NetworkResult<T>`
- **Network Layer** — Retrofit + OkHttp with `AuthInterceptor` for proactive JWT refresh
- **Local Storage** — Jetpack DataStore for tokens; in-memory cache for profile

---

## 🛠️ Tech Stack

| Category | Library / Tool | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.09 |
| Navigation | Navigation Compose | 2.7.7 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| JSON | Gson | 2.11.0 |
| Local Storage | DataStore Preferences | 1.1.1 |
| Async | Kotlin Coroutines | 1.9.0 |
| WebSocket | Socket.IO Client | 2.1.1 |
| Push Notifications | Firebase Cloud Messaging | BOM 33.7.0 |
| Maps | Maps Compose + Play Services | 6.2.1 / 19.0.0 |
| Location | Play Services Location | 21.3.0 |
| Min SDK | Android 8.0 (Oreo) | API 26 |
| Target SDK | Android 16 | API 36 |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11+
- A running instance of the [Asfalis Backend](https://github.com/asfalis2026/asfalis-backend) (Docker or deployed)
- `google-services.json` placed in `app/`

### 1. Clone the repository

```bash
git clone https://github.com/asfalis2026/asfalis-frontend.git
cd asfalis-frontend
```

### 2. Configure the backend URL

Open [app/src/main/java/com/yourname/womensafety/data/network/ApiConstants.kt](app/src/main/java/com/yourname/womensafety/data/network/ApiConstants.kt) and set `BASE_URL`:

```kotlin
object ApiConstants {
    const val BASE_URL_LOCAL_EMULATOR = "http://10.0.2.2:5000/api/"      // Android Emulator
    const val BASE_URL_LOCAL_DEVICE   = "http://192.168.x.x:5000/api/"   // Physical device (same WiFi)
    const val BASE_URL_PRODUCTION     = "https://asfalis-backend.onrender.com/api/"

    const val BASE_URL = BASE_URL_PRODUCTION   // ← change this
}
```

### 3. Network Security (HTTP for local dev)

If using a local HTTP backend on a physical device, ensure your LAN IP is listed in [`app/src/main/res/xml/network_security_config.xml`](app/src/main/res/xml/network_security_config.xml):

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">192.168.x.x</domain>
</domain-config>
```

### 4. Build & Run

```bash
./gradlew assembleDebug
```

Or press **Run ▶** in Android Studio.

---

## 📁 Project Structure

```
app/src/main/java/com/yourname/womensafety/
├── AsfalisApplication.kt          # App entry point, initialises ServiceLocator
├── AsfalisFirebaseService.kt      # FCM message handling & notifications
├── MainActivity.kt
├── data/
│   ├── AppServiceLocator.kt       # Manual DI — creates all singletons
│   ├── SessionManager.kt          # Global session-expired signal
│   ├── local/
│   │   └── TokenManager.kt        # JWT storage & silent refresh via DataStore
│   ├── network/
│   │   ├── ApiConstants.kt        # Base URLs & WebSocket URLs
│   │   ├── AuthInterceptor.kt     # Attaches Bearer token; handles 401 refresh
│   │   ├── RetrofitClient.kt      # Retrofit singleton factory
│   │   ├── LocationSocketManager.kt # Socket.IO live location
│   │   ├── api/                   # Retrofit service interfaces
│   │   └── dto/                   # Request / response data classes
│   └── repository/
│       ├── BaseRepository.kt      # safeApiCall() wrapper → NetworkResult<T>
│       ├── AuthRepository.kt
│       ├── UserRepository.kt
│       ├── ContactsRepository.kt
│       ├── SosRepository.kt
│       ├── LocationRepository.kt
│       └── SettingsRepository.kt
└── ui/
    ├── navigation/
    │   └── AppNavGraph.kt         # NavHost + bottom navigation bar
    ├── screens/                   # One file per screen
    │   ├── LoginScreen.kt
    │   ├── RegisterScreen.kt
    │   ├── VerifyOTPScreen.kt
    │   ├── DashboardScreen.kt
    │   ├── SOSAlertScreen.kt
    │   ├── SOSHistoryScreen.kt
    │   ├── LiveMapScreen.kt
    │   ├── TrustedContacts.kt
    │   ├── ProfileScreen.kt
    │   └── SettingsScreen.kt
    ├── viewmodels/                # One ViewModel per screen
    └── theme/
```

---

## 🔐 Authentication Flow

```
Register (phone + password)
    └── POST /api/auth/register/phone
            └── Twilio sends OTP SMS
                    └── Enter OTP → POST /api/auth/verify-phone-otp
                                        └── JWT tokens stored in DataStore
                                                └── Navigate to Dashboard ✅

Login (phone + password)
    └── POST /api/auth/login/phone
            ├── Success → JWT tokens → Dashboard ✅
            └── PHONE_NOT_VERIFIED → Resend OTP → VerifyOTP screen
```

Token refresh is handled **silently** by `AuthInterceptor` — proactive refresh 60s before expiry, with a `Mutex` to prevent parallel refresh races.

---

## 🆘 SOS Flow

```
Trigger (manual tap or shake)
    └── POST /api/sos/trigger  (uses long-lived sos_token)
            └── 10-second countdown (cancellable)
                    └── POST /api/sos/send-now
                                └── Backend dispatches SMS + FCM to all trusted contacts
                                        └── Contacts receive push notification with live location
```

---

## 🌐 Backend

The Asfalis backend is a Flask + PostgreSQL application containerised with Docker.

| | |
|---|---|
| **Repo** | [asfalis2026/asfalis-backend](https://github.com/asfalis2026/asfalis-backend) |
| **Production** | `https://asfalis-backend.onrender.com` |
| **Local** | `docker compose up` → `http://0.0.0.0:5000` |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">
  Built with ❤️ for women's safety
</div>
