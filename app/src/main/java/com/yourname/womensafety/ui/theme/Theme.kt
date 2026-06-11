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

// ── Dark colour scheme — matches the image palette precisely ──────────────────
private val SafetyDarkColorScheme = darkColorScheme(
    primary          = RedPrimary,
    onPrimary        = Color.White,
    primaryContainer = RedPrimary.copy(alpha = 0.15f),

    secondary        = AmberWarning,
    onSecondary      = Color.White,

    tertiary         = BlueInfo,
    onTertiary       = Color.White,

    background       = BgDark,
    onBackground     = Color.White,

    surface          = CardBg,
    onSurface        = Color.White,
    surfaceVariant   = CardBgAlt,
    onSurfaceVariant = TextGray,

    error            = RedPrimary,
    onError          = Color.White,

    outline          = Color.White.copy(alpha = 0.08f),
    outlineVariant   = Color.White.copy(alpha = 0.05f)
)

// ── App theme composable ──────────────────────────────────────────────────────
@Composable
fun WomenSafetyAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SafetyDarkColorScheme
    val view = LocalView.current

    // Transparent status bar so the gradient bleeds through perfectly
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = Color.Transparent.toArgb()
            window.navigationBarColor = Color(0xFF090000).toArgb()

            // Keep status-bar icons white on the dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AsfalisTypography,
        content     = content
    )
}
