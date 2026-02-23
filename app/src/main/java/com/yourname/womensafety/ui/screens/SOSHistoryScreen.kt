package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Mock data class for the history items
data class SOSAlert(val date: String, val time: String, val location: String)

@Composable
fun SOSHistoryScreen(navController: NavController) {
    // Mock Data
    val alertHistory = listOf(
        SOSAlert("Oct 24, 2025", "10:30 PM", "Sector 62, Noida"),
        SOSAlert("Oct 12, 2025", "08:15 PM", "Cyber City, Gurgaon"),
        SOSAlert("Sept 30, 2025", "11:45 PM", "Hauz Khas, Delhi")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color(0xFF1a0000), Color(0xFF2d0000))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // --- RESTORED BACK BUTTON ---
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- Header: Consistent with Dashboard ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ASFALIS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Alert History", color = Color.Gray, fontSize = 16.sp)
                }
                Surface(
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "History Icon",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (alertHistory.isEmpty()) {
                // Empty State
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No SOS alerts triggered yet.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                // Scrollable List of History
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(alertHistory) { alert ->
                        HistoryCard(alert)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(alert: SOSAlert) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red Alert Icon Circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE10600).copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    null,
                    tint = Color(0xFFE10600),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.location,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${alert.date} • ${alert.time}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            // "Alert Sent" Status tag
            Surface(
                color = Color(0xFFE10600).copy(0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "SENT",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFFE10600),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}