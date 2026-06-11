package com.yourname.womensafety.ui.screens

import com.yourname.womensafety.utils.tr
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.yourname.womensafety.R

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    val bg = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)).border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Privacy Policy".tr(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Last Updated: June 2026".tr(), color = Color.Gray, fontSize = 12.sp)
            }
        }

        // --- Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text("At Asfalis, your safety and privacy are our highest priorities. We only collect the data absolutely necessary to protect you in an emergency.".tr(),
                color = Color.White.copy(0.8f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PolicySectionCard(
                icon = Icons.Default.LocationOn,
                title = "1. Location Data".tr(),
                content = "We collect your real-time GPS coordinates ONLY when an SOS alert is actively triggered. This location is securely shared with your trusted contacts via WhatsApp and SMS. We do not track your location in the background when you are safe.".tr()
            )

            PolicySectionCard(
                icon = Icons.Default.Sensors,
                title = "2. Sensor Data & Machine Learning".tr(),
                content = "If 'Auto SOS' is enabled, the app monitors your device's accelerometer for sudden impacts or vigorous shakes. This analysis happens entirely ON-DEVICE using our local ML models. Raw motion data is never uploaded to our servers.".tr()
            )

            PolicySectionCard(
                icon = Icons.Default.Watch,
                title = "3. IoT Wearables".tr(),
                content = "When connected to an Asfalis smart bracelet, the app communicates locally via Bluetooth. Button presses trigger the app directly. We do not store Bluetooth pairing history beyond your current session.".tr()
            )

            PolicySectionCard(
                icon = Icons.Default.ContactPhone,
                title = "4. Contacts & Communication".tr(),
                content = "The emergency contacts you add are stored securely on our encrypted database so we can dispatch alerts if your device is destroyed. Messages sent to WhatsApp are encrypted by Meta and cannot be intercepted by us.".tr()
            )

            PolicySectionCard(
                icon = Icons.Default.DeleteForever,
                title = "5. Data Retention & Deletion".tr(),
                content = "You have the right to request deletion of your account at any time. When you delete your account, your profile, contacts, and SOS history are permanently wiped from our databases within 24 hours.".tr()
            )

            Spacer(Modifier.height(16.dp))

            // --- Contact Box ---
            Surface(
                color = Color(0xFF2A161A).copy(0.9f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MailOutline, null, tint = Color(0xFFE25F71), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Questions about your privacy?".tr(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("fyear2022.26@gmail.com".tr(), color = Color(0xFFE25F71), fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
        }
    }
}

@Composable
private fun PolicySectionCard(icon: ImageVector, title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1416).copy(0.85f))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE25F71).copy(0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFFE25F71), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(content, color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
    }
}
