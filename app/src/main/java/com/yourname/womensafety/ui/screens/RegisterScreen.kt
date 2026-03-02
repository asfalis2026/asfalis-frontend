package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun RegisterScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    var fullName by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(ALL_COUNTRIES.first()) }
    var password by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 16.dp).clip(CircleShape).background(Color.White.copy(0.08f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }

            Spacer(Modifier.height(20.dp))

            Text("Create Account", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Join the ASFALIS safety network", color = Color.Gray, fontSize = 16.sp)

            Spacer(Modifier.height(30.dp))

            AuthInput(value = fullName, onValueChange = { fullName = it }, label = "Full Name", icon = Icons.Default.Person)
            Spacer(Modifier.height(16.dp))
            AuthInput(value = country, onValueChange = { country = it }, label = "Country", icon = Icons.Default.Public)
            Spacer(Modifier.height(16.dp))
            PhoneInputRow(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it },
                phoneInput = phoneInput,
                onPhoneChanged = { phoneInput = it.filter { c -> c.isDigit() } }
            )
            Spacer(Modifier.height(16.dp))
            AuthInput(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fullPhone = selectedCountry.dialCode + phoneInput.trim()
                    navController.navigate("verify_otp/$fullPhone")
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Register & Send OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFE10600)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFE10600),
            unfocusedBorderColor = Color.White.copy(0.1f)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}
