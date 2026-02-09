package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    var isProtectionOn by remember { mutableStateOf(false) }
    var isBraceletConnected by remember { mutableStateOf(false) }

    // --- NEW: State for Search Window ---
    var showSearchSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "dashboard_anims")

    // --- YOUR ORIGINAL ANIMATIONS (INTACT) ---
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

            // --- Header (Intact) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RAKSHA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Dashboard", color = Color.Gray, fontSize = 16.sp)
                }
                Surface(
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("settings")
                    },
                    shape = CircleShape, color = Color.White.copy(alpha = 0.08f)
                ) {
                    Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(Modifier.weight(0.5f))

            // --- Main Shield (Intact) ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (isProtectionOn) {
                    Box(modifier = Modifier.size(240.dp * shieldPulseScale).clip(CircleShape).background(Color(0xFFE10600).copy(0.1f)))
                }
                Box(
                    modifier = Modifier.size(240.dp).drawBehind {
                        if (isProtectionOn) {
                            drawArc(color = Color(0xFFE10600), startAngle = rotation, sweepAngle = 100f, useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }.padding(12.dp).clip(CircleShape).background(if (isProtectionOn) Color(0xFFE10600).copy(0.15f) else Color.White.copy(0.05f))
                        .border(width = 1.dp, color = if (isProtectionOn) Color(0xFFE10600).copy(0.3f) else Color.White.copy(0.1f), shape = CircleShape)
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); isProtectionOn = !isProtectionOn },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Shield, null, tint = if (isProtectionOn) Color.White else Color.Gray, modifier = Modifier.size(68.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (isProtectionOn) "Protection is ON" else "Protection is OFF", color = if (isProtectionOn) Color.White else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            // --- Connected Device Section ---
            Text("Connected Device", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showSearchSheet = true
                },
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBraceletConnected) Color(0xFFE10600).copy(0.3f) else Color.White.copy(0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(if (isBraceletConnected) Color(0xFFE10600).copy(0.15f) else Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Watch, null, tint = if (isBraceletConnected) Color(0xFFE10600) else Color.Gray, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isBraceletConnected) "Bracelet Connected" else "Bracelet not connected", color = if (isBraceletConnected) Color.White else Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(if (isBraceletConnected) "Raksha Band v1.0" else "Tap to connect", color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.Bluetooth, null, tint = if (isBraceletConnected) Color(0xFFE10600) else Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(color = if (isBraceletConnected) Color(0xFF00FF00).copy(alpha = pulseAlpha) else Color.Gray, shape = CircleShape))
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Quick Actions ---
            Text("Quick Actions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(Icons.Outlined.Call, "SOS", true, Modifier.weight(1f).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navController.navigate("sos_alert") })
                ActionCard(Icons.Outlined.People, "Contacts", false, Modifier.weight(1f).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); navController.navigate("contacts") })
                ActionCard(Icons.Outlined.LocationOn, "Location", false, Modifier.weight(1f).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); navController.navigate("map") })
            }

            Spacer(Modifier.height(24.dp))

            // --- Status Message ---
            if (isProtectionOn) {
                Surface(modifier = Modifier.fillMaxWidth().height(56.dp), color = Color(0xFFE10600).copy(0.1f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE10600).copy(0.2f))) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF00), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text("System Active • Monitoring Movement", color = Color.White, fontSize = 14.sp)
                    }
                }
            } else { Spacer(Modifier.height(56.dp)) }
            Spacer(Modifier.height(20.dp))
        }

        // --- UPDATED: MODAL SEARCH WINDOW ---
        if (showSearchSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSearchSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF120000),
                scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                // Pass current state and logic to the content
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
    }
}

// --- UPDATED: CONTENT FOR SEARCH WINDOW ---
@Composable
fun SearchDeviceContent(
    isConnected: Boolean,
    onToggleConnection: (Boolean) -> Unit
) {
    // Only scan if we are not already connected
    var isScanning by remember { mutableStateOf(!isConnected) }

    LaunchedEffect(Unit) {
        if (!isConnected) {
            delay(3000) // Scanning time
            isScanning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isConnected) "Device Details" else "Searching for Devices",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isConnected) "Your bracelet is currently active" else "Make sure your Raksha Bracelet is nearby",
            color = Color.Gray, fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        if (isScanning) {
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.4f,
                animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "scale"
            )

            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(80.dp * scale).clip(CircleShape).background(Color(0xFFE10600).copy(0.1f)))
                Icon(Icons.Default.BluetoothSearching, null, tint = Color(0xFFE10600), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color(0xFFE10600), trackColor = Color.White.copy(0.1f))
        } else {
            // Found / Connected Device Row
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    // Toggle the actual state
                    onToggleConnection(!isConnected)
                },
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Watch, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Raksha Band v1.0", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isConnected) "Connected" else "Strong Signal",
                            color = if (isConnected) Color(0xFF00FF00) else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = if (isConnected) "Disconnect" else "Connect",
                        color = Color(0xFFE10600),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(100.dp), color = if (isActive) Color(0xFFE10600) else Color.White.copy(0.05f),
        shape = RoundedCornerShape(22.dp), border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)) else null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}