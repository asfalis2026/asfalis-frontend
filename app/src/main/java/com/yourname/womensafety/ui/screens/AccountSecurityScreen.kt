package com.yourname.womensafety.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.repository.NetworkResult
import kotlinx.coroutines.launch
import java.io.File


@Composable
fun AccountSecurityScreen(navController: NavController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    
    var appLockEnabled by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        appLockEnabled = AppServiceLocator.appCache.isAppLockEnabled()
    }

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        // Subtle radial glow from top-center
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp).background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFE25F71).copy(0.08f), Color.Transparent),
                    radius = 700f
                )
            )
        )
        // Background image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.yourname.womensafety.R.drawable.background_image),
            contentDescription = null,
            alpha = 0.35f,
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        )
        // Overlay to blend the image seamlessly into the background
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color.Transparent, Color(0xFF120508)),
                    startY = 0f
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(Color(0xFFE25F71).copy(0.25f))
                        .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Account Security".tr(), color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Manage your privacy and data".tr(), color = Color(0xFFF09CA8), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 80.dp)
            ) {

            // ── Account Overview ──────────────────────────────────────────────
            SecuritySectionHeader("Your Account".tr())
            SecurityCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneIphone, null, tint = Color(0xFFE25F71))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Registered Device".tr(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("This device is authorized for SOS alerts".tr(), color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(24.dp))



            // ── Data & Privacy Controls ───────────────────────────────────────
            SecuritySectionHeader("Data Controls".tr())
            var isExporting by remember { mutableStateOf(false) }
            SecurityCard {
                SecurityRow(
                    icon = if (isExporting) Icons.Default.HourglassTop else Icons.Default.Download,
                    title = "Export My Data".tr(),
                    subtitle = if (isExporting) "Building CSV… please wait".tr() else "Download last 100 SOS alert records as CSV".tr(),
                    tint = Color(0xFF4FC3F7),
                    onClick = {
                        if (!isExporting) {
                            isExporting = true
                            scope.launch {
                                try {
                                    // Fetch real SOS history from the server
                                    val result = AppServiceLocator.sosRepository.getSosHistory()
                                    val items = when (result) {
                                        is NetworkResult.Success -> result.data.take(100)
                                        else -> emptyList()
                                    }

                                    val file = File(context.cacheDir, "asfalis_sos_export.csv")
                                    val sb = StringBuilder()
                                    sb.appendLine("id,triggered_at,trigger_type,status,address,resolved_at")
                                    items.forEach { item ->
                                        sb.append(item.id).append(',')
                                            .append(item.triggeredAt).append(',')
                                            .append(item.triggerType).append(',')
                                            .append(item.status).append(',')
                                            .append("\"${item.address ?: ""}\"").append(',')
                                            .appendLine(item.resolvedAt ?: "")
                                    }
                                    if (items.isEmpty()) {
                                        sb.appendLine("# No SOS records found on your account.")
                                    }
                                    file.writeText(sb.toString())

                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Asfalis SOS Export — ${items.size} records")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share SOS Export (${items.size} records)")
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Export failed: ".trNonComposable() + "${e.message}")
                                } finally {
                                    isExporting = false
                                }
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── App Protection ────────────────────────────────────────────────
            SecuritySectionHeader("App Protection".tr())
            SecurityCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color(0xFFE25F71).copy(0.15f))
                        .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Fingerprint, null, tint = Color(0xFFE25F71))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric & PIN Lock".tr(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Require authentication to open Asfalis".tr(), color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = {
                            if (it) {
                                showPinSetupDialog = true
                            } else {
                                scope.launch {
                                    AppServiceLocator.appCache.setAppLockEnabled(false)
                                    appLockEnabled = false
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE25F71), checkedTrackColor = Color(0xFFE25F71).copy(0.3f))
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Security Tips ─────────────────────────────────────────────────
            SecurityTipsCard()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )

        // --- Logout Dialog ---
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Sign Out".tr(), color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to sign out?".tr(), color = Color.Gray) },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel".tr(), color = Color.Gray) }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            scope.launch {
                                AppServiceLocator.tokenManager.clearTokens()
                                AppServiceLocator.appCache.clearAll()
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                    ) {
                        Text("Sign Out".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- Clear Data Dialog ---
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Clear Local Data".tr(), color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("This will wipe all cached SOS history and settings. Your account will not be deleted, and data will be re-fetched from the server. Continue?".tr(), color = Color.Gray) },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel".tr(), color = Color.Gray) }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDataDialog = false
                            scope.launch {
                                AppServiceLocator.appCache.clearAll()
                                snackbarHostState.showSnackbar("Local data cleared successfully".trNonComposable())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                    ) {
                        Text("Clear".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- PIN Setup Dialog ---
        if (showPinSetupDialog) {
            AlertDialog(
                onDismissRequest = { showPinSetupDialog = false; newPin = "" },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Set App Lock PIN".tr(), color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        // Warning box
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFB74D).copy(0.1f))
                                .border(1.dp, Color(0xFFFFB74D).copy(0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("⚠️ Remember this PIN — it will be required to unlock Asfalis if biometrics are unavailable. There is a hidden emergency bypass if you forget it.".tr(),
                                    color = Color(0xFFFFB74D), fontSize = 12.sp, lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Enter a 4-digit PIN as fallback for biometrics.".tr(), color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4) newPin = it },
                            label = { Text("4-digit PIN".tr(), color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE25F71)
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinSetupDialog = false; newPin = "" }) { Text("Cancel".tr(), color = Color.Gray) }
                },
                confirmButton = {
                    Button(
                        enabled = newPin.length == 4,
                        onClick = {
                            showPinSetupDialog = false
                            scope.launch {
                                AppServiceLocator.appCache.setAppLockPin(newPin)
                                AppServiceLocator.appCache.setAppLockEnabled(true)
                                appLockEnabled = true
                                newPin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                    ) {
                        Text("Enable App Lock".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SecuritySectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)) {
        Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFE25F71)))
        Spacer(Modifier.width(10.dp))
        Text(
            title.uppercase(),
            color = Color(0xFFF09CA8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SecurityCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1C1212), Color(0xFF160E0E))
                )
            )
            .border(1.dp, Color(0xFFE25F71).copy(0.18f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun SecurityRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SecurityTipsCard() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF160B00), Color(0xFF231100))))
            .border(1.dp, Color(0xFFFFB74D).copy(0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFFB74D))
            Spacer(Modifier.width(10.dp))
            Text("Security Best Practices".tr(), color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))
        TipRow("1", "Review your trusted contacts monthly to ensure they are up to date.".tr())
        Spacer(Modifier.height(8.dp))
        TipRow("2", "Test your SOS trigger in a safe environment once a month.".tr())
        Spacer(Modifier.height(8.dp))
        TipRow("3", "Do not share your Asfalis login OTP with anyone.".tr())
    }
}

@Composable
private fun TipRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFFFB74D).copy(0.2f)),
            contentAlignment = Alignment.Center) {
            Text(number, color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White.copy(0.8f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}
