package com.yourname.womensafety.ui.screens

import android.content.Context
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
    val context = LocalContext.current
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF2D0000))
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
                Text("Account", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            // --- Profile Identity Section ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(70.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFE10600), Color(0xFF8B0000)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Jessica Parker", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("jessica.parker@email.com", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Main Actions ---
            Text("Settings & Customization", color = Color(0xFFE10600), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            // NEW: The Settings Button
            Surface(
                onClick = { navController.navigate("settings") },
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = Color.White)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("App Settings", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("SOS Message, Shake Sensitivity", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Compact Info Section ---
            Text("Personal Details", color = Color(0xFFE10600), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.03f))
                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(20.dp))
            ) {
                InfoRowCompact("Phone", "+1 (555) 123-4567")
                HorizontalDivider(color = Color.White.copy(0.05f))
                InfoRowCompact("Emergency Contact", "+1 (911) 000-0000")
            }

            Spacer(Modifier.height(32.dp))

            // --- Footer Style Quick Links ---
            Text("Support & Legal", color = Color(0xFFE10600), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                FooterLink("Emergency Contacts") { navController.navigate("contacts") }
                FooterLink("Help & Support") { navController.navigate("help") }
                FooterLink("Privacy Policy") { navController.navigate("privacy_policy") }
                FooterLink("About Asfalis") { navController.navigate("about") }
            }

            Spacer(Modifier.height(40.dp))

            // --- Destructive Actions ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Sign Out", color = Color.White)
                }

                Button(
                    onClick = { /* Handle Delete */ },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE10600).copy(0.5f))
                ) {
                    Text("Delete Account", color = Color(0xFFE10600), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(120.dp))
        }

        // --- Logout Dialog (Keep your existing working logic) ---
        if (showLogoutDialog) {
            Dialog(onDismissRequest = { showLogoutDialog = false }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.width(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF0F0F0F)).padding(24.dp)) {
                        Text("Confirm Logout", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Are you sure you want to log out?", color = Color.Gray)
                        Spacer(Modifier.height(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showLogoutDialog = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))) {
                                Text("No")
                            }
                            Button(onClick = {
                                val sharedPref = context.getSharedPreferences("raksha_prefs", Context.MODE_PRIVATE)
                                sharedPref.edit().putBoolean("is_logged_in", false).apply()
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600))) {
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRowCompact(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FooterLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.7f), fontSize = 15.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp))
    }
}