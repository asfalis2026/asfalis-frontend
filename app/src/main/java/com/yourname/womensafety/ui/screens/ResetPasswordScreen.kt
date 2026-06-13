package com.yourname.womensafety.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.ui.viewmodels.AuthViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable

@Composable
fun ResetPasswordScreen(navController: NavController, phoneArg: String) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val uiState by authViewModel.uiState.collectAsState()

    // OTP entry (6 boxes)
    var otpCode by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Password fields
    var newPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Navigate back to login on success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Password reset successfully! Please log in.".trNonComposable(), Toast.LENGTH_LONG).show()
            navController.navigate("login") { popUpTo(0) { inclusive = true } }
            authViewModel.clearError()
        }
    }

    // Show errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE25F71).copy(0.25f))
                    .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(text = "Reset Password".tr(),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(text = "Enter the OTP sent to $phoneArg,\nthen set your new password.".tr(),
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(36.dp))

        // ── OTP row ──────────────────────────────────────────────────────────
        Text("Verification Code".tr(),
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            otpCode.forEachIndexed { index, char ->
                OutlinedTextField(
                    value = char,
                    onValueChange = { newValue ->
                        when {
                            // Digit entered: fill this box and move forward
                            newValue.length == 1 && newValue.all { it.isDigit() } -> {
                                val newCode = otpCode.toMutableList()
                                newCode[index] = newValue
                                otpCode = newCode
                                if (index < 5) focusRequesters[index + 1].requestFocus()
                            }
                            // Backspace on filled box: clear it
                            newValue.isEmpty() && char.isNotEmpty() -> {
                                val newCode = otpCode.toMutableList()
                                newCode[index] = ""
                                otpCode = newCode
                            }
                            // Backspace on empty box: move focus left and clear that box
                            newValue.isEmpty() && char.isEmpty() && index > 0 -> {
                                val newCode = otpCode.toMutableList()
                                newCode[index - 1] = ""
                                otpCode = newCode
                                focusRequesters[index - 1].requestFocus()
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
                        focusedBorderColor = Color(0xFFE25F71),
                        unfocusedBorderColor = Color.White.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── New password ──────────────────────────────────────────────────────
        OutlinedTextField(
            value = newPassword,
            onValueChange = { 
                newPassword = it
                passwordError = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New Password".tr(), color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFFE25F71)) },
            trailingIcon = {
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(
                        if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError != null,
            supportingText = passwordError?.let {
                { Text(it, color = Color(0xFFE25F71), fontSize = 12.sp) }
            },
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

        Spacer(Modifier.height(8.dp))
        com.yourname.womensafety.ui.screens.PasswordCriteriaChecklist(password = newPassword)

        Spacer(Modifier.height(16.dp))

        // ── Confirm password ──────────────────────────────────────────────────
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm Password".tr(), color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFFE25F71)) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null, tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                    Text("Passwords do not match".tr(), color = Color(0xFFE25F71), fontSize = 12.sp)
                }
            },
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

        Spacer(Modifier.height(32.dp))

        // ── Submit ────────────────────────────────────────────────────────────
        Button(
            onClick = {
                val code = otpCode.joinToString("")
                when {
                    code.length < 6 ->
                        Toast.makeText(context, "Enter the 6-digit OTP".trNonComposable(), Toast.LENGTH_SHORT).show()
                    newPassword.length < 8 || !newPassword.any { it.isUpperCase() } || !newPassword.any { it.isLowerCase() } || !newPassword.any { it.isDigit() } ->
                        passwordError = "Password must be at least 8 characters with uppercase, lowercase, and digit.".trNonComposable()
                    newPassword != confirmPassword ->
                        Toast.makeText(context, "Passwords do not match".trNonComposable(), Toast.LENGTH_SHORT).show()
                    else ->
                        authViewModel.resetPassword(phoneArg, code, newPassword)
                }
            },
            enabled = !uiState.isLoading,
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
                Text("Reset Password".tr(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
