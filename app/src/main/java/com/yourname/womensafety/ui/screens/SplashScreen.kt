package com.yourname.womensafety.ui.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.R
import kotlinx.coroutines.delay

@Composable
fun AppSplashScreen(navController: NavController) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(key1 = true) {
        // 1. Logo Animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // 2. Branding Display Duration
        delay(1600)

        // 3. Load State Flags
        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = sharedPref.getBoolean("onboarding_complete", false)
        val arePermissionsGranted = sharedPref.getBoolean("permissions_granted", false)
        val isUserLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        // 4. MASTER NAVIGATION LOGIC
        // We check these in order of a new user's journey.
        when {
            // Journey Start: Needs to see Get Started
            !isOnboardingComplete -> {
                navController.navigate("onboarding") {
                    popUpTo(0) { inclusive = true }
                }
            }
            // Journey Step 2: Needs to grant permissions
            !arePermissionsGranted -> {
                navController.navigate("permissions") {
                    popUpTo(0) { inclusive = true }
                }
            }
            // Journey Complete: User is fully verified
            isUserLoggedIn -> {
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            }
            // Journey Step 3: Seen intro and permissions, but needs to Login
            else -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color(0xFF1A0000), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Asfalis Logo",
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "ASFALIS",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )

            Text(
                text = "Your Safety, Our Priority",
                color = Color(0xFFE10600).copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}