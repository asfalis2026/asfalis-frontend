package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SignInWithPhone(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF1A0000),
            Color(0xFF330000)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navController.popBackStack()
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Sign in with Phone",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "We'll send you a verification code",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Phone Number",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) },
            placeholder = { Text("+1 (555) 000-0000", color = Color.White.copy(0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(0.05f),
                unfocusedContainerColor = Color.White.copy(0.05f),
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.White.copy(0.1f)
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Standard message rates may apply",
            color = Color.DarkGray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(40.dp))

        // --- NAVIGATION TO VERIFY OTP ---
        Button(
            onClick = {
                if (phoneNumber.isNotBlank()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // We navigate to OTP verification.
                    // The 'is_logged_in' flag should be saved in the VerifyOTPScreen code.
                    navController.navigate("verify_otp")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Send OTP",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}