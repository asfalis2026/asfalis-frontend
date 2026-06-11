package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.R
import com.yourname.womensafety.utils.tr

@Composable
fun PricingScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Background Gradient matching the theme
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF0A0404), Color(0xFF14080A))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // --- Header Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                // Background Illustration
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.65f)
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp, y = (-20).dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pricing_image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Left blending
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            0.0f to Color.Black,
                            0.4f to Color.Transparent
                        )
                    ))
                    // Bottom blending
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.7f to Color.Transparent,
                            1.0f to Color.Black
                        )
                    ))
                    // Top blending
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.2f to Color.Transparent
                        )
                    ))
                }

                // Content Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
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

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Choose Your\nProtection".tr(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Pick the plan that keeps you safe, always.".tr(),
                        color = Color.White.copy(0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            // --- Premium Plan Card ---
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF160E12))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE25F71).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    // Recommended Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD64D64))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RECOMMENDED".tr(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE25F71).copy(0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Asfalis Premium".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Complete protection for you".tr(), color = Color(0xFFE25F71).copy(0.9f), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹149".tr(), color = Color(0xFFE25F71), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text(" /month".tr(), color = Color.White.copy(0.7f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel anytime".tr(), color = Color.White.copy(0.7f), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(0.05f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Features Grid
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItem(
                                "Real-time live location tracking".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            FeatureItem(
                                "Priority alert routing & reduced latency".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItem(
                                "Multi-contact escalation\n(up to 10 contacts)".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            FeatureItem(
                                "24/7 Helpline integration".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItem(
                                "Completely ad-free experience".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Activate Button
                    Button(
                        onClick = { 
                            navController.navigate("dashboard") {
                                popUpTo(0) { inclusive = true }
                            } 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD64D64)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activate Premium".tr(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Secure payments. Your safety is our priority.".tr(), color = Color.White.copy(0.5f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Hardware Plan Card ---
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131313))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE25F71).copy(0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Watch,
                                contentDescription = "Smart Wearable",
                                tint = Color(0xFFE25F71),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Asfalis Smart Wearable".tr(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("The hardware foundation".tr(), color = Color(0xFFE25F71).copy(0.9f), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E3A20))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ONE-TIME PURCHASE".tr(), color = Color(0xFF4CAF50), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹1,999".tr(), color = Color(0xFFE25F71), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text(" – ₹2,499".tr(), color = Color.White.copy(0.7f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay once. Use for years.".tr(), color = Color.White.copy(0.7f), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(0.05f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Features Grid
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItemAlt(
                                "Bluetooth 5.0\nLow Energy".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            FeatureItemAlt(
                                "Battery life\nup to 7 days".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItemAlt(
                                "Tactile SOS\nButton".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            FeatureItemAlt(
                                "Water resistant\n(IPX5)".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FeatureItemAlt(
                                "Vibration feedback\n& LED confirmation".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                            FeatureItemAlt(
                                "Anti-search proximity\nalerts".tr(), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Learn More Button
                    Button(
                        onClick = { navController.navigate("premium_features") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFFE25F71).copy(0.4f), RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Learn More".tr(), color = Color(0xFFE25F71), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Enterprise & Campus Safety Card ---
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF110D16))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF8B5CF6).copy(0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Enterprise & Campus Safety".tr(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("For Corporate Night Shifts & Universities".tr(), color = Color(0xFF8B5CF6).copy(0.9f), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Features List
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureItemEnterprise("Bulk hardware procurement".tr())
                        FeatureItemEnterprise("Centralized safety dashboard for admins".tr())
                        FeatureItemEnterprise("Annual billing per employee/student".tr())
                        FeatureItemEnterprise("Dedicated support & training".tr())
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Request Demo Button
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:fyear2022.26@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Demo Request for Enterprise & Campus Safety")
                                putExtra(android.content.Intent.EXTRA_TEXT, "Hello Asfalis Team,\n\nWe are interested in a demo of your Enterprise & Campus Safety solutions. Please contact us to schedule a discussion.\n\nThank you.")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFF8B5CF6).copy(0.4f), RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Request Demo".tr(), color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.width(40.dp), color = Color.White.copy(0.1f))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your security is our priority".tr(), color = Color.White.copy(0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(modifier = Modifier.width(40.dp), color = Color.White.copy(0.1f))
            }
        }
    }
}

@Composable
fun FeatureItem(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFFE25F71)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White.copy(0.9f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun FeatureItemAlt(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFE25F71), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE25F71), modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White.copy(0.8f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun FeatureItemEnterprise(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF8B5CF6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White.copy(0.8f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
