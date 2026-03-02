package com.yourname.womensafety.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.data.AppServiceLocator
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // --- UPDATED NAVIGATION LOGIC ---
    fun navigateToLogin() {
        scope.launch {
            // Mark permissions as handled so SplashViewModel DataStore flag is set
            AppServiceLocator.tokenManager.setPermissionsGranted()
        }
        // ALWAYS go to login — never skip to Dashboard from here
        navController.navigate("login") {
            popUpTo("permissions") { inclusive = true }
        }
    }

    // --- SYSTEM PERMISSION LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // After system dialog closes, move to login regardless of choice
        navigateToLogin()
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

        // Shield Icon Header
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
            text = "To keep you safe, ASFALIS needs access to these vital features.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(48.dp))

        // Permission Items List
        PermissionItem(
            icon = Icons.Outlined.LocationOn,
            title = "Location Access",
            desc = "To share your live coordinates with emergency contacts."
        )
        PermissionItem(
            icon = Icons.Outlined.NotificationsActive,
            title = "Critical Alerts",
            desc = "To ensure SOS sounds are heard even in silent mode."
        )

        Spacer(Modifier.weight(1f))

        // --- PRIMARY BUTTON ACTION ---
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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

        // Secondary Skip Button
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navigateToLogin()
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