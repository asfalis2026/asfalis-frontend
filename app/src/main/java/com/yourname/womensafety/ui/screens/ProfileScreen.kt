package com.yourname.womensafety.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController

@Composable
fun ProfileScreen(navController: NavController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current // Added context for persistence

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF1A0000),
            Color(0xFF330000)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            // --- Header ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // --- Profile Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.05f))))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(70.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(Color(0xFFE10600), Color(0xFF8B0000)))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(35.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Jessica Parker", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("jessica.parker@email.com", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        color = Color(0xFF00FF00).copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00).copy(0.2f))
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF00), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("System Active", color = Color(0xFF00FF00), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Personal Info ---
            Text("Personal Information", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp))
            ) {
                InfoRow("Phone Number", "+1 (555) 123-4567")
                HorizontalDivider(color = Color.White.copy(0.1f), thickness = 1.dp)
                InfoRow("Emergency Contact", "+1 (911) 000-0000")
                HorizontalDivider(color = Color.White.copy(0.1f), thickness = 1.dp)
                InfoRow("Member Since", "January 2026")
            }

            Spacer(Modifier.height(32.dp))

            // --- Quick Links ---
            Text("Quick Links", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(Icons.Default.Shield, "Emergency Contacts", Color(0xFFE10600)) {
                    navController.navigate("contacts")
                }
                MenuButton(Icons.Default.HelpOutline, "Help & Support", Color(0xFF60A5FA)) {
                    navController.navigate("help")
                }
                MenuButton(Icons.Default.Description, "About App", Color.Gray) {
                    navController.navigate("about")
                }
                MenuButton(Icons.Default.Lock, "Privacy Policy", Color.Gray) {
                    navController.navigate("privacy_policy")
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Sign Out ---
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(100.dp))
        }

        // --- Logout Dialog ---
        if (showLogoutDialog) {
            Dialog(
                onDismissRequest = { showLogoutDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0F0F0F))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Text("Confirm Logout", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Are you sure you want to log out?", color = Color.Gray, fontSize = 15.sp)
                        Spacer(Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showLogoutDialog = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    // FIXED: Save 'false' so Splash knows we are logged out
                                    val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                                    sharedPref.edit().putBoolean("is_logged_in", false).apply()

                                    showLogoutDialog = false
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(16.dp)) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MenuButton(icon: ImageVector, label: String, iconColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(label, color = Color.White, fontSize = 15.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}
