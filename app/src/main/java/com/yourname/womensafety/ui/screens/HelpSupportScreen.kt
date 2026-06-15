package com.yourname.womensafety.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.yourname.womensafety.utils.tr
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.R
import com.yourname.womensafety.data.network.dto.FaqItem
import com.yourname.womensafety.ui.tour.LocalTourEngine
import com.yourname.womensafety.ui.viewmodels.HelpSupportViewModel
import com.yourname.womensafety.ui.viewmodels.HelpUiState
import com.yourname.womensafety.ui.viewmodels.TicketUiState

private fun faqIconFor(iconName: String): ImageVector = when (iconName.lowercase()) {
    "timeline"    -> Icons.Default.Timeline
    "flash_on"    -> Icons.Default.FlashOn
    "lock"        -> Icons.Default.Lock
    "security"    -> Icons.Default.Security
    "message"     -> Icons.Default.Message
    "location_on" -> Icons.Default.LocationOn
    "shield"      -> Icons.Default.Shield
    "warning"     -> Icons.Default.Warning
    "info"        -> Icons.Default.Info
    "settings"    -> Icons.Default.Settings
    "bluetooth"   -> Icons.Default.Bluetooth
    "phone"       -> Icons.Default.Phone
    "contacts"    -> Icons.Default.Contacts
    else          -> Icons.Default.HelpOutline
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {

    val helpViewModel: HelpSupportViewModel = viewModel(factory = HelpSupportViewModel.Factory)
    val uiState     by helpViewModel.uiState.collectAsStateWithLifecycle()
    val ticketState by helpViewModel.ticketState.collectAsStateWithLifecycle()
    val searchQuery by helpViewModel.searchQuery.collectAsStateWithLifecycle()

    var expandedIndex     by remember { mutableStateOf<Int?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    var ticketSubject     by remember { mutableStateOf("") }
    var ticketMessage     by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(ticketState) {
        if (ticketState is TicketUiState.Submitted) {
            showContactDialog = false
            ticketSubject = ""
            ticketMessage = ""
            helpViewModel.resetTicketState()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundGradient)
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            alpha = 0.35f,
            contentScale = ContentScale.FillWidth,
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
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(60.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)).border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Help & Support".tr(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Find answers to common questions".tr(), color = Color.Gray, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        TextField(
            value = searchQuery,
            onValueChange = { helpViewModel.onSearchQueryChange(it) },
            placeholder = { Text("Search for help...".tr(), color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF1E1416).copy(0.85f),
                focusedContainerColor   = Color(0xFFE25F71).copy(0.2f),
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White
            )
        )

        Spacer(Modifier.height(24.dp))

        when (val state = uiState) {
            is HelpUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE25F71))
                }
            }
            is HelpUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color(0xFFE25F71), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { helpViewModel.onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                        ) { Text("Retry".tr()) }
                    }
                }
            }
            is HelpUiState.Success -> {
                val tourEngine = LocalTourEngine.current
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ── Replay Tour Banner ────────────────────────────────────────────
                    if (tourEngine != null) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tourEngine.restart()
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                color = Color(0xFFE25F71).copy(0.07f),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFE25F71).copy(0.32f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE25F71).copy(0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFFE25F71),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Replay App Tour".tr(),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Take the guided walkthrough again".tr(),
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFFE25F71).copy(0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --- Quick Guides Section ---
                    item {
                        Text("Quick Guides".tr(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                        QuickGuideCard(
                            icon = Icons.Default.Sensors,
                            title = "How Auto SOS Works".tr(),
                            content = "Keep the app in the background. If a strong shake or fall is detected by the ML model, a 10-second countdown begins before alerting contacts.".tr()
                        )
                        Spacer(Modifier.height(8.dp))
                        QuickGuideCard(
                            icon = Icons.Default.Groups,
                            title = "Trusted Contacts".tr(),
                            content = "Add up to 3 contacts. When an SOS is triggered, they will receive an SMS and WhatsApp message with a link to track your live location.".tr()
                        )
                        Spacer(Modifier.height(8.dp))
                        QuickGuideCard(
                            icon = Icons.Default.Lock,
                            title = "App Lock & Security".tr(),
                            content = "Secure your app with a PIN or fingerprint to prevent unauthorized access to your safety settings.".tr()
                        )
                        Spacer(Modifier.height(8.dp))
                        QuickGuideCard(
                            icon = Icons.Default.Warning,
                            title = "Emergency Bypass".tr(),
                            content = "If forced to unlock the app, tap the shield icon 7 times and enter 'ASFALIS' to instantly bypass the lock.".tr()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Frequently Asked Questions".tr(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (state.faqs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                Text("No results found.".tr(), color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        itemsIndexed(state.faqs) { index, faq ->
                            ApiFAQCard(
                                faq = faq,
                                isExpanded = expandedIndex == index,
                                onClick = { expandedIndex = if (expandedIndex == index) null else index }
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFE25F71).copy(0.25f), Color.Transparent)))
                                .border(1.dp, Color(0xFFE25F71).copy(0.2f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(Color(0xFFE25F71).copy(0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color(0xFFE25F71))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Still need help?".tr(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("Our support team is available 24/7 at fyear2022.26@gmail.com".tr(), color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { showContactDialog = true },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Ticket".tr(), fontWeight = FontWeight.Bold)
                                    }
                                    
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Button(
                                        onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                data = android.net.Uri.parse("mailto:fyear2022.26@gmail.com")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Email Us".tr(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
    } // closes Box

    if (showContactDialog) {
        Dialog(onDismissRequest = { showContactDialog = false; helpViewModel.resetTicketState() }) {
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF160E0E))
                    .border(1.dp, Color(0xFFE25F71).copy(0.3f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text("Contact Support".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = ticketSubject,
                    onValueChange = { ticketSubject = it },
                    label = { Text("Subject *".tr(), color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1416).copy(0.85f), unfocusedContainerColor = Color(0xFF1E1416).copy(0.85f),
                        focusedBorderColor = Color(0xFFE25F71), unfocusedBorderColor = Color.White.copy(0.2f)
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ticketMessage,
                    onValueChange = { ticketMessage = it },
                    label = { Text("Message *".tr(), color = Color.Gray) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1416).copy(0.85f), unfocusedContainerColor = Color(0xFF1E1416).copy(0.85f),
                        focusedBorderColor = Color(0xFFE25F71), unfocusedBorderColor = Color.White.copy(0.2f)
                    )
                )
                (ticketState as? TicketUiState.Error)?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err.message, color = Color(0xFFE25F71), fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showContactDialog = false; helpViewModel.resetTicketState() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                    ) { Text("Cancel".tr()) }
                    Button(
                        onClick = {
                            if (ticketSubject.isNotBlank() && ticketMessage.isNotBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:fyear2022.26@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, ticketSubject.trim())
                                    putExtra(android.content.Intent.EXTRA_TEXT, ticketMessage.trim())
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle case where no email app is installed
                                }
                                showContactDialog = false
                                helpViewModel.resetTicketState()
                            }
                        },
                        enabled = ticketState !is TicketUiState.Submitting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
                    ) {
                        if (ticketState is TicketUiState.Submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Submit".tr())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickGuideCard(icon: ImageVector, title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1416).copy(0.85f))
            .border(1.dp, Color(0xFFE25F71).copy(0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.2f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFFE25F71), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(content, color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun ApiFAQCard(faq: FaqItem, isExpanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF1E1416).copy(0.85f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(faqIconFor(faq.icon), null, tint = Color(0xFFE25F71), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    faq.question.tr(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color.Gray
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1E1416).copy(0.85f))
                    Spacer(Modifier.height(12.dp))
                    Text(faq.answer.tr(), color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
