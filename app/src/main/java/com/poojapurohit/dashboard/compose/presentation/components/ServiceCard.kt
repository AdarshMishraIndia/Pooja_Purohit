package com.poojapurohit.dashboard.compose.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.dashboard.ServiceItem
import com.poojapurohit.ui.theme.DarkBrandOrange
import com.poojapurohit.ui.theme.BrandOrange


@Composable
fun ServiceCard(
    service: ServiceItem,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2A2A2F) else Color(0xFFFDF6EC)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (isDark)
                    listOf(Color(0xFFCC9300).copy(alpha = 0.6f), Color(0xFF5E1100).copy(alpha = 0.4f))
                else
                    listOf(Color(0xFFFFB500).copy(alpha = 0.8f), Color(0xFF811C01).copy(alpha = 0.5f))
            )
        )
    ) {
        // Gold accent line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isDark)
                            listOf(Color(0xFFCC9300), Color(0xFF5E1100))
                        else
                            listOf(Color(0xFFFFB500), Color(0xFF811C01))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image bleeds edge-to-edge on the left with gradient fade
            Box(modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
            ) {
                Image(
                    painter = painterResource(id = service.iconResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient fade: right edge blends into card background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    1.0f to if (isDark) Color(0xFF2A2A2F) else Color(0xFFFDF6EC)
                                )
                            )
                        )
                )
            }

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = service.name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Color(0xFF3D1A00),
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = service.description,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6B3A1F).copy(alpha = 0.8f),
                    lineHeight = 17.sp
                )
            }

            // Arrow
            Box(
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(30.dp)
                    .background(
                        color = if (isDark) Color(0xFFCC9300).copy(alpha = 0.15f)
                        else Color(0xFFFFB500).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Navigate",
                    modifier = Modifier.size(16.dp),
                    tint = if (isDark) DarkBrandOrange else BrandOrange
                )
            }
        }
    }
}