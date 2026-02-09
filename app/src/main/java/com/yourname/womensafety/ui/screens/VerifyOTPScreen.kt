package com.yourname.womensafety.ui.screens

import android.content.Context // Added for SharedPrefs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Added
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun VerifyOTPScreen(navController: NavController) {
    val context = LocalContext.current // Added context
    var otpCode by remember { mutableStateOf(listOf("", "", "", "")) }
    val focusRequesters = remember { List(4) { FocusRequester() } }

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
            modifier = Modifier.padding(top = 16.dp).clip(CircleShape).background(Color.White.copy(0.08f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }

        Spacer(Modifier.height(32.dp))

        Text("Verification Code", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Enter the 4-digit code sent to your phone", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(40.dp))

        // OTP Input Boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            otpCode.forEachIndexed { index, char ->
                OutlinedTextField(
                    value = char,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1) {
                            val newCode = otpCode.toMutableList()
                            newCode[index] = newValue
                            otpCode = newCode

                            if (newValue.isNotEmpty() && index < 3) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp)
                        .focusRequester(focusRequesters[index]),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedBorderColor = Color.Red,
                        unfocusedBorderColor = Color.White.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        TextButton(
            onClick = {
                otpCode = listOf("", "", "", "")
                focusRequesters[0].requestFocus()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear All", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- FIXED LOGIC HERE ---
        Button(
            onClick = {
                // 1. SAVE THE LOGIN STATUS
                val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().putBoolean("is_logged_in", true).apply()

                // 2. NAVIGATE TO DASHBOARD (Changed from otp_status to dashboard to avoid crash)
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Verify & Proceed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = {
                    otpCode = listOf("", "", "", "")
                    focusRequesters[0].requestFocus()
                }
            ) {
                Text("Resend Code", color = Color.Gray)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}