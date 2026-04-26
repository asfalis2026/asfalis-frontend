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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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
        colors = listOf(Color(0xFF0A0A0A), Color(0xFF1A0000), Color(0xFF0D0000))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            // Back button row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(0.08f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("My Account", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // ── Hero Banner ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3A0000), Color(0xFF1A0000), Color(0xFF0D0000))
                        )
                    )
                    .border(1.dp, Color(0xFFE10600).copy(0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFE10600), Color(0xFF8B0000)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            when (val s = uiState) {
                                is ProfileUiState.Loading -> {
                                    CircularProgressIndicator(color = Color(0xFFE10600), modifier = Modifier.size(24.dp))
                                }
                                is ProfileUiState.Error -> {
                                    Text(s.message, color = Color(0xFFE10600), fontSize = 12.sp)
                                }
                                else -> {
                                    Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(displayPhone, color = Color.Gray, fontSize = 13.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF4CAF50).copy(0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "✓  Verified Account",
                                            color = Color(0xFF4CAF50), fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(0.07f))
                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem("Country",
                            if (uiState is ProfileUiState.Success)
                                (uiState as ProfileUiState.Success).profile.country ?: "—" else "—")
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))
                        ProfileStatItem("Phone", displayPhone.ifEmpty { "—" })
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.1f)))
                        ProfileStatItem("Status", "Active")
                    }
                }
            }


            Spacer(Modifier.height(20.dp))

            // ── Settings & Customization ─────────────────────────────────────
            ProfileSectionLabel("Settings & Customization")
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.03f))
                    .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(20.dp))
            ) {
                ProfileMenuRow(Icons.Default.Tune, "App Settings", "SOS message, sensitivity", Color(0xFFE10600)) { navController.navigate("settings") }
                HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Default.People, "Trusted Contacts", "Manage emergency contacts", Color(0xFF4FC3F7)) { navController.navigate("contacts") }
                HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Default.History, "SOS History", "View past alerts", Color(0xFFFFB74D)) { navController.navigate("sos_history") }
            }

            Spacer(Modifier.height(16.dp))

            // ── Support & Legal ──────────────────────────────────────────────
            ProfileSectionLabel("Support & Legal")
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.03f))
                    .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(20.dp))
            ) {
                ProfileMenuRow(Icons.AutoMirrored.Filled.HelpOutline, "Help & Support", "FAQs and contact us", Color(0xFF4CAF50)) { navController.navigate("help") }
                HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Default.Lock, "Privacy Policy", "How we protect your data", Color(0xFF9C8FFF)) { navController.navigate("privacy_policy") }
                HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Default.Info, "About Asfalis", "Version & app info", Color(0xFFFFAA00)) { navController.navigate("about") }
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
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color.White.copy(0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                },
                title = {
                    Text("Sign Out", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Are you sure you want to sign out? You'll need to log in again to access your account.",
                        color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color.White.copy(0.7f))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showLogoutDialog = false; profileViewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            )
        }

        // --- Delete Account Dialog ---
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingAccount) { showDeleteDialog = false; profileViewModel.clearUpdateError() } },
                containerColor = Color(0xFF141414),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color(0xFFE10600).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE10600), modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text("Delete Account", color = Color(0xFFE10600), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            "This will permanently delete your account and all associated data including trusted contacts and SOS history.",
                            color = Color.Gray, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ This action cannot be undone.",
                            color = Color(0xFFE10600), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        if (deleteError != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(deleteError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false; profileViewModel.clearUpdateError() },
                        enabled = !isDeletingAccount
                    ) {
                        Text("Cancel", color = Color.White.copy(0.7f))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { isDeletingAccount = true; profileViewModel.deleteAccount() },
                        enabled = !isDeletingAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
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

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFFE10600),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(0.25f), modifier = Modifier.size(18.dp))
    }
}
