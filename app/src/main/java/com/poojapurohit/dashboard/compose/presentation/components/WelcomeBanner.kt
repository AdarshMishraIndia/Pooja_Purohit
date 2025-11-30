package com.poojapurohit.dashboard.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.dashboard.compose.theme.*

@Composable
fun WelcomeBanner(userName: String) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(DarkWelcomeBannerStart, DarkWelcomeBannerEnd)
                    } else {
                        listOf(WelcomeBannerStart, WelcomeBannerEnd)
                    }
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome, $userName!",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (isDark) DarkBrandOrange else BrandOrange,
            textAlign = TextAlign.Center
        )
    }
}