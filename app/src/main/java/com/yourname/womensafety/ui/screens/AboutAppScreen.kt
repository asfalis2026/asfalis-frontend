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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.R

@Composable
fun AboutAppScreen(navController: NavController) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF080404), // Dark red
            Color(0xFF120508)  // Deep red
        )
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
            Text("About Asfalis".tr(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            // --- App Logo with Glow ---
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFFE25F71).copy(0.3f), CircleShape)
                        .blur(50.dp)
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE25F71), Color(0xFF8B0000))))
                        .border(2.dp, Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(55.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Asfalis".tr(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("Intelligent Women's Safety".tr(), color = Color(0xFFE25F71), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

            Spacer(Modifier.height(32.dp))

            // --- Vision Statement ---
            Surface(
                color = Color(0xFF1E1416).copy(0.85f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Asfalis empowers women with advanced protection. We combine on-device Machine Learning, IoT wearable integration, and instant WhatsApp alerts to ensure you are never alone in an emergency.".tr(),
                    color = Color.White.copy(0.85f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- Key Features ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("CORE FEATURES".tr(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))

            FeatureRow("Auto SOS".tr(), "ML-powered shake and fall detection that works offline.".tr())
            Spacer(Modifier.height(12.dp))
            FeatureRow("WhatsApp Integration".tr(), "Instant location tracking links sent to trusted contacts.".tr())
            Spacer(Modifier.height(12.dp))
            FeatureRow("IoT Wearables".tr(), "Seamless connection with Asfalis smart bracelets.".tr())

            Spacer(Modifier.height(40.dp))

            // --- Technical Info ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("APP INFORMATION".tr(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutInfoRow("Version".tr(), "1.1.0 (Build 42)")
                AboutInfoRow("Release Date".tr(), "June 2026")
                AboutInfoRow("Environment".tr(), "Production")
            }

            Spacer(Modifier.height(48.dp))
            
            Icon(Icons.Default.Code, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(16.dp))
            Text("© 2026 Asfalis Technologies.\nBuilt with purpose.".tr(), color = Color.Gray.copy(0.7f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 40.dp))
        }
        }
    }
}

@Composable
private fun FeatureRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1416).copy(0.85f))
            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE25F71)))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(desc, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1416).copy(0.85f))
            .border(1.dp, Color(0xFF1E1416).copy(0.85f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
