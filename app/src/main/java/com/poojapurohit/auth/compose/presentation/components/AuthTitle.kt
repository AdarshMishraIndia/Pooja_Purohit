package com.poojapurohit.auth.compose.presentation.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AuthTitle() {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    // Calculate available width
    val availableWidth = with(density) {
        (screenWidth - 16.dp - 16.dp - 4.dp).toPx()
    }

    val textLength = "POOJA PUROHIT".length
    val estimatedCharWidth = 0.65f

    // Calculate font size and convert to TextUnit that ignores fontScale
    val calculatedFontSize = (availableWidth / (textLength * estimatedCharWidth)) / density.density
    val fixedFontSize = TextUnit(minOf(calculatedFontSize, 36f), androidx.compose.ui.unit.TextUnitType.Sp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "POOJA PUROHIT",
                fontSize = fixedFontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF811C01),
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.White,
                        offset = Offset(0f, 0f),
                        blurRadius = 3f
                    ),
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}