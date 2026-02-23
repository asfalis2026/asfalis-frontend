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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // These states would ideally be handled by a ViewModel and saved to DataStore/Room
    var sensitivity by remember { mutableStateOf("Medium") }
    var sosMessage by remember {
        mutableStateOf("Emergency! I need help. This is an automated SOS alert from ASFALIS. My live location is attached.")
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF2D0000))
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

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("App Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // Save Checkmark Icon
            IconButton(
                onClick = {
                    // Add Save Logic Here (Backend/SharedPrefs)
                    navController.popBackStack()
                },
                modifier = Modifier.clip(CircleShape).background(Color(0xFFE10600))
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(40.dp))

        // --- SECTION: SOS MESSAGE ---
        SettingsHeader(Icons.Default.Message, "SOS Message Content")
        Text(
            "This message will be sent to all your trusted contacts when SOS is triggered.",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = sosMessage,
            onValueChange = { sosMessage = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(0.05f),
                unfocusedContainerColor = Color.White.copy(0.05f),
                focusedBorderColor = Color(0xFFE10600),
                unfocusedBorderColor = Color.White.copy(0.1f)
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp)
        )

        Spacer(Modifier.height(32.dp))

        // --- SECTION: SHAKE SENSITIVITY ---
        SettingsHeader(Icons.Default.GraphicEq, "Shake Sensitivity")
        Text(
            "Adjust the intensity of the shake required to automatically trigger an SOS alert.",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(0.03f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { level ->
                        val isSelected = sensitivity == level
                        Button(
                            onClick = { sensitivity = level }, // Haptic removed as requested
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFE10600) else Color.White.copy(0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = level,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Status Description
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.05f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = when(sensitivity) {
                            "Low" -> "Optimized for high-activity environments. Requires a very vigorous shake."
                            "High" -> "Maximum sensitivity. Triggers easily; recommended for emergency use only."
                            else -> "Balanced detection. Triggers on a deliberate, strong shake."
                        },
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // Help Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Motion data is analyzed locally for your privacy.", color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SettingsHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}