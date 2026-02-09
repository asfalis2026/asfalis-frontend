package com.yourname.womensafety.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun PermissionsScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // --- FIXED NAVIGATION LOGIC ---
    fun navigateNext() {
        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)

        // 1. Mark onboarding as complete so splash screen doesn't show it again
        sharedPref.edit().putBoolean("onboarding_complete", true).apply()

        // 2. Check if user is already logged in
        val isUserLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (isUserLoggedIn) {
            navController.navigate("dashboard") {
                // Clear the entire onboarding/permission stack
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                // Clear the onboarding/permission stack
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    // --- SYSTEM PERMISSION LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        navigateNext()
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE10600).copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = Color(0xFFE10600),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Safety Permissions",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "To keep you safe, we need access to a few vital features of your phone.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(48.dp))

        PermissionItem(
            icon = Icons.Outlined.LocationOn,
            title = "Location Access",
            desc = "To share your live coordinates with emergency contacts."
        )
        PermissionItem(
            icon = Icons.Outlined.Message,
            title = "SMS Permission",
            desc = "To automatically send emergency alerts without internet."
        )
        PermissionItem(
            icon = Icons.Outlined.NotificationsActive,
            title = "Critical Alerts",
            desc = "To ensure alerts are heard even in Do Not Disturb."
        )

        Spacer(Modifier.weight(1f))

        // --- UPDATED BUTTON ACTION ---
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.SEND_SMS
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "Grant All Permissions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navigateNext()
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Maybe Later", color = Color.Gray)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}