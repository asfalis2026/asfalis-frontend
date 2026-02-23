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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yourname.womensafety.ui.screens.*

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    val bottomBarScreens = listOf("dashboard", "sos_history", "contacts", "profile")

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            if (currentRoute in bottomBarScreens) {
                NavigationBar(
                    containerColor = Color.Black,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(80.dp)
                ) {
                    val items = listOf(
                        Triple("dashboard", "Home", Icons.Default.Home),
                        Triple("sos_history", "History", Icons.Default.History),
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
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFE10600))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(26.dp),
                                        tint = if (isSelected) Color(0xFFE10600) else Color.Gray
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFE10600) else Color.Gray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
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
                exitTransition = { ExitTransition.None }
            ) {
                composable("app_splash") { AppSplashScreen(navController) }
                composable("onboarding") { OnboardingScreen(navController) }
                composable("permissions") { PermissionsScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable(
                    route = "verify_otp/{email}",
                    arguments = listOf(navArgument("email") { type = NavType.StringType })
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    VerifyOTPScreen(navController, email)
                }
                composable("dashboard") { DashboardScreen(navController) }
                composable("sos_history") { SOSHistoryScreen(navController) }
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