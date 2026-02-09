package com.yourname.womensafety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.yourname.womensafety.ui.navigation.AppNavGraph
import com.yourname.womensafety.ui.theme.WomenSafetyAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Optimized enableEdgeToEdge:
        // We force the status bar and navigation bar to be transparent with dark icons
        // This ensures your custom gradients (Black/Red) flow perfectly into the system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        // 2. Ensures the window draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            WomenSafetyAppTheme {
                // The AppNavGraph handles all screen transitions (Splash -> Login -> Dashboard)
                AppNavGraph()
            }
        }
    }
}