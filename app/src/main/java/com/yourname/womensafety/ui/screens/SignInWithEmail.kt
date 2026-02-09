package com.yourname.womensafety.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SignInWithEmail(navController: NavController) {
    // Context needed for SharedPreferences
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
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
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }

        Spacer(Modifier.height(32.dp))

        Text("Sign in with Email", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Enter your credentials to continue", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(40.dp))

        // Email Input
        Text("Email Address", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
            placeholder = { Text("your.email@example.com", color = Color.White.copy(0.6f)) },
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

        Spacer(Modifier.height(20.dp))

        // Password Input
        Text("Password", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
            placeholder = { Text("Enter your password", color = Color.White.copy(0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
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

        // Forgot Password
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { /* Handle forgot password */ }) {
                Text("Forgot password?", color = Color.Red, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sign In Button
        Button(
            onClick = {
                // FIXED LOGIC: Save login state so the app remembers the user
                val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putBoolean("is_logged_in", true).apply()

                // Navigate and clear the login stack
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Sign In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}