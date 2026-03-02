package com.yourname.womensafety.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourname.womensafety.R
import com.yourname.womensafety.ui.viewmodels.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun AppSplashScreen(navController: NavController) {
    val splashViewModel: SplashViewModel = viewModel(factory = SplashViewModel.Factory)
    val destination by splashViewModel.destination.collectAsState()
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(key1 = true) {
        // Logo animation
        scale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 800))
        delay(1600)
        // Resolve navigation destination via ViewModel (DataStore + API)
        splashViewModel.resolveStartDestination()
    }

    // Navigate once destination is resolved
    LaunchedEffect(destination.route) {
        destination.route?.let { route ->
            navController.navigate(route) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color.Black, Color(0xFF1A0000), Color.Black))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Asfalis Logo",
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "ASFALIS",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )

            Text(
                text = "Your Safety, Our Priority",
                color = Color(0xFFE10600).copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}