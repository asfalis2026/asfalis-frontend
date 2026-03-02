package com.yourname.womensafety.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun VerifyOTPScreen(navController: NavController, phoneArg: String) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val uiState by authViewModel.uiState.collectAsState()

    var otpCode by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Timer State
    var ticks by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (ticks > 0) {
            delay(1000)
            ticks--
        }
    }

    // Navigate to dashboard on successful OTP verification
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate("dashboard") {
                popUpTo("app_splash") { inclusive = true }
            }
            authViewModel.clearError()
        }
    }

    // Resend OTP — Twilio re-sends the SMS; just show a confirmation toast
    LaunchedEffect(uiState.otpResent) {
        if (uiState.otpResent) {
            Toast.makeText(context, "OTP resent via SMS.", Toast.LENGTH_SHORT).show()
            authViewModel.clearError()
        }
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

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
        Text("Verify Phone", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("OTP sent via SMS to $phoneArg", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            otpCode.forEachIndexed { index, char ->
                OutlinedTextField(
                    value = char,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                            val newCode = otpCode.toMutableList()
                            newCode[index] = newValue
                            otpCode = newCode
                            if (newValue.isNotEmpty() && index < 5) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .focusRequester(focusRequesters[index]),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedBorderColor = Color(0xFFE10600),
                        unfocusedBorderColor = Color.White.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                otpCode = List(6) { "" }
                focusRequesters[0].requestFocus()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear All", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (ticks > 0) {
                Text("Resend code in ${ticks}s", color = Color.Gray)
            } else {
                TextButton(onClick = {
                    ticks = 60
                    authViewModel.resendOtp(phoneArg)
                }) {
                    Text("Resend OTP", color = Color(0xFFE10600), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                val code = otpCode.joinToString("")
                if (code.length == 6 && otpCode.all { it.isNotEmpty() }) {
                    authViewModel.verifyPhoneOtp(phoneArg, code)
                } else {
                    Toast.makeText(context, "Please enter the 6-digit code", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !uiState.isLoading && otpCode.all { it.isNotEmpty() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Verify & Finish", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}
