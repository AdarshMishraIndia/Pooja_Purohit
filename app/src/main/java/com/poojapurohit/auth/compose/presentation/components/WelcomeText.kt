package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeText() {
    Text(
        text = "Welcome!",
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFB500),
        fontFamily = FontFamily.Serif,
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                spotColor = Color(0xFF620C0C),
                ambientColor = Color(0xFF620C0C)
            )
            .padding(bottom = 4.dp)
    )
}