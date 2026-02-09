package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    var sensitivity by remember { mutableStateOf("Medium") }
    var batteryOptimization by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }

    val haptic = LocalHapticFeedback.current

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.popBackStack()
                },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        // Section: Emergency Contact
        SettingsHeader(Icons.Default.Phone, "Emergency Contact")
        SettingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+1 (911) 000-0000", color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color(0xFFE10600).copy(0.2f), shape = CircleShape) {
                            Text("Required", color = Color(0xFFE10600), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text("Primary emergency number", color = Color.Gray, fontSize = 13.sp)
                }
                TextButton(onClick = { if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }) {
                    Text("Edit", color = Color(0xFFE10600))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Section: SOS Message
        SettingsHeader(Icons.Default.Message, "SOS Message")
        SettingCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Live location attached", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"Emergency! I need help. This is an automated SOS alert from Women Safety app. My live location is attached.\"",
                    color = Color.White, fontSize = 14.sp
                )
                TextButton(
                    onClick = { if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Edit Message", color = Color(0xFFE10600), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Section: Shake Sensitivity (SIZING FIXED HERE) ---
        SettingsHeader(Icons.Default.GraphicEq, "Shake Sensitivity")
        SettingCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { level ->
                        val isSelected = sensitivity == level
                        Button(
                            onClick = {
                                sensitivity = level
                                if (hapticFeedback) {
                                    when (level) {
                                        "Low" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        "Medium" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        "High" -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp), // Increased height to match other controls
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFE10600) else Color.White.copy(0.05f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp) // Removes internal padding to keep text centered
                        ) {
                            Text(
                                text = level,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 14.sp, // Increased text size
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when(sensitivity) {
                        "Low" -> "Requires strong shake to trigger alert"
                        "High" -> "Very sensitive - triggers on light shake"
                        else -> "Balanced - triggers on moderate shake"
                    },
                    color = Color.Gray,
                    fontSize = 13.sp // Slightly larger font
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Section: App Performance
        SettingsHeader(Icons.Default.Bolt, "App Performance")
        ToggleRow("Battery Optimization", "Reduce power consumption", batteryOptimization) {
            if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            batteryOptimization = it
        }
        Spacer(Modifier.height(12.dp))
        ToggleRow("Haptic Feedback", "Vibrate on interactions", hapticFeedback) {
            if (!hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hapticFeedback = it
        }

        Spacer(Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}

// Helper Composables (SettingCard, SettingsHeader, ToggleRow) remain the same.
@Composable
fun SettingCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
fun SettingsHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun ToggleRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE10600))
        )
    }
}