package com.yourname.womensafety.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

@Composable
fun OnboardingScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // --- AUTO-REDIRECT LOGIC ---
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("onboarding_complete", false)) {
            navController.navigate("login") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    val scale = animateFloatAsState(targetValue = 1f, label = "")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color(0xFF120000), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE10600).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /* SHIELD ICON */
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE10600), Color(0xFF8B0000))
                        )
                    )
                    .shadow(22.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Women Safety",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your safety. Always active.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            FeatureItem(
                icon = Icons.Filled.Warning,
                title = "Automatic SOS alert on unusual movement",
                subtitle = "Detects falls and impacts instantly"
            )

            FeatureItem(
                icon = Icons.Filled.LocationOn,
                title = "Real-time location sharing",
                subtitle = "Share your live location with trusted contacts"
            )

            FeatureItem(
                icon = Icons.Outlined.Shield,
                title = "24/7 protection monitoring",
                subtitle = "Background service always watching over you"
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- BUTTON LOGIC ---
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val hasFineLocation = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasSMS = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFineLocation && hasSMS) {
                        // Mark as complete and go to login
                        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putBoolean("onboarding_complete", true).apply()

                        navController.navigate("login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        // Go to permissions to finish setup
                        navController.navigate("permissions") {
                            // inclusive = false here because we want to be able to go
                            // back to onboarding from permissions if user cancels
                            popUpTo("onboarding") { inclusive = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE10600), Color(0xFFB00500))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GET STARTED",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE10600).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE10600),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 14.sp)
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}