package com.yourname.womensafety.ui.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // --- SYSTEM BACK FIX ---
    // This prevents the "Black Screen". Instead of navigating back to the Splash,
    // it tells the Android Activity to finish/close.
    BackHandler {
        (context as? Activity)?.finish()
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF1A0000),
            Color(0xFF330000)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // --- TOP BAR / EXIT BUTTON ---
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // Also close the app here if they click the back arrow on login
                (context as? Activity)?.finish()
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Exit App",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- LOGO SECTION ---
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFE10600), Color(0xFF8B0000))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                "Welcome to Raksha",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your safety companion",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(50.dp))

            // --- AUTHENTICATION OPTIONS ---
            LoginOptionRow(
                icon = Icons.Default.Email,
                label = "Continue with Email",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("sign_in_email")
                }
            )

            Spacer(Modifier.height(16.dp))

            LoginOptionRow(
                icon = Icons.Default.Phone,
                label = "Continue with Phone",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("sign_in_phone")
                }
            )

            Spacer(Modifier.height(16.dp))

            // Google Login Placeholder
            LoginOptionRow(
                icon = Icons.Default.Email,
                label = "Continue with Google",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Update SharedPrefs so Splash knows we are logged in
                    val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("is_logged_in", true).apply()

                    // Navigate to dashboard and clear the login screen from history
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )

            Spacer(Modifier.height(40.dp))

            // --- FOOTER ---
            Row {
                Text("By continuing, you agree to our ", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Privacy Policy",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("privacy_policy")
                    }
                )
            }
        }
    }
}

@Composable
fun LoginOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE10600).copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(20.dp))
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}