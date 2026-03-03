package com.yourname.womensafety.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.womensafety.ui.viewmodels.ContactsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactOtpVerificationScreen(
    contactId: String,
    phone: String,
    name: String,
    initialExpiresInSeconds: Int,
    onBack: () -> Unit,
    onVerificationSuccess: () -> Unit,
    contactsViewModel: ContactsViewModel = viewModel(factory = ContactsViewModel.Factory)
) {
    var otpCode by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var isPrimary by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(initialExpiresInSeconds) }
    var isExpired by remember { mutableStateOf(false) }
    var canResend by remember { mutableStateOf(false) }
    var verificationAttempted by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(false) }
    
    val isLoading by contactsViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by contactsViewModel.errorMessage.collectAsStateWithLifecycle()

    // Timer countdown
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds <= 270) { // Enable resend after 30 seconds
                canResend = true
            }
        } else {
            isExpired = true
        }
    }

    // Format timer display
    val timerDisplay = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        "%02d:%02d".format(minutes, seconds)
    }

    // Handle verification success
    LaunchedEffect(isLoading, errorMessage) {
        // If we were loading and now we're not, check if it was successful
        if (wasLoading && !isLoading && verificationAttempted) {
            if (errorMessage == null) {
                // Verification succeeded
                delay(500) // Brief delay for UX and to let loadContacts complete
                onVerificationSuccess()
            }
            verificationAttempted = false
        }
        wasLoading = isLoading
    }

    // Auto-focus first OTP field
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Phone Number", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A0000)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A0000), Color.Black)
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // Info Section
                Text(
                    "We've sent a 6-digit code to:",
                    color = Color.White.copy(0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    phone,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask $name to share the code with you.",
                    color = Color.White.copy(0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                // Timer Display
                if (!isExpired) {
                    Text(
                        "Code expires in: $timerDisplay",
                        color = Color(0xFFE10600),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        "Code expired",
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))

                // OTP Input - 6 separate boxes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                                    contactsViewModel.clearError()
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
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                errorBorderColor = Color.Red,
                                cursorColor = Color(0xFFE10600)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            isError = errorMessage != null
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Error message
                errorMessage?.let {
                    Text(
                        it,
                        color = Color(0xFFE10600),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Clear all button
                TextButton(
                    onClick = {
                        otpCode = listOf("", "", "", "", "", "")
                        focusRequesters[0].requestFocus()
                        contactsViewModel.clearError()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear All", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Primary Contact Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFE10600),
                            uncheckedColor = Color.White.copy(0.5f),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Set as primary emergency contact",
                        color = Color.White.copy(0.8f),
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Verify Button
                Button(
                    onClick = {
                        val code = otpCode.joinToString("")
                        if (code.length == 6) {
                            verificationAttempted = true
                            contactsViewModel.verifyContactOtp(contactId, code, isPrimary)
                        }
                    },
                    enabled = otpCode.all { it.isNotEmpty() } && !isLoading && !isExpired,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE10600),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "Verify & Add Contact",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Resend Button
                TextButton(
                    onClick = {
                        contactsViewModel.resendContactOtp(contactId) { newExpiresInSeconds ->
                            remainingSeconds = newExpiresInSeconds
                            isExpired = false
                            canResend = false
                            otpCode = listOf("", "", "", "", "", "")
                        }
                    },
                    enabled = canResend && !isLoading,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        if (canResend) "Didn't receive code? Resend Code" 
                        else "Resend available in ${remainingSeconds - 270}s",
                        color = if (canResend) Color(0xFFE10600) else Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Additional help text
                if (isExpired) {
                    Text(
                        "The code has expired. Please request a new one.",
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}
