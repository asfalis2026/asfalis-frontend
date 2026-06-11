package com.yourname.womensafety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.yourname.womensafety.utils.tr
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LanguageSelectionScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf("English") }
    val context = LocalContext.current
    val languages = listOf("English".tr(), "Bengali", "Hindi")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(com.yourname.womensafety.ui.theme.BgDark, com.yourname.womensafety.ui.theme.BgMid, com.yourname.womensafety.ui.theme.BgEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Language".tr(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You can change this later in Settings".tr(),
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            languages.forEach { language ->
                LanguageOption(
                    language = language,
                    isSelected = selectedLanguage == language,
                    onClick = { selectedLanguage = language }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        // Apply the locale change at the app level
                        val code = LocaleHelper.getCodeFromLanguage(selectedLanguage)
                        LocaleHelper.setLocale(context, code)
                        
                        AppServiceLocator.tokenManager.setLanguageSelected()
                        navController.navigate("onboarding") {
                            popUpTo("language_selection") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE25F71))
            ) {
                Text(
                    text = "Continue".tr(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFE25F71).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = language,
            color = if (isSelected) Color(0xFFE25F71) else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFE25F71),
                unselectedColor = Color.Gray
            )
        )
    }
}
