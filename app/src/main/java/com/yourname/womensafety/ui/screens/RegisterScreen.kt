package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.ui.viewmodels.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val uiState by authViewModel.uiState.collectAsState()

    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(ALL_COUNTRIES.first()) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var country by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }

    // Navigate to OTP screen when successfully registered
    LaunchedEffect(uiState.registeredPhone) {
        uiState.registeredPhone?.let {
            val fullPhone = selectedCountry.dialCode + phoneInput.trim()
            navController.navigate("verify_otp?phone=${android.net.Uri.encode(fullPhone)}")
            authViewModel.clearError()
        }
    }

    // Show error messages
    val context = LocalContext.current
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(com.yourname.womensafety.ui.theme.BgDark, com.yourname.womensafety.ui.theme.BgMid, com.yourname.womensafety.ui.theme.BgEnd)
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 16.dp).size(42.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)).border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text("Create Account".tr(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Join the ASFALIS safety network".tr(), color = Color.Gray, fontSize = 16.sp)

            Spacer(Modifier.height(30.dp))

            AuthInput(value = fullName, onValueChange = { fullName = it }, label = "Full Name".tr(), icon = Icons.Default.Person)
            Spacer(Modifier.height(16.dp))
            AuthInput(value = country, onValueChange = { country = it }, label = "Country".tr(), icon = Icons.Default.Public)
            Spacer(Modifier.height(16.dp))
            PhoneInputRow(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it },
                phoneInput = phoneInput,
                onPhoneChanged = { phoneInput = it.filter { c -> c.isDigit() } }
            )
            Spacer(Modifier.height(16.dp))
            AuthInput(
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
            Spacer(Modifier.height(8.dp))
            PasswordCriteriaChecklist(password = password)

            Spacer(Modifier.height(24.dp))

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
                Column(modifier = Modifier.clickable { navController.navigate("terms_and_conditions?source=register") }) {
                    Text("I agree to the Terms & Conditions".tr(),
                        color = Color(0xFFE25F71),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                    Text("By registering you consent to SOS data collection for your safety.".tr(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fullPhone = selectedCountry.dialCode + phoneInput.trim()
                    if (password.length < 8 || !password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() }) {
                        passwordError = "Password must be at least 8 characters with uppercase, lowercase, and digit.".trNonComposable()
                        return@Button
                    }
                    authViewModel.registerWithPhone(fullName, fullPhone, password, country)
                },
                enabled = termsAccepted && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE25F71),
                    disabledContainerColor = Color(0xFFE25F71).copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register & Send OTP".tr(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = if (termsAccepted) Color.White else Color.White.copy(0.5f))
                }
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
    isPassword: Boolean = false,
    errorMessage: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFE25F71)) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { 
            { Text(it, color = Color(0xFFE25F71), fontSize = 12.sp) } 
        },
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFE25F71),
            unfocusedBorderColor = Color.White.copy(0.1f)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
fun PasswordCriteriaChecklist(password: String) {
    val hasMinLen = password.length >= 8
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text("Password must contain:".tr(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                CriteriaItem(text = "8+ characters".tr(), isValid = hasMinLen)
                CriteriaItem(text = "1 uppercase".tr(), isValid = hasUpper)
            }
            Column {
                CriteriaItem(text = "1 lowercase".tr(), isValid = hasLower)
                CriteriaItem(text = "1 digit".tr(), isValid = hasDigit)
            }
        }
    }
}

@Composable
fun CriteriaItem(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isValid) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, color = if (isValid) Color(0xFF4CAF50) else Color.Gray, fontSize = 11.sp)
    }
}
