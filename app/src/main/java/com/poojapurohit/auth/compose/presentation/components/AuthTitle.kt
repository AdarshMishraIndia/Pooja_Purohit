package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AuthTitle() {
    val fontScale = LocalDensity.current.fontScale
    
    // NEW: Progressive font size scaling to prevent overflow
    val adaptiveFontSize = when {
        fontScale > 1.5f -> 28.sp  // XL - significant reduction
        fontScale > 1.3f -> 32.sp  // Large - moderate reduction
        fontScale > 1.15f -> 36.sp  // Medium-Large - slight reduction
        else -> 40.sp  // Normal/Small - original size
    }
    
    // NEW: Reduce padding at larger scales to save space
    val adaptivePaddingHorizontal = when {
        fontScale > 1.3f -> 6.dp
        else -> 8.dp
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),  // NEW: added horizontal padding
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = "POOJA PUROHIT",
            fontSize = adaptiveFontSize,  // CHANGED: adaptive
            fontWeight = FontWeight.Bold,
            color = Color(0xFF811C01),
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,  // NEW: center align for safety
            lineHeight = adaptiveFontSize * 1.2f,  // NEW: proportional line height
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFB500),
                            Color(0xFF811C01)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFF5A0F00),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = adaptivePaddingHorizontal, vertical = 3.dp)  // CHANGED: adaptive
        )
    }
}