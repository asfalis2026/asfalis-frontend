package com.yourname.womensafety.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.yourname.womensafety.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val uiState by authViewModel.uiState.collectAsState()

    var isLoginMode by rememberSaveable { mutableStateOf(true) }

    // Phone-based form states
    var selectedCountryCode by rememberSaveable { mutableStateOf(ALL_COUNTRIES.first().isoCode) }
    val selectedCountry = ALL_COUNTRIES.find { it.isoCode == selectedCountryCode } ?: ALL_COUNTRIES.first()
    
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }

    // Prevent going back to Splash; close app instead
    BackHandler { (context as? Activity)?.finish() }

    // Navigate on successful login/register
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
            authViewModel.clearError()
        }
    }

    // After Step 1 register — Twilio has sent the OTP SMS; navigate to OTP screen
    LaunchedEffect(uiState.registeredPhone) {
        uiState.registeredPhone?.let { phoneNumber ->
            navController.navigate("verify_otp?phone=${Uri.encode(phoneNumber)}")
            authViewModel.clearError()
        }
    }

    // PHONE_NOT_VERIFIED during login → toast the error, then redirect to OTP screen
    LaunchedEffect(uiState.unverifiedPhone) {
        uiState.unverifiedPhone?.let { phoneNumber ->
            Toast.makeText(context, "Phone not verified. Sending OTP to $phoneNumber…".trNonComposable(), Toast.LENGTH_LONG).show()
            authViewModel.resendOtp(phoneNumber) // send a fresh OTP in case the original expired
            delay(1500L) // let the toast be readable before switching screens
            navController.navigate("verify_otp?phone=${Uri.encode(phoneNumber)}")
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

    // Forgot password OTP dispatched by Twilio — navigate to reset-password screen
    LaunchedEffect(uiState.forgotPasswordSent) {
        uiState.forgotPasswordSent?.let { phoneNumber ->
            navController.navigate("reset_password/${Uri.encode(phoneNumber)}")
            authViewModel.clearError()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 30.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // --- LOGO SECTION ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE25F71).copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFFE25F71),
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (isLoginMode) "Welcome back".tr() else "Create Account".tr(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isLoginMode) "Sign in to continue".tr() else "Join our safety network".tr(),
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(40.dp))

            // --- REGISTER-ONLY FIELDS ---
            if (!isLoginMode) {
                AuthTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name".tr(),
                    icon = Icons.Default.Person
                )
                Spacer(Modifier.height(16.dp))
                AuthTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = "Country".tr(),
                    icon = Icons.Default.Public
                )
                Spacer(Modifier.height(16.dp))
            }

            // --- PHONE NUMBER FIELD (shared by login & register) ---
            PhoneInputRow(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountryCode = it.isoCode },
                phoneInput = phoneInput,
                onPhoneChanged = { phoneInput = it.filter { c -> c.isDigit() } }
            )

            Spacer(Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = null
                },
                label = "Password".tr(),
                icon = Icons.Default.Lock,
                isPassword = true,
                errorMessage = passwordError
            )

            if (!isLoginMode) {
                Spacer(Modifier.height(8.dp))
                PasswordCriteriaChecklist(password = password)
            }

            if (isLoginMode) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = {
                        val phoneNumber = selectedCountry.dialCode + phoneInput.trim()
                        if (phoneInput.isNotBlank()) {
                            authViewModel.forgotPassword(phoneNumber)
                        } else {
                            Toast.makeText(context, "Enter your phone number first".trNonComposable(), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Forgot Password?".tr(), color = Color(0xFFE25F71), fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Terms & Conditions Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFE25F71),
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.clickable { navController.navigate("terms_and_conditions?source=login") }) {
                    Text("I agree to the Terms & Conditions".tr(),
                        color = Color(0xFFE25F71),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                    Text("By proceeding you consent to SOS data collection for your safety.".tr(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- PRIMARY ACTION ---
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fullPhone = selectedCountry.dialCode + phoneInput.trim()
                    if (isLoginMode) {
                        if (phoneInput.isNotBlank() && password.isNotBlank()) {
                            authViewModel.loginWithPhone(fullPhone, password)
                        } else {
                            Toast.makeText(context, "Please enter phone number and password".trNonComposable(), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (fullName.isNotBlank() && phoneInput.isNotBlank() && password.isNotBlank() && country.isNotBlank()) {
                            if (password.length < 8 || !password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() }) {
                                passwordError = "Password must be at least 8 characters with uppercase, lowercase, and digit.".trNonComposable()
                                return@Button
                            }
                            authViewModel.registerWithPhone(fullName, fullPhone, password, country)
                        } else {
                            Toast.makeText(context, "Please fill in all fields".trNonComposable(), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !uiState.isLoading && termsAccepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Log In".tr() else "Get Verification Code".tr(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- TOGGLE MODE ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLoginMode) "New to ASFALIS?".tr() else "Already have an account?".tr(),
                    color = Color.Gray
                )
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isLoginMode = !isLoginMode
                }) {
                    Text(
                        text = if (isLoginMode) "Register".tr() else "Log In".tr(),
                        color = Color(0xFFE25F71),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        uiState.handsetTransfer?.let { transferState ->
            var remainingSeconds by remember(transferState.requestId, transferState.phoneNumber) {
                mutableStateOf(transferState.remainingSeconds ?: 0)
            }

            LaunchedEffect(transferState.requestId, transferState.confirmHandoverRequired) {
                if (!transferState.confirmHandoverRequired && remainingSeconds > 0) {
                    while (remainingSeconds > 0) {
                        delay(1000)
                        remainingSeconds--
                    }
                }
            }

            val transferEnabled = transferState.confirmHandoverRequired || remainingSeconds <= 0

            Dialog(onDismissRequest = { }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF160E0E),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Device Change Detected".tr(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Text(
                            text = if (transferEnabled)
                                "Transfer window is open. You can move your account to this device now.".tr()
                            else
                                "Login on this new device is blocked for safety. You can transfer after the waiting period.".tr(),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        if (!transferEnabled) {
                            Text(text = "Time remaining: ".tr() + formatDuration(remainingSeconds),
                                color = Color(0xFFE25F71),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        transferState.eligibleAt?.let {
                            Text(text = "Eligible at: $it".tr(),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val fullPhone = selectedCountry.dialCode + phoneInput.trim()
                                if (password.isBlank()) {
                                    Toast.makeText(context, "Enter password to confirm transfer".trNonComposable(), Toast.LENGTH_SHORT).show()
                                } else {
                                    authViewModel.confirmHandsetTransfer(fullPhone, password)
                                }
                            },
                            enabled = transferEnabled && !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "Transfer account to this device".tr(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = { authViewModel.clearHandsetTransferState() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Close".tr())
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

/** Inline phone number row: country-code badge + digit-only input. */
@Composable
fun PhoneInputRow(
    selectedCountry: CountryDialCode,
    onCountrySelected: (CountryDialCode) -> Unit,
    phoneInput: String,
    onPhoneChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Phone Number".tr(), color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country code chip
            OutlinedButton(
                onClick = { expanded = true; search = "" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("${selectedCountry.flag} ${selectedCountry.dialCode}".tr(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }

            // Phone digits field
            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("9876543210".tr(), color = Color.Gray.copy(0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(0.05f),
                    unfocusedContainerColor = Color.White.copy(0.05f),
                    focusedBorderColor = Color(0xFFE25F71),
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    cursorColor = Color(0xFFE25F71)
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }
    }

    // Country picker dialog (reuses existing ALL_COUNTRIES list from TrustedContacts)
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 480.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF160E0E))
                        .padding(16.dp)
                ) {
                    Text("Select Country".tr(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search country...".tr(), color = Color.Gray) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(0.05f),
                            unfocusedContainerColor = Color.White.copy(0.05f),
                            focusedBorderColor = Color(0xFFE25F71),
                            unfocusedBorderColor = Color.White.copy(0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = ALL_COUNTRIES.filter {
                        it.name.contains(search, ignoreCase = true) || it.dialCode.contains(search)
                    }
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(filtered, key = { it.isoCode }) { c ->
                            val isSel = c.isoCode == selectedCountry.isoCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFFE25F71).copy(0.15f) else Color.Transparent)
                                    .clickable {
                                        onCountrySelected(c)
                                        expanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(c.flag, fontSize = 22.sp)
                                Text(
                                    c.name,
                                    color = if (isSel) Color(0xFFE25F71) else Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(c.dialCode, color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
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
    isPassword: Boolean = false,
    errorMessage: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFE25F71)) },
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
        isError = errorMessage != null,
        supportingText = errorMessage?.let { 
            { Text(it, color = Color(0xFFE25F71), fontSize = 12.sp) } 
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(0.05f),
            unfocusedContainerColor = Color.White.copy(0.05f),
            focusedBorderColor = Color(0xFFE25F71),
            unfocusedBorderColor = Color.White.copy(0.1f),
            cursorColor = Color(0xFFE25F71)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}



