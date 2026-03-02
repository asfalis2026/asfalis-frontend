package com.yourname.womensafety.ui.screens

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.yourname.womensafety.ui.viewmodels.SosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("MissingPermission")
@Composable
fun SOSAlertScreen(onSafe: () -> Unit) {
    val context = LocalContext.current
    val sosViewModel: SosViewModel = viewModel(factory = SosViewModel.Factory)
    val uiState by sosViewModel.uiState.collectAsState()
    var ticks by remember { mutableIntStateOf(10) }

    // Trigger SOS with real GPS coordinates
    LaunchedEffect(Unit) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = try {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (e: SecurityException) { null }
        sosViewModel.triggerSos(
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0
        )
    }

    // Auto-send when countdown reaches 0
    LaunchedEffect(Unit) {
        while (ticks > 0) {
            delay(1000L)
            ticks--
        }
        if (!uiState.isCancelled) {
            sosViewModel.sendNow()
        }
    }

    // Navigate back when cancelled or sent
    LaunchedEffect(uiState.isCancelled) {
        if (uiState.isCancelled) onSafe()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alert")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color(0xFF2D0000).copy(alpha = alpha))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- ALERT ICON ---
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Red.copy(0.08f), CircleShape)
                    .border(1.5.dp, Color.Red.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ALERT TRIGGERED",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Unusual Movement Detected",
                color = Color.Red.copy(0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "A sudden impact or fall was detected by\nyour device sensors.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(50.dp))

            // --- COUNTDOWN ---
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { ticks / 10f },
                    modifier = Modifier.size(180.dp),
                    color = Color.Red,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(0.05f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ticks.toString(),
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SECONDS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = if (uiState.isSent) "SOS Dispatched!" else "Sending Auto-SOS...",
                color = if (uiState.isSent) Color(0xFF00E676) else Color.White.copy(0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(50.dp))

            // --- I'M SAFE BUTTON ---
            Button(
                onClick = { sosViewModel.cancelSos() },
                enabled = !uiState.isSent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1111)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Text("I'M SAFE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- SEND NOW BUTTON ---
            Button(
                onClick = { sosViewModel.sendNow() },
                enabled = !uiState.isSent && !uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC60000)),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("SEND SOS NOW", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }

        // --- FOOTER ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Location sharing is active",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

