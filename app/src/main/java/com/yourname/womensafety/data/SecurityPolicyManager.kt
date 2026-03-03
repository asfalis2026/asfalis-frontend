package com.yourname.womensafety.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SecurityPolicyState(
    val screenshotProtectionEnabled: Boolean = true,
    val protectedScreens: Set<String> = setOf("trusted_contacts", "sos_history")
)

object SecurityPolicyManager {
    private val _state = MutableStateFlow(SecurityPolicyState())
    val state: StateFlow<SecurityPolicyState> = _state

    fun update(enabled: Boolean, protectedScreens: List<String>) {
        _state.value = SecurityPolicyState(
            screenshotProtectionEnabled = enabled,
            protectedScreens = protectedScreens.map { it.lowercase() }.toSet()
        )
    }

    fun isScreenProtected(screenKey: String): Boolean {
        val state = _state.value
        return state.screenshotProtectionEnabled && screenKey.lowercase() in state.protectedScreens
    }
}
