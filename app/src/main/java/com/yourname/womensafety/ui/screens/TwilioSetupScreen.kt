package com.yourname.womensafety.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.utils.tr

@Composable
fun TwilioSetupScreen(
    navController: NavController,
    contactId: String,
    contactPhone: String,
    contactName: String,
    whatsappLink: String?,
    sandboxCode: String?,
    twilioNumber: String?
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse".tr())
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale".tr()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF080404), Color(0xFF120508))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Animated Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366).copy(0.15f))
                    .border(2.dp, Color(0xFF25D366).copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = "WhatsApp".tr(),
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("WhatsApp Setup Required".tr(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text("To send emergency SOS alerts to ".tr() + contactName + " via WhatsApp, they must join the Asfalis Sandbox first.".tr(),
                color = Color.Gray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // Instructions Card
            Surface(
                color = Color.White.copy(0.03f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFE25F71)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1".tr(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Save Twilio Number".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Ask ".tr() + contactName + " to save this number in their contacts:".tr(),
                                color = Color.Gray, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("+1 (415) 523-8886".tr(),
                                color = Color(0xFF25D366),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFE25F71)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2".tr(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Send Join Message".tr(), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Ask them to send this exact WhatsApp message to +1 (415) 523-8886:".tr(),
                                color = Color.Gray, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = Color.Black.copy(0.3f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(0.3f))
                            ) {
                                Text("join something-popular".tr(),
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            var userName by remember { mutableStateOf("A user") }
            val userRepository = com.yourname.womensafety.data.AppServiceLocator.userRepository
            LaunchedEffect(Unit) {
                // Try from API/Cache through repository
                val res = userRepository.getProfile()
                if (res is com.yourname.womensafety.data.repository.NetworkResult.Success) {
                    userName = res.data?.fullName ?: "A user"
                } else {
                    val name = com.yourname.womensafety.data.local.AppCache(context).getUserName()
                    if (!name.isNullOrBlank()) {
                        userName = name
                    }
                }
            }

            val inviteText = buildString {
                appendLine("✅ $userName " + "added you as a trusted contact in Asfalis, a personal safety app. You will receive emergency alerts with their location if they trigger an SOS.".tr())
                appendLine()
                appendLine("📱 " + "To receive WhatsApp alerts:".tr())
                appendLine("1. " + "Save this number:".tr() + " +14155238886")
                appendLine("2. " + "Send this message on WhatsApp:".tr() + " join something-popular")
                appendLine()
                appendLine("Quick link:".tr() + " https://wa.me/14155238886?text=join%20something-popular")
                appendLine()
                appendLine("(Note: You must send the join code first to enable WhatsApp alerts)".tr())
            }

            Button(
                onClick = {
                    val shareIntent = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, inviteText)
                        },
                        "Send Instructions to ${contactName}"
                    )
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Share, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Share Instructions".tr(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Done, I've shared it".tr(), color = Color.White.copy(0.7f), fontSize = 16.sp)
            }
        }
    }
}
