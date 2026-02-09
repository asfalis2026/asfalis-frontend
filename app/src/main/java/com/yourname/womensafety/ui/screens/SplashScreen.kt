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

    // Use a derived state or local variable to ensure we read fresh data
    LaunchedEffect(key1 = true) {
        // Start animation immediately
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        // Wait for the animation and give the OS time to initialize SharedPreferences
        delay(1600)

        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = sharedPref.getBoolean("onboarding_complete", false)
        val isUserLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        // Clear the splash from the backstack completely using popUpTo(0)
        // This ensures the user can't "back" into the splash screen.
        when {
            !isOnboardingComplete -> {
                navController.navigate("onboarding") {
                    popUpTo(0) { inclusive = true }
                }
            }
            isUserLoggedIn -> {
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            }
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
                    listOf(Color.Black, Color(0xFF120000), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Raksha Logo",
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "RAKSHA",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                text = "Your Safety, Our Priority",
                color = Color.Gray.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}