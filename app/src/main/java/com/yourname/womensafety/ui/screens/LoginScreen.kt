package com.yourname.womensafety.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Toggle between "Login" and "Register" view
    var isLoginMode by remember { mutableStateOf(true) }

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    // Prevent going back to Splash; close app instead
    BackHandler { (context as? Activity)?.finish() }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // --- LOGO SECTION ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE10600).copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFFE10600),
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (isLoginMode) "Welcome back" else "Create Account",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isLoginMode) "Sign in to continue" else "Join our safety network",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(40.dp))

            // --- DYNAMIC FORM ---
            if (!isLoginMode) {
                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    icon = Icons.Default.Person
                )
                Spacer(Modifier.height(16.dp))
                AuthTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = "Country",
                    icon = Icons.Default.Public
                )
                Spacer(Modifier.height(16.dp))
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                icon = Icons.Default.Email
            )

            Spacer(Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Default.Lock,
                isPassword = true
            )

            if (isLoginMode) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { /* Implement Forgot Password */ }) {
                        Text("Forgot Password?", color = Color(0xFFE10600), fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            // --- PRIMARY ACTION ---
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)

                        if (isLoginMode) {
                            // 1. RETURNING USER: Show Popup & Go to Dashboard
                            val userName = email.substringBefore("@")
                            Toast.makeText(context, "Welcome back, $userName!", Toast.LENGTH_SHORT).show()

                            // Save Login State
                            sharedPref.edit().putBoolean("is_logged_in", true).apply()

                            navController.navigate("dashboard") {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            // 2. NEW USER: Go to OTP verification
                            navController.navigate("verify_otp/$email")
                        }
                    } else {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isLoginMode) "Login" else "Get Verification Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- TOGGLE MODE ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLoginMode) "New to ASFALIS?" else "Already have an account?",
                    color = Color.Gray
                )
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isLoginMode = !isLoginMode
                }) {
                    Text(
                        text = if (isLoginMode) "Register" else "Login",
                        color = Color(0xFFE10600),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFE10600)) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(0.05f),
            unfocusedContainerColor = Color.White.copy(0.05f),
            focusedBorderColor = Color(0xFFE10600),
            unfocusedBorderColor = Color.White.copy(0.1f),
            cursorColor = Color(0xFFE10600)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}