package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.R
import com.yourname.womensafety.ui.viewmodels.AutoSosViewModel
import com.yourname.womensafety.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory
    )
    val autoSosViewModel: AutoSosViewModel = viewModel()

    val isProtectionOn by dashboardViewModel.isProtectionActive.collectAsStateWithLifecycle()
    val userName by dashboardViewModel.userName.collectAsStateWithLifecycle()
    val autoSosMonitoring by dashboardViewModel.autoSosMonitoring.collectAsStateWithLifecycle()
    val shakeSensitivity by dashboardViewModel.shakeSensitivity.collectAsStateWithLifecycle()
    val sensorActive by autoSosViewModel.isActive.collectAsStateWithLifecycle()
    var isBraceletConnected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Load protection status and settings on startup
    LaunchedEffect(Unit) {
        dashboardViewModel.loadProtectionStatus()
        dashboardViewModel.loadGreeting()
    }

    // Start/stop Auto SOS sensor monitoring whenever the combined flag changes
    LaunchedEffect(autoSosMonitoring, shakeSensitivity) {
        autoSosViewModel.setActive(autoSosMonitoring, shakeSensitivity)
    }

    // Navigate to SOS countdown screen when danger is detected by the ML model
    LaunchedEffect(Unit) {
        autoSosViewModel.dangerDetected.collect { event ->
            // Show a brief snackbar so the user sees the transition
            snackbarHostState.showSnackbar(
                message = "⚠️ Danger detected! Starting SOS countdown...",
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
                context,
                "🔒 Auto SOS triggered. Monitoring paused for 10 minutes.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    var showSearchSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val errorMessage by dashboardViewModel.errorMessage.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "dashboard_anims")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing)), label = "rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )
    val shieldPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "shield_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF1a0000), Color(0xFF2d0000))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // --- Header: Branding & PNG Logo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small Red Accent Box before the Name
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE10600))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "ASFALIS",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (userName != null) "Hello, $userName" else "Active Protection",
                            color = Color(0xFFE10600),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // PNG Logo
                    Image(
                        painter = painterResource(id = R.drawable.splash_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.weight(0.6f))

            // --- Main Shield ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (isProtectionOn) {
                    Box(modifier = Modifier.size(220.dp * shieldPulseScale).clip(CircleShape).background(Color(0xFFE10600).copy(0.1f)))
                }
                Box(
                    modifier = Modifier.size(220.dp).drawBehind {
                        if (isProtectionOn) {
                            drawArc(color = Color(0xFFE10600), startAngle = rotation, sweepAngle = 100f, useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }.padding(12.dp).clip(CircleShape).background(if (isProtectionOn) Color(0xFFE10600).copy(0.15f) else Color.White.copy(0.05f))
                        .border(width = 1.dp, color = if (isProtectionOn) Color(0xFFE10600).copy(0.3f) else Color.White.copy(0.1f), shape = CircleShape)
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dashboardViewModel.toggleProtection(!isProtectionOn) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Shield, null, tint = if (isProtectionOn) Color.White else Color.Gray, modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(if (isProtectionOn) "SYSTEM ARMED" else "SYSTEM DISARMED", color = if (isProtectionOn) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.weight(0.6f))

            // --- Connected Device ---
            Text("Connected Device", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSearchSheet = true
                    },
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBraceletConnected) Color(0xFFE10600).copy(0.3f) else Color.White.copy(0.1f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isBraceletConnected) Color(0xFFE10600).copy(0.15f) else Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Watch, null, tint = if (isBraceletConnected) Color(0xFFE10600) else Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isBraceletConnected) "Bracelet Connected" else "Not Connected", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (isBraceletConnected) "Asfalis Band v1.0" else "Tap to sync device", color = Color.Gray, fontSize = 13.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, null, tint = if (isBraceletConnected) Color(0xFFE10600) else Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(color = if (isBraceletConnected) Color(0xFF00FF00).copy(alpha = pulseAlpha) else Color.Gray, shape = CircleShape))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Emergency Action ---
            Text("Emergency Action", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("sos_alert?triggerType=manual")
                    },
                color = Color(0xFFE10600),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Call, "SOS", tint = Color.White, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("TRIGGER SOS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Text("Immediate alerts to trusted contacts", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            if (isProtectionOn) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    color = if (sensorActive) Color(0xFFE10600).copy(0.1f) else Color.White.copy(0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (sensorActive) Color(0xFFE10600).copy(0.2f) else Color.White.copy(0.08f)
                    )
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(
                            if (sensorActive) Color(0xFF00FF00).copy(alpha = pulseAlpha) else Color(0xFFFFAA00),
                            CircleShape
                        ))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (sensorActive) "Auto SOS Active • Shake to trigger" else "System Active • Starting sensors...",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (showSearchSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSearchSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF120000),
                scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                SearchDeviceContent(
                    isConnected = isBraceletConnected,
                    onToggleConnection = { newState ->
                        isBraceletConnected = newState
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showSearchSheet = false
                        }
                    }
                )
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
                containerColor = Color(0xFF2D0000),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun SearchDeviceContent(isConnected: Boolean, onToggleConnection: (Boolean) -> Unit) {
    var isScanning by remember { mutableStateOf(!isConnected) }
    LaunchedEffect(Unit) { if (!isConnected) { delay(3000); isScanning = false } }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isConnected) "Device Details" else "Searching for Devices", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = if (isConnected) "Your bracelet is currently active" else "Make sure your Asfalis Bracelet is nearby", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        if (isScanning) {
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.4f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "scale")
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(80.dp * scale).clip(CircleShape).background(Color(0xFFE10600).copy(0.1f)))
                Icon(Icons.Default.BluetoothSearching, null, tint = Color(0xFFE10600), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color(0xFFE10600), trackColor = Color.White.copy(0.1f))
        } else {
            Surface(modifier = Modifier.fillMaxWidth().clickable { onToggleConnection(!isConnected) }, color = Color.White.copy(0.05f), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Watch, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Asfalis Band v1.0", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(text = if (isConnected) "Connected" else "Strong Signal", color = if (isConnected) Color(0xFF00FF00) else Color.Gray, fontSize = 12.sp)
                    }
                    Text(text = if (isConnected) "Disconnect" else "Connect", color = Color(0xFFE10600), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}