package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

// --- DATA CLASS: Defined at top level to fix "Unresolved Reference" ---
data class ContactInfo(
    val initials: String,
    val name: String,
    val phone: String,
    val relation: String
)

@Composable
fun TrustedContactsScreen(onBack: () -> Unit) {
    // Data lists with explicit type
    val emergencyContacts = listOf(
        ContactInfo("SJ", "Sarah Johnson", "+1 (555) 123-4567", "Sister"),
        ContactInfo("MC", "Michael Chen", "+1 (555) 234-5678", "Friend")
    )

    val otherContacts = listOf(
        ContactInfo("ED", "Emily Davis", "+1 (555) 345-6789", "Colleague"),
        ContactInfo("RW", "Robert Wilson", "+1 (555) 456-7890", "Neighbor")
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
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Button container with padding for bottom system navigation bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Button(
                        onClick = { /* Implement Add Logic */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC60000)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Contact", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Pushes header below the clock/camera notch
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // --- Header ---
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.08f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Trusted Contacts",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Add people who will receive emergency alerts and your live location",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                    lineHeight = 20.sp
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    // Uses innerPadding to ensure list content isn't hidden by the Floating Button
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Emergency Section Header
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFC60000), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Emergency Contacts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(emergencyContacts) { contact ->
                        ContactItem(contact, isEmergency = true)
                    }

                    // Other Section Header
                    item {
                        Text(
                            "Other Contacts",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    items(otherContacts) { contact ->
                        ContactItem(contact, isEmergency = false)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: ContactInfo, isEmergency: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF140808).copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
        border = if (isEmergency) androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isEmergency) Color(0xFFC60000) else Color.White.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(contact.initials, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(contact.phone, color = Color.Gray, fontSize = 13.sp)
            }

            // Relationship Tag
            Surface(
                color = if (isEmergency) Color.Red.copy(0.12f) else Color.White.copy(0.05f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = contact.relation,
                    color = if (isEmergency) Color.Red else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            IconButton(onClick = { /* Delete Logic */ }) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = Color.White.copy(0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}