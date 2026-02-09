package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun OTPStatusScreen(navController: NavController) {
    // Automatically transition to dashboard after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("dashboard") {
            popUpTo(0) { inclusive = true }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF1A0000))
    )

    Column(
        modifier = Modifier.fillMaxSize().background(backgroundGradient),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(Color(0xFF00C853).copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF00C853), modifier = Modifier.size(50.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text("OTP Verified", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Setting up your secure session...", color = Color.Gray, fontSize = 14.sp)
    }
}