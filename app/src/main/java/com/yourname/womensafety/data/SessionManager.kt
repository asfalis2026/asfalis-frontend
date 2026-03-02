package com.yourname.womensafety.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global singleton that tracks whether the user's session has expired.
 *
 * Uses [StateFlow] (not SharedFlow) so the flag remains `true` even if the
 * user is on the SOS screen — once SOS is done and they navigate away,
 * AppNavGraph re-evaluates and shows the "Session Expired" dialog.
 *
 * Call [onSessionExpired] from the OkHttp interceptor when token refresh fails.
 * Call [clearExpiry] once the user has been redirected to login.
 */
object SessionManager {

    private val _sessionExpired = MutableStateFlow(false)

    /** Observe this in AppNavGraph. True means the session is no longer valid. */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    /** Thread-safe — may be called from OkHttp / background threads. */
    fun onSessionExpired() {
        _sessionExpired.value = true
    }

    /** Reset after the user has been sent to the login screen. */
    fun clearExpiry() {
        _sessionExpired.value = false
    }
}
