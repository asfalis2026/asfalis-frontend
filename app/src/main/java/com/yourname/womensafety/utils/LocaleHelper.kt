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
     * This uses the official AndroidX API, which works back to API 14 and
     * automatically handles recreation and saving the preference in Android 13+.
     *
     * @param languageCode ISO 639-1 language code (e.g. "en", "hi", "bn")
     */
    fun setLocale(context: Context, languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        LocaleManager.setLanguage(languageCode)
    }

    /**
     * Helper to map language display names to ISO codes.
     */
    fun getCodeFromLanguage(language: String): String {
        return when (language.lowercase()) {
            "bengali" -> "bn"
            "hindi" -> "hi"
            "english" -> "en"
            else -> "en"
        }
    }
}

object LocaleManager {
    private val _currentLanguage = MutableStateFlow(java.util.Locale.getDefault().language)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }
}
