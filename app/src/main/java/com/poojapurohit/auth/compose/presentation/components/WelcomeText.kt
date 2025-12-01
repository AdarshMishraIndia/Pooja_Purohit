package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeText() {
    val fontScale = LocalDensity.current.fontScale
    
    // NEW: Progressive font size reduction at larger scales
    val adaptiveFontSize = when {
        fontScale > 1.5f -> 24.sp  // XL accessibility
        fontScale > 1.3f -> 26.sp  // Large
        fontScale > 1.15f -> 28.sp  // Medium-Large
        else -> 30.sp  // Normal/Small - original size
    }
    
    Text(
        text = "Welcome!",
        fontSize = adaptiveFontSize,  // CHANGED: adaptive
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFB500),
        fontFamily = FontFamily.Serif,
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                spotColor = Color(0xFF620C0C),
                ambientColor = Color(0xFF620C0C)
            )
            .padding(bottom = 4.dp),
        lineHeight = adaptiveFontSize * 1.2f  // NEW: proportional line height
    )
}