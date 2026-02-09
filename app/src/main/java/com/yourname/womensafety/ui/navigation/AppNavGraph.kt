package com.yourname.womensafety.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.yourname.womensafety.ui.screens.*

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    val bottomBarScreens = listOf("dashboard", "map", "contacts", "profile")

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            if (currentRoute in bottomBarScreens) {
                NavigationBar(
                    containerColor = Color.Black,
                    tonalElevation = 0.dp,
                    // Slightly taller to accommodate bigger icons and the top bar
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(85.dp)
                ) {
                    val items = listOf(
                        Triple("dashboard", "Home", Icons.Default.Home),
                        Triple("map", "Live Map", Icons.Default.Map),
                        Triple("contacts", "Trusted", Icons.Default.Groups),
                        Triple("profile", "Profile", Icons.Default.Person)
                    )
                    items.forEach { (route, label, icon) ->
                        val isSelected = currentRoute == route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != route) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // --- FIGMA RED BAR INDICATOR ---
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                                .background(Color(0xFFE10600))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    } else {
                                        // Invisible spacer to prevent icon jump
                                        Spacer(modifier = Modifier.height(11.dp))
                                    }

                                    // --- BIGGER ICONS ---
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(28.dp), // Increased size
                                        tint = if (isSelected) Color(0xFFE10600) else Color.Gray
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFE10600) else Color.Gray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent // Removes the default pill background
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentRoute in bottomBarScreens) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = "app_splash",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable("app_splash") { AppSplashScreen(navController) }
                composable("onboarding") { OnboardingScreen(navController) }
                composable("permissions") { PermissionsScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable("sign_in_email") { SignInWithEmail(navController) }
                composable("sign_in_phone") { SignInWithPhone(navController) }
                composable("verify_otp") { VerifyOTPScreen(navController) }

                composable("dashboard") { DashboardScreen(navController) }
                composable("map") { LiveMapScreen(onBack = { navController.popBackStack() }) }
                composable("contacts") { TrustedContactsScreen(onBack = { navController.popBackStack() }) }
                composable("profile") { ProfileScreen(navController) }

                composable("settings") { SettingsScreen(navController) }
                composable("privacy_policy") { PrivacyPolicyScreen(navController) }
                composable("help") { HelpSupportScreen(navController) }
                composable("about") { AboutAppScreen(navController) }

                composable("sos_alert") {
                    SOSAlertScreen(onSafe = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}
