package com.yourname.womensafety.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// We define ONLY a dark scheme to match your screenshots 100%
private val SafetyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC60000),    // RedPrimary
    background = Color.Black,
    surface = Color(0xFF1A1111),    // CardBg
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WomenSafetyAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SafetyDarkColorScheme
    val view = LocalView.current

    // This block makes the Status Bar (time/battery) transparent so your gradient works
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Black.toArgb()

            // This ensures icons (clock/wifi) stay white on the dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Ensure you have a Typography.kt file, or remove this line if it errors
        content = content
    )
}