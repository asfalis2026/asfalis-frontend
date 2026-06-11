package com.yourname.womensafety.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.womensafety.data.IotAction
import com.yourname.womensafety.data.IotCommand
import com.yourname.womensafety.data.IotEventBus
import com.yourname.womensafety.ui.viewmodels.SosViewModel
import kotlinx.coroutines.delay
import com.yourname.womensafety.utils.tr
import android.content.Intent
import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast

@Suppress("MissingPermission")
@Composable
fun SOSAlertScreen(
    triggerType: String = "manual",
    /** Pre-existing alert ID from Auto SOS (ML detection). When set, skips triggerSos(). */
    existingAlertId: String? = null,
    onSafe: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sosViewModel: SosViewModel = viewModel(factory = SosViewModel.Factory)
    val uiState by sosViewModel.uiState.collectAsStateWithLifecycle()

    var ticks by remember { mutableIntStateOf(10) }
    var countdownStarted by remember { mutableStateOf(false) }
    var pendingHomeNavigation by remember { mutableStateOf(false) }
    var wearableCancelled by remember { mutableStateOf(false) }

    // ── Determine trigger type (hardware team patch) ──────────────────────────
    val isAutoFall = triggerType == "auto_fall" || triggerType == "auto_shake"
    val isProximity = triggerType == "hardware_distress"
    val isManual = triggerType == "manual" || triggerType == "iot_button"

    // Colour theme depends on trigger type
    val accentColor = when {
        isProximity -> Color(0xFFFF6B00)   // Orange for device separated
        isAutoFall  -> Color(0xFFE25F71)   // Red for auto detection
        else        -> Color(0xFFE25F71)   // Red for manual
    }

    // ── Init: either use existing alertId or trigger new SOS ─────────────────
    LaunchedEffect(Unit) {
        if (existingAlertId != null) {
            sosViewModel.initWithExistingAlert(existingAlertId)
            // Hardware team patch: fire bracelet haptic for auto-detected SOS
            IotEventBus.sendCommand(IotCommand.TriggerFeedback)
        } else {
            Log.d("SOSAlertScreen", "Triggering SOS: type=$triggerType")
            sosViewModel.triggerSos(triggerType = triggerType)
        }
    }

    // Collect wearable cancel events
    LaunchedEffect(Unit) {
        IotEventBus.events.collect { action ->
            if (action is IotAction.Cancelled) {
                wearableCancelled = true
            }
        }
    }

    // Start countdown once trigger succeeds
    LaunchedEffect(uiState.alertId, uiState.isTriggering) {
        if (uiState.alertId != null && !uiState.isTriggering && !countdownStarted) {
            countdownStarted = true
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
            Log.d("SOSAlertScreen", "Trigger confirmed alertId=${uiState.alertId} — starting countdown")
            while (ticks > 0 && !uiState.isCancelled && !wearableCancelled) {
                delay(1000L)
                ticks--
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
            if (ticks == 0 && !uiState.isCancelled && !wearableCancelled) {
                sosViewModel.sendNow()
            }
        }
    }

    // NOTE: backend polling (startPolling) only updates isSent/isCancelled via uiState.
    // The visual countdown ticks are driven purely by the local coroutine above
    // to guarantee every number 10→9→8→…→0 appears without skipping.

    fun handleBackToHome() {
        val isActiveAndUnsent = !uiState.isCancelled && !uiState.isSent && !uiState.isSending
        val shouldSendBeforeLeaving = isActiveAndUnsent && uiState.alertId != null
        when {
            uiState.isTriggering || uiState.isConnectionTimeout -> {
                sosViewModel.abortTrigger()
            }
            shouldSendBeforeLeaving -> {
                pendingHomeNavigation = true
                sosViewModel.sendNow()
            }
            isActiveAndUnsent && uiState.alertId == null -> onSafe()
            else -> {
                // Already sent or cancelled — submit feedback then navigate
                if (isAutoFall && uiState.isSent) {
                    uiState.alertId?.let { sosViewModel.submitFeedback(it, isFalseAlarm = false) }
                }
                onSafe()
            }
        }
    }

    BackHandler(onBack = { handleBackToHome() })

    LaunchedEffect(pendingHomeNavigation, uiState.isSent) {
        if (pendingHomeNavigation && uiState.isSent) {
            // Submit feedback for auto SOS: dispatched = real danger
            if (isAutoFall) {
                uiState.alertId?.let { sosViewModel.submitFeedback(it, isFalseAlarm = false) }
            }
            onSafe()
        }
    }

    LaunchedEffect(pendingHomeNavigation, uiState.alertId, uiState.isSent, uiState.isCancelled, uiState.isSending) {
        if (pendingHomeNavigation && uiState.alertId != null && !uiState.isSent && !uiState.isCancelled && !uiState.isSending) {
            sosViewModel.sendNow()
        }
    }

    LaunchedEffect(pendingHomeNavigation, uiState.errorMessage) {
        if (pendingHomeNavigation && uiState.errorMessage != null) {
            pendingHomeNavigation = false
        }
    }

    // Navigate back when cancelled — hardware team patch: only submit feedback for auto_fall/auto_shake
    LaunchedEffect(uiState.isCancelled) {
        if (uiState.isCancelled) {
            if (isAutoFall) {
                uiState.alertId?.let { sosViewModel.submitFeedback(it, isFalseAlarm = true) }
            }
            onSafe()
        }
    }

    LaunchedEffect(wearableCancelled) {
        if (wearableCancelled) {
            if (isAutoFall) {
                uiState.alertId?.let { sosViewModel.submitFeedback(it, isFalseAlarm = true) }
            }
            onSafe()
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "sos_alert".tr())

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha".tr()
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale".tr()
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "ring_rotation".tr()
    )
    val warnBlink by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "warn_blink".tr()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0000),
                        if (isProximity) Color(0xFF1A0D00) else Color(0xFF160E0E),
                        Color.Black
                    )
                )
            )
    ) {
        // Background radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = pulseAlpha * 0.6f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        // Close / Back button (top left)
        IconButton(
            onClick = { handleBackToHome() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Back to Home".tr(),
                tint = Color.White.copy(0.7f),
                modifier = Modifier.size(26.dp)
            )
        }

        // Trigger type badge (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(0.15f))
                .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = when {
                    isProximity -> "PROXIMITY".tr()
                    isAutoFall  -> "AUTO ML".tr()
                    else        -> "MANUAL".tr()
                },
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))


            // ── Alert Icon with pulsing rings ─────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size((100 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = pulseAlpha * 0.4f))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.5.dp, accentColor.copy(0.3f), CircleShape)
                )
                // Spinning arc (only for auto/manual, not proximity)
                if (!isProximity) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer { rotationZ = ringRotation }
                    ) {
                        CircularProgressIndicator(
                            progress = { 0.25f },
                            modifier = Modifier.fillMaxSize(),
                            color = accentColor,
                            strokeWidth = 2.5.dp,
                            trackColor = Color.Transparent
                        )
                    }
                }
                // Icon
                Icon(
                    imageVector = when {
                        isProximity -> Icons.Default.BluetoothDisabled
                        isAutoFall  -> Icons.Default.Warning
                        else        -> Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(text = "ALERT TRIGGERED".tr(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    isProximity -> "DEVICE SEPARATED".tr()
                    isAutoFall -> "UNUSUAL MOVEMENT DETECTED".tr()
                    else -> "EMERGENCY SOS TRIGGERED".tr()
                },
                color = Color.Red.copy(0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = when {
                    isProximity -> "Your wearable has disconnected or\nmoved out of range.".tr()
                    isAutoFall -> "A sudden impact or fall was detected by\nyour device sensors.".tr()
                    else -> "You have manually triggered an\nemergency SOS alert.".tr()
                },
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(36.dp))

            var contactToCall by remember { mutableStateOf<String?>(null) }
            val callPermissionMsg = "Call permission is required to dial directly.".tr()
            val callPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted && contactToCall != null) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:${contactToCall}")
                    }
                    context.startActivity(intent)
                    contactToCall = null
                } else {
                    Toast.makeText(context, callPermissionMsg, Toast.LENGTH_SHORT).show()
                }
            }

            // ── Countdown Circle / Contacts List ──────────────────────────────
            if (ticks > 0 || !countdownStarted) {
                Box(contentAlignment = Alignment.Center) {
                if (uiState.isTriggering && !countdownStarted) {
                    // Waiting for server — show indeterminate spinner
                    CircularProgressIndicator(
                        modifier = Modifier.size(190.dp),
                        color = accentColor,
                        strokeWidth = 7.dp,
                        trackColor = Color.White.copy(0.05f)
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { ticks / 10f },
                        modifier = Modifier.size(190.dp),
                        color = accentColor,
                        strokeWidth = 7.dp,
                        trackColor = Color.White.copy(0.05f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.isTriggering && !countdownStarted) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = accentColor,
                            strokeWidth = 3.dp,
                            trackColor = Color.Transparent
                        )
                    } else {
                        Text(
                            text = ticks.toString(),
                            color = Color.White,
                            fontSize = 78.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = if (uiState.isTriggering && !countdownStarted) "WAKING UP SERVER..." else "SECONDS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            } else {
                // Show Contacts List
                val contacts = uiState.contacts
                if (contacts.isNotEmpty()) {
                    Text(
                        text = "EMERGENCY CONTACTS".tr(),
                        color = Color.White.copy(0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contacts) { contact ->
                            // Match this contact's delivery status from the report
                            val report = uiState.deliveryReport.firstOrNull { rpt ->
                                rpt.phone == contact.phone ||
                                rpt.phone.endsWith(contact.phone.takeLast(7))
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White.copy(0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Contact icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    report == null -> Color.White.copy(0.08f)
                                                    report.delivered -> Color(0xFF4CAF50).copy(0.18f)
                                                    else -> Color(0xFFFF3333).copy(0.18f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    // Name + primary label
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (contact.isPrimary) {
                                            Text(
                                                text = "Primary".tr(),
                                                color = Color(0xFF4CAF50),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // ── Delivery status badge ──────────────────
                                    val (badgeText, badgeColor) = when {
                                        uiState.isSending -> Pair("Sending…".tr(), Color(0xFFFFAA00))
                                        report == null && uiState.isSent -> Pair("Pending".tr(), Color.Gray)
                                        report == null -> Pair("", Color.Transparent)
                                        report.delivered -> Pair("✓ Sent".tr(), Color(0xFF4CAF50))
                                        else -> Pair("✗ Failed".tr(), Color(0xFFFF3333))
                                    }
                                    if (badgeText.isNotEmpty()) {
                                        Surface(
                                            color = badgeColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // ── Call button ────────────────────────────
                                    IconButton(
                                        onClick = {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                val intent = Intent(Intent.ACTION_CALL).apply {
                                                    data = Uri.parse("tel:${contact.phone}")
                                                }
                                                context.startActivity(intent)
                                            } else {
                                                contactToCall = contact.phone
                                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(accentColor, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Status Text ───────────────────────────────────────────────────
            Text(
                text = when {
                    uiState.isConnectionTimeout ->
                        "⚠️ Connection taking longer than expected.\nTap 'I'M SAFE' to cancel or 'RETRY' below.".tr()
                    uiState.isTriggering        -> "Activating SOS alert…".tr()
                    uiState.isCancelled         -> "✅ Alert Cancelled — You're Safe".tr()
                    uiState.isSent              -> "🚨 SOS DISPATCHED SUCCESSFULLY!!".tr()
                    uiState.isSending || pendingHomeNavigation || (ticks == 0 && countdownStarted) ->
                        "Dispatching SOS to emergency contacts…".tr()
                    countdownStarted            -> "SOS will send in $ticks seconds — tap 'I'M SAFE' to cancel"
                    else                        -> "Activating SOS alert…".tr()
                },
                color = when {
                    uiState.isConnectionTimeout -> Color(0xFFFF6B00)
                    uiState.isCancelled         -> Color(0xFF4CAF50)
                    uiState.isSent              -> Color(0xFFFF3333)
                    uiState.isSending || pendingHomeNavigation || (ticks == 0 && countdownStarted) ->
                        Color(0xFFFFAA00)
                    else                        -> Color.White.copy(0.75f)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── I'M SAFE Button ───────────────────────────────────────────────
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sosViewModel.cancelSos()
                },
                enabled = uiState.isConnectionTimeout ||
                    (!uiState.isCancelled && !uiState.isSending && !uiState.isCancelling),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    disabledContainerColor = Color(0xFF111111)
                ),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!uiState.isCancelled) Color.White.copy(0.15f) else Color(0xFF4CAF50).copy(0.4f)
                )
            ) {
                if (uiState.isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("I'M SAFE".tr(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── SEND SOS NOW Button ───────────────────────────────────────────
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sosViewModel.sendNow()
                },
                enabled = !uiState.isSent && !uiState.isSending && !uiState.isCancelled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(0.3f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("SEND SOS NOW".tr(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Back to Home ──────────────────────────────────────────────────
            OutlinedButton(
                onClick = { handleBackToHome() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("BACK TO HOME".tr(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Error + Retry
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF4444).copy(0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        msg,
                        color = Color(0xFFFF8888),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                if (uiState.alertId == null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { sosViewModel.triggerSos(triggerType) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Connection".tr(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Footer: GPS indicator ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val gpsAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "gps_pulse".tr()
                )
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50).copy(alpha = gpsAlpha),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(text = "Live location sharing active".tr(),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
