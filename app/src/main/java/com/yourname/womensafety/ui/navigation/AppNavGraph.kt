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
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.yourname.womensafety.data.IotAction
import com.yourname.womensafety.data.IotEventBus
import com.yourname.womensafety.data.SessionManager
import com.yourname.womensafety.data.network.RetrofitClient
import com.yourname.womensafety.ui.screens.*
import com.yourname.womensafety.ui.screens.AppLockScreen
import com.yourname.womensafety.utils.tr

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    var isUnlocked by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        AppLockScreen(onUnlocked = { isUnlocked = true })
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    // Sticky session-expired flag from SessionManager
    val sessionExpired by SessionManager.sessionExpired.collectAsState()
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    val iotSnackbarHostState = remember { SnackbarHostState() }

    // Watch for session expiry. Never interrupt an active SOS — the flag stays true
    // and the dialog will appear once the user navigates away from the SOS screen.
    LaunchedEffect(sessionExpired, currentRoute) {
        val safeToShow = currentRoute != null
            && currentRoute != "sos_alert"
            && currentRoute != "login"
            && currentRoute != "app_splash"
            && currentRoute != "onboarding"
            && currentRoute != "permissions"
        if (sessionExpired && safeToShow) {
            showSessionExpiredDialog = true
        }
    }

    // ── IoT wearable events ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        IotEventBus.events.collect { action ->
            when (action) {
                is IotAction.Triggered -> {
                    // Navigate to the SOS countdown screen exactly like a manual trigger.
                    // launchSingleTop prevents stacking multiple SOS screens from rapid presses.
                    navController.navigate(
                        "sos_alert?triggerType=iot_button&alertId=${action.alertId}"
                    ) { launchSingleTop = true }
                }
                is IotAction.Cancelled -> {
                    // Navigation back to the dashboard is handled by SOSAlertScreen
                    // itself via the wearableCancelled flag → onSafe().  Duplicating the
                    // navigate() call here races with the screen's own handler and can
                    // cause a spurious second navigation after the SOS screen is gone.
                    // Only show a non-intrusive confirmation snackbar.
                    iotSnackbarHostState.showSnackbar(
                        message = action.message.ifEmpty { "SOS Cancelled via wearable" },
                        duration = SnackbarDuration.Short
                    )
                }
                IotAction.Connected -> {
                    // Connection confirmed — IotViewModel already updated its state.
                    // No navigation needed here.
                }
                IotAction.Disconnected -> {
                    // Socket dropped; background service is retrying.
                    // IotViewModel already moved state to CONNECTING — nothing to do in nav.
                }
                is IotAction.ProximityUpdate -> {
                    // Distance updates are handled by IotViewModel — no navigation needed.
                }
                is IotAction.LatencyUpdate -> {
                    // Latency updates are handled by IotViewModel — no navigation needed.
                }
                is IotAction.ConnectionFailed -> {
                    // Device was off or out of range — show snackbar with the reason.
                    iotSnackbarHostState.showSnackbar(
                        message = action.reason,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    // Session-expired dialog — non-dismissable, explains what happened
    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { /* non-dismissable to prevent confusion */ },
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = "Session Expired".tr(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = "Your login session has expired. Please log in again to continue.".tr(),
                    color = Color(0xFFAAAAAA)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSessionExpiredDialog = false
                        SessionManager.clearExpiry()
                        RetrofitClient.reset()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text(text = "Log In".tr(),
                        color = Color(0xFFF22A4F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    val bottomBarScreens = listOf("dashboard", "sos_history", "contacts", "profile")

    Scaffold(
        containerColor = com.yourname.womensafety.ui.theme.BgDark,
        snackbarHost = {
            SnackbarHost(hostState = iotSnackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = com.yourname.womensafety.ui.theme.CardBg,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            if (currentRoute in bottomBarScreens) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .background(com.yourname.womensafety.ui.theme.NavBarBg)
                ) {
                    // Hair-line top separator
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        com.yourname.womensafety.ui.theme.PinkAccent.copy(0.35f),
                                        com.yourname.womensafety.ui.theme.PinkAccent.copy(0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = androidx.compose.ui.Modifier
                            .navigationBarsPadding()
                            .height(72.dp)
                    ) {
                    val items = listOf(
                        Triple("dashboard", "Home".tr(), Icons.Default.Home),
                        Triple("sos_history", "History".tr(), Icons.Default.History),
                        Triple("contacts", "Trusted".tr(), Icons.Default.Groups),
                        Triple("profile", "Profile".tr(), Icons.Default.Person)
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
                                                .background(com.yourname.womensafety.ui.theme.PinkAccent)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(26.dp),
                                        tint = if (isSelected) com.yourname.womensafety.ui.theme.PinkAccent else Color.Gray
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) com.yourname.womensafety.ui.theme.PinkAccent else Color.Gray
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
                composable("language_selection") { LanguageSelectionScreen(navController) }
                composable("onboarding") { OnboardingScreen(navController) }
                composable("permissions") { PermissionsScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable(
                    route = "verify_otp?phone={phone}",
                    arguments = listOf(navArgument("phone") { 
                        type = NavType.StringType 
                        defaultValue = ""
                    })
                ) { backStackEntry ->
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    VerifyOTPScreen(navController, phone)
                }
                composable(
                    route = "reset_password/{phone}",
                    arguments = listOf(navArgument("phone") { type = NavType.StringType })
                ) { backStackEntry ->
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    ResetPasswordScreen(navController, phone)
                }
                composable("dashboard") { DashboardScreen(navController) }
                composable("terms") { PrivacyPolicyScreen(navController) }
                composable(
                    route = "terms_and_conditions?source={source}",
                    arguments = listOf(navArgument("source") {
                        type = NavType.StringType
                        defaultValue = "login"
                    })
                ) { backStackEntry ->
                    val source = backStackEntry.arguments?.getString("source") ?: "login"
                    TermsAndConditionsScreen(navController, source)
                }
                composable("sos_history") { SOSHistoryScreen(navController) }
                composable("contacts") { TrustedContactsScreen(navController) }
                composable(
                    route = "contact_otp_verification/{contactId}/{phone}/{name}/{expiresInSeconds}",
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.StringType },
                        navArgument("phone") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("expiresInSeconds") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val expiresInSeconds = backStackEntry.arguments?.getInt("expiresInSeconds") ?: 300
                    ContactOtpVerificationScreen(
                        contactId = contactId,
                        phone = phone,
                        name = name,
                        initialExpiresInSeconds = expiresInSeconds,
                        onBack = { navController.popBackStack() },
                        onVerificationSuccess = {
                            navController.navigate("twilio_setup/$contactId/$phone/$name") {
                                popUpTo("contacts") { inclusive = false }
                            }
                        }
                    )
                }
                composable(
                    route = "twilio_setup/{contactId}/{phone}/{name}",
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.StringType },
                        navArgument("phone") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                    val phone = backStackEntry.arguments?.getString("phone") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    
                    val contactsViewModel: com.yourname.womensafety.ui.viewmodels.ContactsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.yourname.womensafety.ui.viewmodels.ContactsViewModel.Factory
                    )
                    val contacts by contactsViewModel.contacts.collectAsState()
                    val contact = contacts.find { it.id == contactId }
                    
                    TwilioSetupScreen(
                        navController = navController,
                        contactId = contactId,
                        contactPhone = phone,
                        contactName = name,
                        whatsappLink = contact?.whatsappJoinInfo?.whatsappLink,
                        sandboxCode = contact?.whatsappJoinInfo?.sandboxCode,
                        twilioNumber = contact?.whatsappJoinInfo?.twilioNumber
                    )
                }
                composable("profile") { ProfileScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("privacy_policy") { PrivacyPolicyScreen(navController) }
                composable("help") { HelpSupportScreen(navController) }
                composable("about") { AboutAppScreen(navController) }
                composable("safety_stats") { SafetyStatisticsScreen(navController) }
                composable("account_security") { AccountSecurityScreen(navController) }
                composable(
                    route = "sos_alert?triggerType={triggerType}&alertId={alertId}",
                    arguments = listOf(
                        navArgument("triggerType") {
                            type = NavType.StringType
                            defaultValue = "manual"
                        },
                        navArgument("alertId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "app://womensafety/sos_alert?triggerType={triggerType}&alertId={alertId}" }
                    )
                ) { backStackEntry ->
                    val triggerType = backStackEntry.arguments?.getString("triggerType") ?: "manual"
                    val alertId = backStackEntry.arguments?.getString("alertId")
                    SOSAlertScreen(
                        triggerType = triggerType,
                        existingAlertId = alertId,
                        onSafe = {
                            navController.popBackStack("dashboard", inclusive = false)
                        }
                    )
                }
                composable("live_map") {
                    LiveMapScreen(onBack = { navController.popBackStack() })
                }
                composable("pricing") { PricingScreen(navController) }
                composable("premium_features") { PremiumFeaturesScreen(navController) }
            }
        }
    }
}
