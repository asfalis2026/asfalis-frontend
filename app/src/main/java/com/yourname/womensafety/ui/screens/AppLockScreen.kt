package com.yourname.womensafety.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
import kotlinx.coroutines.launch

// Hidden emergency bypass: tap the lock icon 7 times
private const val EMERGENCY_TAP_COUNT = 7

@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appLockEnabled by remember { mutableStateOf(false) }
    var storedPin by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(true) }

    // Which secondary screen is open: null = main lock, "pin" = PIN entry, "emergency" = bypass
    var showScreen by remember { mutableStateOf<String?>(null) }

    // PIN state
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinErrorMsg by remember { mutableStateOf("Incorrect PIN") }

    // Emergency bypass hidden tap
    var lockTapCount by remember { mutableIntStateOf(0) }

    // ── Biometric prompt helper ─────────────────────────────────────────────────
    fun promptBiometric() {
        val activity = context as? FragmentActivity ?: run {
            showScreen = "pin"
            return
        }
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            showScreen = "pin"
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showScreen = "pin"
                    }
                }
                override fun onAuthenticationFailed() {}
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Asfalis")
            .setSubtitle("Use your fingerprint to continue")
            .setNegativeButtonText("Use PIN instead".trNonComposable())
            .build()

        prompt.authenticate(promptInfo)
    }

    // ── Load lock state ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        appLockEnabled = AppServiceLocator.appCache.isAppLockEnabled()
        storedPin = AppServiceLocator.appCache.getAppLockPin()
        if (!appLockEnabled) {
            onUnlocked()
        } else {
            isChecking = false
            promptBiometric()
        }
    }

    if (isChecking) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    // ── Root background: pure dark blend ────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
        // Subtle radial rose glow at top
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp).background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFE25F71).copy(0.07f), Color.Transparent),
                    radius = 800f
                )
            )
        )

        // ── Main lock screen ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScreen == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(100.dp))

                // ── Glowing Shield Button ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            lockTapCount++
                            if (lockTapCount >= EMERGENCY_TAP_COUNT) {
                                lockTapCount = 0
                                showScreen = "emergency"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(modifier = Modifier.size(190.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFE25F71).copy(0.15f), Color.Transparent))))
                    // Solid Red Circle Button
                    Box(
                        modifier = Modifier.size(130.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFE25F71), Color(0xFFA00000))))
                            .border(1.dp, Color(0xFFE25F71).copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                Text("ASFALIS".tr(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your Safety, Our Priority".tr(),
                    color = Color(0xFFE25F71),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(48.dp))

                // ── Unlock options ───────────────────────────────────────────────
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UnlockOption(
                        icon = Icons.Default.Fingerprint,
                        label = "Unlock with Fingerprint".tr(),
                        subLabel = "Quick and secure access".tr(),
                        tint = Color(0xFFE25F71),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { promptBiometric() }
                    )
                    UnlockOption(
                        icon = Icons.Default.Apps,
                        label = "Enter PIN".tr(),
                        subLabel = "Enter your 4 digit PIN".tr(),
                        tint = Color(0xFFE25F71),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            enteredPin = ""
                            pinError = false
                            showScreen = "pin"
                        }
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Footer ───────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(30.dp).height(1.dp).background(Color.White.copy(0.1f)))
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Outlined.Shield, null, tint = Color(0xFFE25F71), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Your security is our priority".tr(), color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.width(30.dp).height(1.dp).background(Color.White.copy(0.1f)))
                }
                Spacer(Modifier.height(40.dp))
            }
        }

        // ── PIN Entry Screen ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScreen == "pin",
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE25F71).copy(0.06f), Color.Transparent),
                        radius = 900f
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glow icon ring
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(80.dp).border(1.dp, Color(0xFFE25F71).copy(0.15f), CircleShape))
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                .background(Color(0xFFE25F71).copy(0.12f))
                                .border(1.dp, Color(0xFFE25F71).copy(0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pin, null, tint = Color(0xFFE25F71), modifier = Modifier.size(30.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Enter your PIN".tr(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Enter your 4-digit PIN to unlock Asfalis".tr(),
                        color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(40.dp))

                    // PIN dot indicators — bigger & Neon Rose filled
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        (0 until 4).forEach { index ->
                            val filled = index < enteredPin.length
                            Box(
                                modifier = Modifier.size(18.dp).clip(CircleShape)
                                    .background(
                                        if (pinError) Color(0xFFE25F71)
                                        else if (filled) Color(0xFFE25F71)
                                        else Color.White.copy(0.12f)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (filled || pinError) Color(0xFFE25F71) else Color.White.copy(0.25f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    if (pinError) {
                        Text(pinErrorMsg, color = Color(0xFFE25F71), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(36.dp))

                    NumericKeypad(
                        onDigit = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                pinError = false
                                if (enteredPin.length == 4) {
                                    if (enteredPin == storedPin) {
                                        onUnlocked()
                                    } else {
                                        pinError = true
                                        pinErrorMsg = "Incorrect PIN. Try again."
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                pinError = false
                            }
                        }
                    )

                    Spacer(Modifier.height(28.dp))

                    TextButton(onClick = { showScreen = null; lockTapCount = 0 }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFE25F71).copy(0.8f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back to Unlock".tr(), color = Color(0xFFE25F71).copy(0.8f), fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Emergency Bypass Screen ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScreen == "emergency",
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut()
        ) {
            var confirmText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(0xFFFFB74D).copy(0.12f))
                        .border(1.dp, Color(0xFFFFB74D).copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Emergency Access".tr(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This will disable App Lock and let you in. Type \".tr()ASFALIS\" to confirm.",
                    color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it.uppercase() },
                    label = { Text("Type ASFALIS to confirm".tr(), color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB74D), unfocusedBorderColor = Color.White.copy(0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (confirmText.trim() == "ASFALIS") {
                            scope.launch {
                                AppServiceLocator.appCache.setAppLockEnabled(false)
                                onUnlocked()
                            }
                        }
                    },
                    enabled = confirmText.trim() == "ASFALIS",
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Disable Lock & Enter".tr(), color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { showScreen = null; lockTapCount = 0 }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Go Back".tr(), color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Unlock option card ────────────────────────────────────────────────────────
@Composable
private fun UnlockOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subLabel: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF160E0E),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(0.28f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(tint.copy(0.2f), Color.Transparent)))
                    .border(1.dp, tint.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(subLabel, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = tint.copy(0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Numeric keypad ────────────────────────────────────────────────────────────
@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1".tr(), "2".tr(), "3".tr()),
        listOf("4".tr(), "5".tr(), "6".tr()),
        listOf("7".tr(), "8".tr(), "9".tr()),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        Surface(
                            onClick = { if (key == "⌫") onBackspace() else onDigit(key) },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = if (key == "⌫") Color.Transparent else Color(0xFF1C1212),
                            border = if (key == "⌫") null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(Icons.Default.Backspace, null, tint = Color(0xFFE25F71).copy(0.7f), modifier = Modifier.size(22.dp))
                                } else {
                                    Text(key, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
