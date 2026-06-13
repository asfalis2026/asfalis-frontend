package com.yourname.womensafety.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.womensafety.utils.tr
import com.yourname.womensafety.utils.trNonComposable
import androidx.navigation.NavController

@Composable
fun PremiumFeaturesScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF0A0404), Color(0xFF14080A))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE25F71).copy(0.25f))
                        .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hardware Features\n& Specifications".tr(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Everything you need to know about the Asfalis Smart Wearable before purchasing.".tr(),
                color = Color.White.copy(0.7f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Feature Sections
            FeatureDetailCard(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth 5.0 Low Energy".tr(),
                description = "Maintains a constant, ultra-low power connection to your phone without draining its battery. Provides a seamless link for instant SOS triggers.".tr()
            )

            FeatureDetailCard(
                icon = Icons.Default.TouchApp,
                title = "Tactile SOS Button".tr(),
                description = "Designed to be easily pressed without looking, but requires deliberate force to prevent accidental triggers. Double-tap to cancel.".tr()
            )

            FeatureDetailCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Extended Battery Life".tr(),
                description = "Lasts up to 7 days on a single charge. Charges via a standard USB-C port in under 45 minutes.".tr()
            )

            FeatureDetailCard(
                icon = Icons.Default.Warning,
                title = "Anti-Snatch Proximity Alerts".tr(),
                description = "If your phone is snatched or you are separated from it by more than 5 meters, the wearable instantly vibrates 5 times to warn you.".tr()
            )

            FeatureDetailCard(
                icon = Icons.Default.WaterDrop,
                title = "IPX5 Water Resistant".tr(),
                description = "Built to withstand rain, sweat, and splashes. You can wear it confidently in any weather condition.".tr()
            )

            FeatureDetailCard(
                icon = Icons.Default.Vibration,
                title = "Haptic & Visual Feedback".tr(),
                description = "Features a vibration motor and LED indicator. When an SOS is successfully dispatched, it vibrates 3 times so you know help is on the way.".tr()
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD64D64)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Pricing".tr(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureDetailCard(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF160E12))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE25F71).copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(description, color = Color.White.copy(0.7f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}
