package com.yourname.womensafety.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.data.IotAction
import com.yourname.womensafety.data.IotEventBus
import com.yourname.womensafety.data.SecurityPolicyManager
import com.yourname.womensafety.data.network.dto.SosHistoryItem
import com.yourname.womensafety.ui.components.SecureScreen
import com.yourname.womensafety.ui.viewmodels.SosHistoryUiState
import com.yourname.womensafety.ui.viewmodels.SosHistoryViewModel
import kotlinx.coroutines.delay

@Composable
fun SOSHistoryScreen(navController: NavController) {
    val securityPolicy by SecurityPolicyManager.state.collectAsState()
    SecureScreen(
        enabled = securityPolicy.screenshotProtectionEnabled &&
            "sos_history" in securityPolicy.protectedScreens
    )

    val historyViewModel: SosHistoryViewModel = viewModel(factory = SosHistoryViewModel.Factory)
    val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Sent", "Cancelled", "Auto")
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        IotEventBus.events.collect { action ->
            if (action is IotAction.Triggered || action is IotAction.Cancelled) {
                delay(2_000L)
                historyViewModel.loadHistory()
            }
        }
    }

    val bg = Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1A0000), Color(0xFF0D0000)))

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF1A0000),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(Color(0xFFE10600).copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFE10600), modifier = Modifier.size(24.dp)) }
            },
            title = { Text("Clear All History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will remove all locally visible alerts. You can reload from server anytime.",
                    color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.White.copy(0.6f))
                }
            },
            confirmButton = {
                Button(
                    onClick = { historyViewModel.clearAll(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Clear All", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // ── Header ───────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alert History", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your SOS timeline", color = Color.Gray, fontSize = 13.sp)
                }
                // Clear all button — only shown when there are items
                if (uiState is SosHistoryUiState.Success &&
                    (uiState as SosHistoryUiState.Success).items.isNotEmpty()
                ) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color(0xFFE10600).copy(0.12f))
                            .border(1.dp, Color(0xFFE10600).copy(0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val state = uiState) {
                is SosHistoryUiState.Success -> {
                    val total = state.items.size
                    val sent = state.items.count { it.status == "sent" || it.status == "dispatched" }
                    val cancelled = state.items.count { it.status == "cancelled" }

                    // ── Stats Banner ──────────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF3A0000), Color(0xFF1A0000))))
                            .border(1.dp, Color(0xFFE10600).copy(0.2f), RoundedCornerShape(20.dp))
                            .padding(vertical = 18.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HistoryStatItem("Total", "$total", Icons.Outlined.History, Color(0xFFE10600))
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(0.1f)))
                            HistoryStatItem("Sent", "$sent", Icons.AutoMirrored.Filled.Send, Color(0xFF4CAF50))
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(0.1f)))
                            HistoryStatItem("Cancelled", "$cancelled", Icons.Default.Cancel, Color(0xFFFFAA00))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Filter Chips ──────────────────────────────────────────
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { f ->
                            val selected = selectedFilter == f
                            Surface(
                                onClick = { selectedFilter = f },
                                color = if (selected) Color(0xFFE10600) else Color.White.copy(0.06f),
                                shape = RoundedCornerShape(20.dp),
                                border = if (!selected) androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(0.1f)
                                ) else null
                            ) {
                                Text(
                                    f, color = if (selected) Color.White else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val filtered = when (selectedFilter) {
                        "Sent"      -> state.items.filter { it.status == "sent" || it.status == "dispatched" }
                        "Cancelled" -> state.items.filter { it.status == "cancelled" }
                        "Auto"      -> state.items.filter { it.triggerType.startsWith("auto") }
                        else        -> state.items
                    }

                    // Swipe-to-delete hint
                    if (filtered.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SwipeLeft, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Swipe left on a card to delete it", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (filtered.isEmpty()) {
                        EmptyHistoryState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(filtered) { item ->
                                SwipeToDeleteHistoryCard(
                                    item = item,
                                    onDelete = { historyViewModel.deleteItem(item) }
                                )
                            }
                        }
                    }
                }

                is SosHistoryUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFFE10600),
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Loading your alerts…", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                is SosHistoryUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, null, tint = Color(0xFFE10600).copy(0.6f),
                                modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center,
                                lineHeight = 20.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { historyViewModel.loadHistory() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Swipe-to-Delete wrapper ───────────────────────────────────────────────────

@Composable
private fun SwipeToDeleteHistoryCard(
    item: SosHistoryItem,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "swipe"
    )
    val deleteThreshold = -200f
    val revealing = animatedOffset < -60f

    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Delete background
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE10600).copy(if (revealing) 0.25f else 0f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (revealing) {
                    Row(
                        modifier = Modifier.padding(end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFE10600), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", color = Color(0xFFE10600), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Card itself
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset.coerceAtLeast(deleteThreshold).dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX < deleteThreshold) {
                                    visible = false
                                    onDelete()
                                } else {
                                    offsetX = 0f
                                }
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceAtMost(0f)
                            }
                        )
                    }
            ) {
                PremiumHistoryCard(item = item)
            }
        }
    }
}

// ── History stat cell ─────────────────────────────────────────────────────────

@Composable
private fun HistoryStatItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState() {
    val inf = rememberInfiniteTransition(label = "empty")
    val scale by inf.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "s"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Shield, null,
                tint = Color(0xFFE10600).copy(0.45f),
                modifier = Modifier.size(80.dp).scale(scale)
            )
            Spacer(Modifier.height(18.dp))
            Text("No alerts here", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "No SOS events match the selected filter.\nYou're safe! 💚",
                color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}

// ── Premium card ─────────────────────────────────────────────────────────────

@Composable
fun PremiumHistoryCard(item: SosHistoryItem) {
    val triggerInfo = when (item.triggerType) {
        "iot_button", "bracelet" -> Triple(Icons.Default.Watch,    Color(0xFF4FC3F7), "Hardware Button")
        "auto_fall"              -> Triple(Icons.Default.Sensors,   Color(0xFFFFB74D), "Auto Fall Detected")
        "auto_shake"             -> Triple(Icons.Default.Vibration, Color(0xFFFFB74D), "Auto Shake Detected")
        "manual"                 -> Triple(Icons.Default.TouchApp,  Color(0xFFE10600), "Manual SOS")
        else                     -> Triple(Icons.Default.Warning,   Color(0xFFE10600),
            item.triggerType.replaceFirstChar { it.uppercase() })
    }
    val cardIcon = triggerInfo.first
    val accent   = triggerInfo.second
    val label    = triggerInfo.third

    val statusColor = when (item.status) {
        "cancelled", "expired"    -> Color(0xFF888888)
        "sent", "dispatched"      -> Color(0xFF4CAF50)
        else                      -> Color(0xFFFFAA00)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(90.dp)
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(0.3f)))
                )
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(cardIcon, null, tint = accent, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(14.dp))

            // Text block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.triggeredAt,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                item.address?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = Color.Gray.copy(0.7f), fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            Spacer(Modifier.width(10.dp))

            // Status badge
            Surface(
                color = statusColor.copy(0.14f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    item.status.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
