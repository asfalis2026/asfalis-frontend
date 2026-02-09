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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class FAQItem(val question: String, val answer: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun HelpSupportScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    // --- GRADIENT DEFINITION (Matches About App/Profile style) ---
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF1A0000), // Very dark red
            Color(0xFF330000)  // Deep red blend at the bottom
        )
    )

    val faqs = listOf(
        FAQItem("How does motion detection work?", "Our app uses your device's accelerometer to detect unusual movements. When detected, a countdown begins before an SOS is sent.", Icons.Default.Timeline),
        FAQItem("When is SOS triggered automatically?", "SOS is triggered on sudden impacts, falls, or vigorous shaking based on your sensitivity settings.", Icons.Default.FlashOn),
        FAQItem("What permissions does the app require?", "The app needs Location, Motion, Contacts, and SMS permissions to function effectively.", Icons.Default.Lock),
        FAQItem("How is my location data protected?", "Data is encrypted end-to-end and only shared with your trusted contacts during an active emergency.", Icons.Default.Security),
        FAQItem("Can I customize the SOS message?", "Yes, navigate to settings to change the pre-defined text sent to your contacts.", Icons.Default.Message)
    )

    // Updated background to use the backgroundGradient
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
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search for help...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(0.05f),
                focusedContainerColor = Color.White.copy(0.08f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(faqs) { index, faq ->
                FAQCard(faq, expandedIndex == index) {
                    expandedIndex = if (expandedIndex == index) null else index
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
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFE10600).copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
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
                            onClick = {},
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

@Composable
fun FAQCard(faq: FAQItem, isExpanded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(if (isExpanded) Color(0xFFE10600).copy(0.2f) else Color.White.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(faq.icon, null, tint = if (isExpanded) Color(0xFFE10600) else Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(faq.question, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = faq.answer,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp, start = 48.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}