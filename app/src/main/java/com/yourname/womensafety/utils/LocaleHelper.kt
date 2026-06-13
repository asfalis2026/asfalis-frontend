package com.yourname.womensafety.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocaleHelper {
    /**
     * Changes the application's locale at runtime.
     * Uses the official AndroidX API which persists the preference automatically.
     *
     * @param languageCode Frontend ISO code: "en", "hi", or "bn"
     */
    fun setLocale(context: Context, languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        LocaleManager.setLanguage(languageCode)
    }

    /**
     * Helper to map language display names to ISO codes.
     * Used by LanguageSelectionScreen.
     */
    fun getCodeFromLanguage(language: String): String {
        return when (language.lowercase()) {
            "bengali" -> "bn"
            "hindi"   -> "hi"
            "english" -> "en"
            else      -> "en"
        }
    }

    /**
     * Maps a frontend locale code to the backend API language code.
     * Frontend: "en" | "hi" | "bn"  →  Backend: "en" | "hin" | "ben"
     */
    fun toBackendCode(frontendCode: String): String = when (frontendCode) {
        "hi" -> "hin"
        "bn" -> "ben"
        else -> "en"
    }

    /**
     * Maps a backend API language code to the frontend locale code.
     * Backend: "en" | "hin" | "ben"  →  Frontend: "en" | "hi" | "bn"
     */
    fun fromBackendCode(backendCode: String): String = when (backendCode) {
        "hin" -> "hi"
        "ben" -> "bn"
        else  -> "en"
    }
}

object LocaleManager {
    /**
     * Initialize from AppCompatDelegate — reads the locale that was previously
     * persisted by setApplicationLocales(). Falls back to "en" if none was saved.
     *
     * This is the fix for the "language reverts on restart" bug.
     * The old code used Locale.getDefault().language which returns the SYSTEM
     * locale, not the app-specific persisted locale.
     */
    private val _currentLanguage = MutableStateFlow(
        AppCompatDelegate.getApplicationLocales()
            .get(0)
            ?.toLanguageTag()
            ?.split("-")
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "en"
    )
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }
}
