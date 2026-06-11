package com.yourname.womensafety.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    rightIcon: ImageVector? = null,
    onRightIconClick: (() -> Unit)? = null,
    rightIconTint: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Title
        Text(
            text = title, 
            color = Color.White, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        // Optional Right Icon
        if (rightIcon != null && onRightIconClick != null) {
            IconButton(onClick = onRightIconClick) {
                Icon(rightIcon, contentDescription = null, tint = rightIconTint, modifier = Modifier.size(24.dp))
            }
        } else {
            Spacer(Modifier.width(48.dp)) // To keep title left-aligned but balanced if needed
        }
    }
}
