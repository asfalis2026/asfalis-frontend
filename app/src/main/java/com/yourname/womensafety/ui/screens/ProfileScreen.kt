package com.yourname.womensafety.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.ui.viewmodels.ProfileUiState
import com.yourname.womensafety.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(navController: NavController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory
    )
    val uiState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val deleteError by profileViewModel.updateError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    // Reset spinner and surface error when deleteAccount() fails
    LaunchedEffect(deleteError) {
        if (deleteError != null) {
            isDeletingAccount = false
        }
    }

    // Navigate to login on logout or account deletion
    LaunchedEffect(uiState) {
        when (uiState) {
            is ProfileUiState.LoggedOut,
            is ProfileUiState.AccountDeleted -> {
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
            else -> Unit
        }
    }

    val displayName = when (val s = uiState) {
        is ProfileUiState.Success -> s.profile.fullName
        else -> "Loading..."
    }
    val displayPhone = when (val s = uiState) {
        is ProfileUiState.Success -> s.profile.phone ?: "Not set"
        else -> ""
    }
    val initials = displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

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
            when (val s = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE10600), modifier = Modifier.size(32.dp))
                    }
                }
                is ProfileUiState.Error -> {
                    Text(s.message, color = Color(0xFFE10600), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(70.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(Color(0xFFE10600), Color(0xFF8B0000)))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(displayPhone, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Main Actions ---
            Text("Settings & Customization", color = Color(0xFFE10600), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

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
                InfoRowCompact("Phone", displayPhone)
                HorizontalDivider(color = Color.White.copy(0.05f))
                InfoRowCompact("Country", if (uiState is ProfileUiState.Success) (uiState as ProfileUiState.Success).profile.country ?: "Not set" else "")
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
                    onClick = { showDeleteDialog = true },
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

        // --- Logout Dialog ---
        if (showLogoutDialog) {
            Dialog(onDismissRequest = { showLogoutDialog = false }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.width(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF0F0F0F)).padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Sign Out", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Are you sure you want to sign out?", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { showLogoutDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                            ) { Text("Cancel", color = Color.White) }
                            Button(
                                onClick = {
                                    showLogoutDialog = false
                                    profileViewModel.logout()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600))
                            ) { Text("Sign Out", color = Color.White) }
                        }
                    }
                }
            }
        }

        // --- Delete Account Dialog ---
        if (showDeleteDialog) {
            Dialog(onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteDialog = false
                    profileViewModel.clearUpdateError()
                }
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0F0F0F))
                            .border(1.dp, Color(0xFFE10600).copy(0.3f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning, null,
                                tint = Color(0xFFE10600),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Delete Account", color = Color(0xFFE10600), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This will permanently delete your account and all associated data including trusted contacts and SOS history.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This action cannot be undone.",
                            color = Color(0xFFE10600),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (deleteError != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                deleteError!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    profileViewModel.clearUpdateError()
                                },
                                enabled = !isDeletingAccount,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                            ) { Text("Cancel", color = Color.White) }
                            Button(
                                onClick = {
                                    isDeletingAccount = true
                                    profileViewModel.deleteAccount()
                                },
                                enabled = !isDeletingAccount,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isDeletingAccount) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                }
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
