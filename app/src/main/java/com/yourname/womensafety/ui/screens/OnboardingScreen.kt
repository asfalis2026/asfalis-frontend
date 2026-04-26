package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val accentColor: Color,
    val tag: String,
    val title: String,
    val subtitle: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Shield,
        accentColor = Color(0xFFE10600),
        tag = "ALWAYS PROTECTED",
        title = "Your Safety,\nAlways On",
        subtitle = "Real-time protection that works even when you can't speak.",
        description = "Asfalis monitors your safety 24/7 with intelligent background services that never sleep."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Sensors,
        accentColor = Color(0xFFFF6B00),
        tag = "AI-POWERED",
        title = "Auto SOS\nvia Machine Learning",
        subtitle = "Falls and impacts detected instantly by on-device AI.",
        description = "Our lightweight LightGBM model runs entirely on your device — no internet needed to detect danger."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Watch,
        accentColor = Color(0xFFE10600),
        tag = "HARDWARE SYNC",
        title = "ESP32 Bracelet\nIntegration",
        subtitle = "One press on your wearable = immediate SOS trigger.",
        description = "Your bracelet connects via Bluetooth. Press once to trigger SOS, double-press to cancel. Proximity alerts if separated."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Group,
        accentColor = Color(0xFF00B0FF),
        tag = "TRUSTED NETWORK",
        title = "Your People\nGet Notified",
        subtitle = "WhatsApp + SMS alerts reach your trusted contacts instantly.",
        description = "Add verified contacts who receive your live location and SOS alert within seconds of triggering."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == onboardingPages.lastIndex

    val currentAccent = onboardingPages[currentPage].accentColor

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060000), Color(0xFF120000), Color.Black)))
    ) {
        // Dynamic radial glow that follows accent colour
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(currentAccent.copy(alpha = pulseAlpha * 0.8f), Color.Transparent),
                        radius = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button top-right
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                com.yourname.womensafety.data.AppServiceLocator.tokenManager.setOnboardingComplete()
                            }
                            navController.navigate("permissions") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Skip", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    pulseScale = pulseScale
                )
            }

            // Page indicators
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) currentAccent else Color.White.copy(0.2f))
                    )
                }
            }

            // CTA Button
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isLastPage) {
                            scope.launch {
                                com.yourname.womensafety.data.AppServiceLocator.tokenManager.setOnboardingComplete()
                            }
                            navController.navigate("permissions") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(currentAccent, currentAccent.copy(0.75f))
                                ),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLastPage) "GET STARTED" else "NEXT",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            if (!isLastPage) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pulseScale: Float) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Box(
                modifier = Modifier
                    .size(160.dp * pulseScale)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(page.accentColor.copy(0.2f), page.accentColor.copy(0.06f))
                        )
                    )
                    .graphicsLayer {
                        shadowElevation = 32.dp.toPx()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.accentColor,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Tag chip
        Surface(
            color = page.accentColor.copy(0.12f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, page.accentColor.copy(0.3f))
        ) {
            Text(
                text = page.tag,
                color = page.accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.subtitle,
            color = Color.White.copy(0.75f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.description,
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE10600).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFE10600), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.Gray, fontSize = 13.sp)
        }
    }
}