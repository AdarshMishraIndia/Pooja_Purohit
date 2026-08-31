// ─── REPLACE ENTIRE FILE CONTENT ─────────────────────────────────────────────

package com.poojapurohit.dashboard.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.ui.theme.BrandOrange
import com.poojapurohit.ui.theme.BrandRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int = 0
) {
    // Outer Box owns the gradient and stretches edge-to-edge including
    // behind the status bar. TopAppBar inside zeroes out its own insets
    // so it doesn't double-consume the status bar height, then we add
    // statusBarsPadding() on the Bar itself to push content below the bar.
    Box(
        modifier = Modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BrandOrange, BrandRed),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        TopAppBar(
            // Zero internal insets; outer Box + statusBarsPadding owns the top space
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier     = Modifier
                .statusBarsPadding()
                .height(56.dp),
            title = {
                Text(
                    text = "POOJA PUROHIT (ପୂଜା ପୁରୋହିତ)",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Badge box wraps the icon button so the badge offsets relative to the icon
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = (-6).dp, y = 6.dp)
                                .size(18.dp)
                                .background(color = Color(0xFFD32F2F), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 99) "99+"
                                else unreadNotificationCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent  // gradient from outer Box shows through
            )
        )
    }
}