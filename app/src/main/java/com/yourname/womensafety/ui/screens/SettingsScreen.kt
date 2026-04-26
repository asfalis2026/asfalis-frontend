package com.yourname.womensafety.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.data.network.dto.UpdateSettingsRequest
import com.yourname.womensafety.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val isLoading by settingsViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by settingsViewModel.errorMessage.collectAsStateWithLifecycle()
    val loadedSettings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var sensitivity by remember { mutableStateOf("Medium") }
    var sosMessage by remember {
        mutableStateOf("Emergency! I need help. This is an automated SOS alert from ASFALIS. My live location is attached.")
    }
    var autoSosEnabled by remember { mutableStateOf(false) }
    var settingsSeeded by remember { mutableStateOf(false) }

    LaunchedEffect(loadedSettings) {
        if (loadedSettings != null && !settingsSeeded) {
            settingsSeeded = true
            loadedSettings?.let { s ->
                sensitivity = when (s.shakeSensitivity) { "low" -> "Low"; "high" -> "High"; else -> "Medium" }
                sosMessage = s.sosMessage
                autoSosEnabled = s.autoSosEnabled
            }
        }
    }
    LaunchedEffect(Unit) { settingsViewModel.loadSettings() }
    LaunchedEffect(Unit) { settingsViewModel.saveSuccess.collect { navController.popBackStack() } }

    val bg = Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1A0000), Color(0xFF0D0000)))

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("App Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Customize your protection", color = Color.Gray, fontSize = 13.sp)
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0xFFE10600).copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE10600), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = Color(0xFFE10600), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── SOS MESSAGE SECTION ──────────────────────────────────────────
            SettingsSectionCard(
                icon = Icons.AutoMirrored.Filled.Message,
                title = "SOS Message",
                subtitle = "Sent to contacts when SOS is triggered"
            ) {
                OutlinedTextField(
                    value = sosMessage,
                    onValueChange = { if (it.length <= 500) sosMessage = it },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedBorderColor = Color(0xFFE10600),
                        unfocusedBorderColor = Color.White.copy(0.1f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    supportingText = {
                        Text("${sosMessage.length}/500", color = Color.Gray, fontSize = 11.sp)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── SHAKE SENSITIVITY SECTION ────────────────────────────────────
            SettingsSectionCard(
                icon = Icons.Default.GraphicEq,
                title = "Shake Sensitivity",
                subtitle = "How hard you need to shake to trigger Auto SOS"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Low", "Requires vigorous shake", Color(0xFF4CAF50)),
                        Triple("Medium", "Balanced detection", Color(0xFFFFB74D)),
                        Triple("High", "Triggers easily", Color(0xFFE10600))
                    ).forEach { (level, desc, color) ->
                        val selected = sensitivity == level
                        val bgAnim by animateColorAsState(
                            if (selected) color.copy(0.2f) else Color.White.copy(0.04f), label = "bg_$level"
                        )
                        val borderAnim by animateColorAsState(
                            if (selected) color else Color.White.copy(0.08f), label = "border_$level"
                        )
                        val inf = rememberInfiniteTransition(label = "pulse_$level")
                        val scl by inf.animateFloat(
                            1f, if (selected) 1.04f else 1f,
                            infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s_$level"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .scale(scl)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgAnim)
                                .border(1.dp, borderAnim, RoundedCornerShape(14.dp))
                                .clickable { sensitivity = level }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val barHeights = when (level) {
                                "Low" -> listOf(6, 10, 7)
                                "Medium" -> listOf(8, 14, 10)
                                else -> listOf(12, 18, 14)
                            }
                            // Mini wave bars
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                                barHeights.forEach { h ->
                                    Box(modifier = Modifier.width(5.dp).height(h.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (selected) color else Color.Gray.copy(0.4f)))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(level, color = if (selected) color else Color.Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.04f)).padding(10.dp)
                ) {
                    Text(
                        text = when (sensitivity) {
                            "Low" -> "🟢 Requires a very vigorous shake. Best for active environments."
                            "High" -> "🔴 Maximum sensitivity. Triggers easily — use with care."
                            else -> "🟡 Balanced. Triggers on a deliberate, strong shake."
                        },
                        color = Color.White.copy(0.7f), fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── AUTO SOS SECTION ─────────────────────────────────────────────
            val autoSosBorder by animateColorAsState(
                if (autoSosEnabled) Color(0xFFE10600).copy(0.5f) else Color.White.copy(0.08f), label = "auto_border"
            )
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (autoSosEnabled) Color(0xFFE10600).copy(0.06f) else Color.White.copy(0.03f))
                    .border(1.dp, autoSosBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(if (autoSosEnabled) Color(0xFFE10600).copy(0.15f) else Color.White.copy(0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sensors, null,
                            tint = if (autoSosEnabled) Color(0xFFE10600) else Color.Gray,
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (autoSosEnabled) "Auto SOS Active" else "Auto SOS Off",
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                        )
                        Text(
                            if (autoSosEnabled) "ML model monitors shake & fall patterns"
                            else "Enable to auto-detect danger via sensors",
                            color = Color.Gray, fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoSosEnabled,
                        onCheckedChange = { autoSosEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE10600),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(0.1f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Privacy note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Motion data is analyzed entirely on-device. Never uploaded.", color = Color.Gray, fontSize = 12.sp)
            }
        }

        // Floating Save Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    settingsViewModel.saveSettings(
                        UpdateSettingsRequest(
                            sosMessage = sosMessage.trim().take(500),
                            shakeSensitivity = sensitivity.lowercase(),
                            autoSosEnabled = autoSosEnabled
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                shape = RoundedCornerShape(18.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Save Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.03f))
            .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Color(0xFFE10600).copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(0.06f))
        Spacer(Modifier.height(16.dp))
        content()
    }
}

// Keep the old SettingsHeader for backward compat with any callers
@Composable
fun SettingsHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
