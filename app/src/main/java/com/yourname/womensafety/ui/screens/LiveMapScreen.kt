package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveMapScreen(onBack: () -> Unit) {
    var isSharing by remember { mutableStateOf(false) }

    // Root background is solid black
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Column(modifier = Modifier.fillMaxSize()) {
            // --- SECTION A: MAP AREA (Increased to 75% to bring grid further down) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .background(Color(0xFF0D1117))
            ) {
                // Large Zoomed-Out Grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 85.dp.toPx()
                    val gridColor = Color(0xFF1E2631).copy(alpha = 0.5f)

                    // Major Road Lines
                    drawLine(Color.White.copy(0.12f), Offset(size.width * 0.3f, 0f), Offset(size.width * 0.3f, size.height), 2.5f)
                    drawLine(Color.White.copy(0.12f), Offset(size.width * 0.7f, 0f), Offset(size.width * 0.7f, size.height), 2.5f)
                    drawLine(Color.White.copy(0.12f), Offset(0f, size.height * 0.45f), Offset(size.width, size.height * 0.45f), 2.5f)

                    // The Grid
                    for (x in 0..size.width.toInt() step spacing.toInt()) {
                        drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
                    }
                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                        drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                    }
                }

                // Header Overlay
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Live Map", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Your current location", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    GPSBadge()
                }

                // Central Large Red Marker
                LocationPulseMarker(Modifier.align(Alignment.Center))

                // Boundary Line
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(Color.White.copy(0.1f)))
            }

            // --- SECTION B: MINIMAL RED GLOW (Only behind the card area) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF330000))))
            )
        }

        // --- THE FLOATING INFO BOX (Lowered to sit exactly as requested) ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp) // Adjusted to sit lower
                .fillMaxWidth(),
            color = Color(0xFF0A0B10),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Location Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp).background(Color.Red.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Current Location", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("123 Safety Street, Downtown District", color = Color.Gray, fontSize = 14.sp)
                        Text("Lat: 40.7128° N, Long: 74.0060° W", color = Color.DarkGray, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (!isSharing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(0.03f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
                    ) {
                        Text("Location sharing is currently off", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { isSharing = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Live Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    SharingOnUI { isSharing = false }
                }
            }
        }
    }
}

@Composable
fun LocationPulseMarker(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.5f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "scale")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(120.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(Color.Red.copy(0.12f), CircleShape))
        Surface(modifier = Modifier.size(75.dp), shape = CircleShape, color = Color(0xFFE10600), shadowElevation = 20.dp) {
            Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.padding(20.dp).graphicsLayer(rotationZ = 45f))
        }
    }
}

@Composable
fun GPSBadge() {
    Surface(color = Color.Black.copy(0.85f), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.12f))) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
            Spacer(Modifier.width(10.dp))
            Text("GPS Accuracy: High", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SharingOnUI(onStop: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A0505), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(0.2f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, null, tint = Color.Red)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Sharing with 2 contacts", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Sarah Johnson, Michael Chen", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(0.2f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
        Icon(Icons.Default.StopCircle, null)
        Spacer(Modifier.width(12.dp))
        Text("Stop Sharing", fontWeight = FontWeight.Bold)
    }
}