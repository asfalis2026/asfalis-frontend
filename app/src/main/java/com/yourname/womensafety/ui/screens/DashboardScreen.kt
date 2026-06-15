package com.yourname.womensafety.ui.screens

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.Build
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.yourname.womensafety.ui.tour.TourTargetRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.R
import com.yourname.womensafety.ui.viewmodels.AutoSosViewModel
import com.yourname.womensafety.ui.viewmodels.DashboardViewModel
import com.yourname.womensafety.ui.viewmodels.IotViewModel
import com.yourname.womensafety.utils.ModelStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory
    )
    val autoSosViewModel: AutoSosViewModel = viewModel(
        factory = AutoSosViewModel.factory(context.applicationContext as Application)
    )
    val iotViewModel: IotViewModel = viewModel(factory = IotViewModel.Factory)

    val isProtectionOn by dashboardViewModel.isProtectionActive.collectAsStateWithLifecycle()
    val userName by dashboardViewModel.userName.collectAsStateWithLifecycle()
    val autoSosMonitoring by dashboardViewModel.autoSosMonitoring.collectAsStateWithLifecycle()
    val shakeSensitivity by dashboardViewModel.shakeSensitivity.collectAsStateWithLifecycle()
    val sensorActive by autoSosViewModel.isActive.collectAsStateWithLifecycle()

    // ML pipeline observables — drive real-time Dashboard status UI
    val bufferProgress by autoSosViewModel.bufferProgress.collectAsStateWithLifecycle()
    val modelStatus by autoSosViewModel.modelStatus.collectAsStateWithLifecycle()
    val currentWindowIndex by autoSosViewModel.currentWindowIndex.collectAsStateWithLifecycle()

    // IoT wearable state
    val iotConnectionState by iotViewModel.connectionState.collectAsStateWithLifecycle()
    val iotFoundDevice by iotViewModel.foundDevice.collectAsStateWithLifecycle()
    val iotError by iotViewModel.errorMessage.collectAsStateWithLifecycle()
    val deviceDistance by iotViewModel.deviceDistance.collectAsStateWithLifecycle()
    val isBraceletConnected = iotConnectionState == IotViewModel.ConnectionState.CONNECTED
    val isIotConnecting     = iotConnectionState == IotViewModel.ConnectionState.CONNECTING

    val snackbarHostState = remember { SnackbarHostState() }

    // Bluetooth runtime permission launcher (Android 12+)
    val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
    var showSearchSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            iotViewModel.scanForDevice()
            showSearchSheet = true
        } else {
            Toast.makeText(
                context, "Bluetooth permission required to connect the wearable".trNonComposable(),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate("sos_alert?triggerType=manual")
        } else {
            Toast.makeText(context, "Location permission is required to send SOS alerts.".trNonComposable(), Toast.LENGTH_LONG).show()
        }
    }

    // Start/stop Auto SOS sensor monitoring whenever the combined flag changes
    LaunchedEffect(autoSosMonitoring, shakeSensitivity) {
        autoSosViewModel.setActive(autoSosMonitoring, shakeSensitivity)
    }

    // Navigate to SOS countdown screen when danger is detected by the ML model
    LaunchedEffect(Unit) {
        autoSosViewModel.dangerDetected.collect { event ->
            // Tell the service the UI is now handling this countdown —
            // this cancels the service-side countdown so it won't double-dispatch.
            val uiTookOverIntent = android.content.Intent(context, com.yourname.womensafety.service.SafetyForegroundService::class.java).apply {
                action = com.yourname.womensafety.service.SafetyForegroundService.ACTION_UI_TOOK_OVER
                putExtra(com.yourname.womensafety.service.SafetyForegroundService.EXTRA_ALERT_ID, event.alertId)
            }
            context.startService(uiTookOverIntent)

            // Show a brief snackbar so the user sees the transition
            snackbarHostState.showSnackbar(
                message = "⚠️ Danger detected! Starting SOS countdown...".trNonComposable(),
                duration = SnackbarDuration.Short
            )
            navController.navigate(
                "sos_alert?triggerType=${event.triggerType}&alertId=${event.alertId}"
            )
        }
    }

    // Show a Toast whenever the 10-minute cooldown starts after an Auto SOS trigger
    LaunchedEffect(Unit) {
        autoSosViewModel.cooldownStarted.collect {
            Toast.makeText(
                context, "🔒 Auto SOS triggered. Monitoring paused for 10 minutes.".trNonComposable(),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Load protection status and settings on startup
    LaunchedEffect(Unit) {
        dashboardViewModel.loadProtectionStatus()
        dashboardViewModel.loadGreeting()
    }

    // Reload settings when resuming the screen (e.g., coming back from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.refreshSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val autoSosEnabled by dashboardViewModel.autoSosEnabled.collectAsStateWithLifecycle()

    val errorMessage by dashboardViewModel.errorMessage.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Show Dashboard errors (toggle failure, profile load failure) via Snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            dashboardViewModel.clearError()
        }
    }

    var isScanningForDevice by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "dashboard_anims".tr())

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing)), label = "rotation".tr()
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse".tr()
    )
    val shieldPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "shield_pulse".tr()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(com.yourname.womensafety.ui.theme.BgDark, com.yourname.womensafety.ui.theme.BgMid, com.yourname.womensafety.ui.theme.BgEnd)))
    ) {
        // Subtle background glow — radial red bloom from top-centre
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE25F71).copy(alpha = 0.10f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        
        // Background illustration
        Image(
            painter = painterResource(id = R.drawable.dashboard_illustration),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().offset(y = 100.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            alpha = 0.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // --- Header: Premium Greeting ---
            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val greeting = when {
                hour < 12 -> "Good Morning".tr()
                hour < 17 -> "Good Afternoon".tr()
                else      -> "Good Evening".tr()
            }
            val greetEmoji = when { hour < 12 -> "☀️".tr(); hour < 17 -> "🌤️".tr(); else -> "🌙".tr() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 36.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFE25F71), Color(0xFFFF5555))))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("$greetEmoji $greeting".tr(),
                            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (userName != null) userName!! else " ",
                            color = Color.White, fontSize = 26.sp,
                            fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp
                        )
                    }
                }
                // Sleek Asfalis Logo (Enlarged, No Circle)
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "Asfalis".tr(),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.weight(0.4f))

            Spacer(Modifier.height(40.dp))

            // --- Main Shield (Premium Aesthetic) ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                // Pulsing rings when armed
                if (isProtectionOn) {
                    Box(modifier = Modifier.requiredSize(0.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.requiredSize(280.dp * shieldPulseScale).clip(CircleShape).background(Color(0xFFE25F71).copy(0.05f)))
                        Box(modifier = Modifier.requiredSize(240.dp * shieldPulseScale).clip(CircleShape).background(Color(0xFFE25F71).copy(0.1f)))
                        Box(modifier = Modifier.requiredSize(200.dp * shieldPulseScale).clip(CircleShape).background(Color(0xFFE25F71).copy(0.15f)))
                    }
                }

                // Main Button
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .onGloballyPositioned { coords ->
                            TourTargetRegistry.register("tour_protection_btn", coords.boundsInWindow())
                        }
                        .background(
                            if (isProtectionOn) Brush.radialGradient(listOf(Color(0xFFE25F71).copy(0.4f), Color(0xFFE25F71).copy(0.1f)))
                            else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .border(
                            width = 1.dp,
                            color = if (isProtectionOn) Color(0xFFFF3333).copy(0.6f) else Color(0xFFE25F71).copy(0.4f),
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isProtectionOn && !autoSosEnabled) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Auto SOS is off — enable it to arm protection".trNonComposable(),
                                        actionLabel = "Settings",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        navController.navigate("settings")
                                    }
                                }
                            } else {
                                dashboardViewModel.toggleProtection(!isProtectionOn)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (isProtectionOn) Color.White else Color(0xFFE25F71),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (isProtectionOn) "You're Protected".tr() else "Protection Off".tr(),
                            color = if (isProtectionOn) Color.White else Color(0xFFE25F71),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        if (isProtectionOn) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "All systems normal".tr().tr(),
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tap to enable".tr(),
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Rotating dashes
                if (isProtectionOn) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        drawArc(
                            color = Color(0xFFE25F71).copy(0.8f),
                            startAngle = rotation,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFFE25F71).copy(0.5f),
                            startAngle = rotation + 180f,
                            sweepAngle = 40f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // --- Sensor Status Box (real-time ML pipeline status) ---
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isProtectionOn) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Model status chip
                        val (chipColor, chipText) = when (modelStatus) {
                            ModelStatus.READY   -> Pair(Color(0xFF4CAF50), "🟢 " + "Model Ready".tr())
                            ModelStatus.ERROR   -> Pair(Color(0xFFE25F71), "🔴 " + "Model Error — restart app".tr())
                            ModelStatus.LOADING -> Pair(Color(0xFFFFC107), "⏳ " + "Loading model…".tr())
                        }

                        // Buffer fill chip (shown while collecting window data)
                        val bufferPct = (bufferProgress * 100).toInt()
                        val monitorText = when {
                            modelStatus == ModelStatus.ERROR  -> "ML model failed to load".tr()
                            bufferProgress < 1f               -> "Buffering motion… $bufferPct%"
                            currentWindowIndex > 0            -> "Window $currentWindowIndex/10 — analysing"
                            else                              -> "Active threat monitoring".tr()
                        }
                        val monitorColor = when {
                            modelStatus == ModelStatus.ERROR -> Color(0xFFE25F71)
                            bufferProgress < 1f             -> Color(0xFFFFC107)
                            else                            -> Color(0xFF4CAF50)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            // Main status pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(monitorColor.copy(0.15f))
                                    .border(1.dp, monitorColor.copy(0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dotAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                                        label = "dotAlpha".tr()
                                    )
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(monitorColor.copy(alpha = dotAlpha)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(monitorText, color = monitorColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Buffer fill progress bar (only visible while window is filling)
                            if (bufferProgress > 0f && bufferProgress < 1f && modelStatus == ModelStatus.READY) {
                                LinearProgressIndicator(
                                    progress = { bufferProgress },
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Color(0xFFFFC107),
                                    trackColor = Color.White.copy(0.1f)
                                )
                            }

                            // Model health chip (small, secondary)
                            if (modelStatus != ModelStatus.READY) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipColor.copy(0.1f))
                                        .border(1.dp, chipColor.copy(0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(chipText, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.4f))

            // --- Connected Device Glass Card ---
            Text("DEVICE SYNC".tr(), color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .onGloballyPositioned { coords ->
                        TourTargetRegistry.register("tour_ble_card", coords.boundsInWindow())
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isScanningForDevice = true
                        btPermissionLauncher.launch(btPermissions)
                    },
                color = Color.Black.copy(0.3f), // Premium dark glass
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBraceletConnected) Color(0xFFE25F71).copy(0.4f) else Color.White.copy(0.05f))
            ) {
                // Glass highlight
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(0.05f), Color.Transparent))))

                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Icon Box
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(if (isBraceletConnected) Color(0xFFE25F71).copy(0.15f) else Color.White.copy(0.04f)), contentAlignment = Alignment.Center) {
                        if (iotConnectionState == IotViewModel.ConnectionState.CONNECTING) {
                            CircularProgressIndicator(color = Color(0xFFE25F71), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Watch, null, tint = if (isBraceletConnected) Color(0xFFFF5555) else Color.White.copy(0.6f), modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (iotConnectionState) {
                                IotViewModel.ConnectionState.CONNECTED   -> "Bracelet Active".tr()
                                IotViewModel.ConnectionState.CONNECTING  -> "Connecting…".tr()
                                IotViewModel.ConnectionState.ERROR       -> "Connection Failed".tr()
                                else                                     -> "Not Connected".tr()
                            },
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (iotConnectionState) {
                                IotViewModel.ConnectionState.CONNECTED  -> "Monitoring life vitals".tr()
                                IotViewModel.ConnectionState.CONNECTING -> "Please wait…".tr()
                                IotViewModel.ConnectionState.ERROR      -> "Tap to retry connection".tr()
                                else                                    -> "Tap to sync hardware".tr()
                            },
                            color = Color.White.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                        // Proximity chip — only shown while connected and BLE readings arrive.
                        if (isBraceletConnected) {
                            val dist = deviceDistance
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = when {
                                    dist == null -> "\uD83D\uDCE1 Calibrating distance…".tr()
                                    dist < 1f    -> "\uD83D\uDCCD Right next to you".tr()
                                    dist < 3f    -> "\uD83D\uDCCD %.1f m away".tr().format(dist)
                                    dist < 5f    -> "\u26A0\uFE0F %.1f m — getting far".tr().format(dist)
                                    else         -> "\uD83D\uDEA8 %.0f m — SOS will trigger!".tr().format(dist)
                                },
                                color = when {
                                    dist == null || dist < 3f -> Color(0xFF4CAF50)
                                    dist < 5f                 -> Color(0xFFFFAA00)
                                    else                      -> Color(0xFFFF4444)
                                },
                                fontSize = 11.sp,
                                fontWeight = if (dist != null && dist >= 5f) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (iotConnectionState == IotViewModel.ConnectionState.CONNECTING) {
                        TextButton(onClick = { iotViewModel.disconnect() }) {
                            Text("Abort".tr(), color = Color(0xFFFFAA00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isBraceletConnected) {
                        TextButton(onClick = { iotViewModel.disconnect() }) {
                            Text("Unpair".tr(), color = Color(0xFFFF5555), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bluetooth, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Emergency Action Button ---
            Text("EMERGENCY".tr(), color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .onGloballyPositioned { coords ->
                        TourTargetRegistry.register("tour_sos_btn", coords.boundsInWindow())
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            navController.navigate("sos_alert?triggerType=manual")
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF3B61).copy(0.45f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE25F71), Color(0xFFBB0030), Color(0xFF8B0000))
                        )
                    )
                ) {
                    // Pulsing glow overlay
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(0.12f * pulseAlpha), Color.Transparent),
                            radius = 700f
                        )
                    ))

                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Call, "SOS", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text("Trigger SOS".tr(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text("Tap to send emergency alert".tr(),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showSearchSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSearchSheet = false },
                sheetState = sheetState,
                containerColor = com.yourname.womensafety.ui.theme.CardBg,
                scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                SearchDeviceContent(
                    isScanning      = isScanningForDevice,
                    onScanFinished  = { isScanningForDevice = false },
                    connectionState = iotConnectionState,
                    foundDevice     = iotFoundDevice,
                    errorMessage    = iotError,
                    onConnect       = { device ->
                        iotViewModel.connect(device)
                        // Do NOT dismiss here — keep the sheet open so the user can see
                        // the CONNECTING spinner and any subsequent error message.
                    },
                    onDisconnect    = {
                        iotViewModel.disconnect()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showSearchSheet = false
                        }
                    }
                )

                // Auto-dismiss the sheet once the socket is confirmed open.
                LaunchedEffect(iotConnectionState) {
                    if (iotConnectionState == IotViewModel.ConnectionState.CONNECTED && showSearchSheet) {
                        sheetState.hide()
                        showSearchSheet = false
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = com.yourname.womensafety.ui.theme.CardBg,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun DashStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(0.04f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DashActionCard(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = accent.copy(0.08f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SearchDeviceContent(
    isScanning: Boolean,
    onScanFinished: () -> Unit,
    connectionState: IotViewModel.ConnectionState,
    foundDevice: BluetoothDevice?,
    errorMessage: String?,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected   = connectionState == IotViewModel.ConnectionState.CONNECTED
    val isConnecting  = connectionState == IotViewModel.ConnectionState.CONNECTING
    val context = LocalContext.current

    LaunchedEffect(isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(2000L) // Simulate scanning time for UX
            onScanFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isConnected) "Bracelet Details".tr() else "Connect ESP32 Bracelet".tr(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                isConnected  -> "Your Asfalis wearable is active and protecting you".tr()
                isScanning   -> "Searching for your bracelet via Bluetooth…".tr()
                isConnecting -> "Establishing secure Bluetooth connection…".tr()
                else         -> "Pair \".tr()ESP32_SOS_DEVICE\" via Android Bluetooth Settings first".tr()
            },
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(28.dp))

        when {
            isConnecting || isScanning -> {
                val infiniteTransition = rememberInfiniteTransition(label = "iot_connect".tr())
                val outerScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "outer".tr()
                )
                val innerScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "inner".tr()
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    Box(modifier = Modifier.size(100.dp * outerScale).clip(CircleShape).background(Color(0xFFE25F71).copy(0.06f)))
                    Box(modifier = Modifier.size(72.dp * innerScale).clip(CircleShape).background(Color(0xFFE25F71).copy(0.12f)))
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, null, tint = Color(0xFFE25F71), modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(if (isScanning) "Scanning for nearby devices…".tr() else "Connecting to bracelet…".tr(), color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFFE25F71),
                    trackColor = Color.White.copy(0.08f)
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF4444).copy(0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⚠️ ".tr() + errorMessage, color = Color(0xFFFF8888), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            isConnected -> {
                var showDisconnectDialog by remember { mutableStateOf(false) }
                val infTrans = rememberInfiniteTransition(label = "connected_pulse".tr())
                val dotAlpha by infTrans.animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot".tr()
                )

                if (showDisconnectDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisconnectDialog = false },
                        containerColor = Color(0xFF160E0E),
                        shape = RoundedCornerShape(20.dp),
                        title = { Text("Disconnect Bracelet?".tr(), color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Disconnecting will disable the hardware SOS button and proximity alert. ".tr() +
                                "The app will still work for manual and auto SOS.".tr(),
                                color = Color(0xFFAAAAAA), fontSize = 14.sp, lineHeight = 20.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { showDisconnectDialog = false; onDisconnect() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Disconnect".tr(), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDisconnectDialog = false }) {
                                Text("Keep Connected".tr(), color = Color(0xFF4CAF50))
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE25F71).copy(0.06f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.25f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Watch, null, tint = Color(0xFFE25F71), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ESP32 SOS Bracelet".tr(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF00FF00).copy(dotAlpha), CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Connected & Active".tr(), color = Color(0xFF4CAF50), fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.Bluetooth, null, tint = Color(0xFFE25F71), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(0.07f))
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SOS Button ✓".tr(), "Haptic ✓".tr(), "Proximity ✓".tr()).forEach { chip ->
                                Surface(
                                    color = Color.White.copy(0.04f), shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
                                ) {
                                    Text(chip, color = Color.White.copy(0.6f), fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showDisconnectDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A0000)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.3f))
                        ) {
                            Icon(Icons.Default.BluetoothDisabled, null, tint = Color(0xFFE25F71), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Disconnect Bracelet".tr(), color = Color(0xFFE25F71), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            foundDevice != null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Watch, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            @Suppress("MissingPermission")
                            Text(foundDevice.name ?: "ESP32_SOS_DEVICE", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Paired · tap Connect".tr(), color = Color.Gray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onConnect(foundDevice) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("Connect".tr(), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFF4444).copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                        Text("⚠️ ".tr() + errorMessage, color = Color(0xFFFF8888), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            else -> {
                Icon(Icons.Outlined.Watch, null, tint = Color(0xFF444444), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "No wearable found in paired devices.\nPair \"ESP32_SOS_DEVICE\" in Bluetooth Settings first.".tr(),
                    color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE25F71))
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Bluetooth Settings".tr(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}
