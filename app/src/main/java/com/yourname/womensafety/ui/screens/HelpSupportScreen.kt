package com.yourname.womensafety.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.data.network.dto.FaqItem
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

    LaunchedEffect(ticketState) {
        if (ticketState is TicketUiState.Submitted) {
            showContactDialog = false
            ticketSubject = ""
            ticketMessage = ""
            helpViewModel.resetTicketState()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color.Black, Color(0xFF1A0000), Color(0xFF330000))
    )

    Column(modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(60.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Help & Support", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Find answers to common questions", color = Color.Gray, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        TextField(
            value = searchQuery,
            onValueChange = { helpViewModel.onSearchQueryChange(it) },
            placeholder = { Text("Search for help...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(0.05f),
                focusedContainerColor   = Color.White.copy(0.08f),
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
                    CircularProgressIndicator(color = Color(0xFFE10600))
                }
            }
            is HelpUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color(0xFFE10600), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { helpViewModel.onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600))
                        ) { Text("Retry") }
                    }
                }
            }
            is HelpUiState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.faqs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                Text("No results found.", color = Color.Gray, fontSize = 15.sp)
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
                                .background(Brush.linearGradient(listOf(Color(0xFFE10600).copy(0.15f), Color.Transparent)))
                                .border(1.dp, Color(0xFFE10600).copy(0.2f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(Color(0xFFE10600).copy(0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, tint = Color(0xFFE10600))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Still need help?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("Our support team is available 24/7 to assist you.", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = { showContactDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Contact Support", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        Dialog(onDismissRequest = { showContactDialog = false; helpViewModel.resetTicketState() }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1A0000))
                        .padding(24.dp)
                ) {
                    Text("Contact Support", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = ticketSubject,
                        onValueChange = { ticketSubject = it },
                        label = { Text("Subject *", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.05f),
                            focusedBorderColor = Color(0xFFE10600), unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ticketMessage,
                        onValueChange = { ticketMessage = it },
                        label = { Text("Message *", color = Color.Gray) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.05f),
                            focusedBorderColor = Color(0xFFE10600), unfocusedBorderColor = Color.White.copy(0.2f)
                        )
                    )
                    (ticketState as? TicketUiState.Error)?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(err.message, color = Color(0xFFE10600), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showContactDialog = false; helpViewModel.resetTicketState() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (ticketSubject.isNotBlank() && ticketMessage.isNotBlank()) {
                                    helpViewModel.submitTicket(ticketSubject.trim(), ticketMessage.trim())
                                }
                            },
                            enabled = ticketState !is TicketUiState.Submitting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600))
                        ) {
                            if (ticketState is TicketUiState.Submitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Submit")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiFAQCard(faq: FaqItem, isExpanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isExpanded) Color(0xFFE10600).copy(0.2f) else Color.White.copy(0.05f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        faqIconFor(faq.icon),
                        null,
                        tint = if (isExpanded) Color(0xFFE10600) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(faq.question, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = Color.Gray
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    faq.answer,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
