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

/**
 * Terms & Conditions screen.
 *
 * @param navController  Standard nav controller.
 * @param source         Either "login" or "register" — used to label the back-button correctly.
 */
@Composable
fun TermsAndConditionsScreen(
    navController: NavController,
    source: String = "login"
) {
    val bg = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))
    )

    val backLabel = if (source == "register") "Back to Register" else "Back to Login"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Background image with gradient blend
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            alpha = 0.35f,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        )
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
            // ── Sticky Header ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF000000), Color(0xFF000000).copy(0.85f), Color.Transparent)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE25F71).copy(0.25f))
                            .border(1.dp, Color(0xFFE25F71).copy(0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Terms & Conditions".tr(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Last Updated: June 2026".tr(), color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // ── Scrollable Content ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    "By creating an account or using the ASFALIS app, you agree to the following terms. Please read them carefully.".tr(),
                    color = Color.White.copy(0.8f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                TermsSectionCard(
                    icon = Icons.Default.VerifiedUser,
                    title = "1. Acceptance of Terms".tr(),
                    content = "By registering and using the Asfalis application, you agree to be bound by these Terms & Conditions. If you do not agree, please do not use the app.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.PersonOutline,
                    title = "2. User Eligibility".tr(),
                    content = "You must be at least 13 years of age to use Asfalis. By registering, you confirm that you meet this age requirement and that all information you provide is accurate and complete.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.PhonelinkLock,
                    title = "3. Account Responsibility".tr(),
                    content = "You are solely responsible for maintaining the confidentiality of your account credentials. You agree to notify Asfalis immediately if you suspect any unauthorized use of your account. Asfalis is not liable for any loss resulting from unauthorized access due to your failure to secure your credentials.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "4. SOS Alert Usage".tr(),
                    content = "The SOS feature is intended strictly for genuine emergency situations. Misuse of the SOS system, including false alerts, is prohibited and may result in account suspension. You are responsible for ensuring your trusted contacts are willing to receive emergency notifications.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.DataUsage,
                    title = "5. Data Collection & Use".tr(),
                    content = "Asfalis collects minimal data required to operate the safety features of the app, including location (only during active SOS alerts), device motion data (processed locally), and your emergency contacts. This data is used exclusively to provide emergency assistance. Please see our Privacy Policy for full details.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.LocationOff,
                    title = "6. Location Services".tr(),
                    content = "You grant Asfalis permission to access your device location during SOS alerts. Location data is shared with your trusted contacts to facilitate emergency assistance. You may revoke this permission at any time via your device settings, though this will limit the app's core functionality.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.Block,
                    title = "7. Prohibited Conduct".tr(),
                    content = "You may not: (a) use the app for any unlawful purpose; (b) send false emergency alerts; (c) attempt to gain unauthorized access to any part of our service; (d) distribute malicious code; or (e) impersonate any person or entity. Violations may result in immediate account termination.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.Update,
                    title = "8. Changes to Terms".tr(),
                    content = "Asfalis reserves the right to modify these Terms at any time. Continued use of the app after any changes constitutes your acceptance of the updated Terms. We will notify users of significant changes via in-app notification or email.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.GppMaybe,
                    title = "9. Limitation of Liability".tr(),
                    content = "Asfalis provides this service on an 'as-is' basis. While we strive for 100% uptime, we cannot guarantee uninterrupted service. Asfalis shall not be liable for any indirect, incidental, or consequential damages arising from your use of the app, including any delays in emergency response.".tr()
                )

                TermsSectionCard(
                    icon = Icons.Default.Gavel,
                    title = "10. Governing Law".tr(),
                    content = "These Terms shall be governed by and construed in accordance with the laws of India. Any disputes arising under these Terms shall be subject to the exclusive jurisdiction of the courts located in India.".tr()
                )

                Spacer(Modifier.height(16.dp))

                // Contact / acceptance footer
                Surface(
                    color = Color(0xFF2A161A).copy(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE25F71).copy(0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MailOutline,
                            null,
                            tint = Color(0xFFE25F71),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Questions about our terms?".tr(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "fyear2022.26@gmail.com".tr(),
                            color = Color(0xFFE25F71),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Back-to-Login/Register contextual button at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        color = Color(0xFFE25F71),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                backLabel.tr(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun TermsSectionCard(icon: ImageVector, title: String, content: String) {
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
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE25F71).copy(0.25f)),
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
