package com.poojapurohit.notification.compose.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientStart
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientStart
import com.poojapurohit.notification.NotificationUiState
import com.poojapurohit.notification.compose.presentation.NotificationEvent
import com.poojapurohit.notification.compose.presentation.NotificationViewModel
import com.poojapurohit.notification.compose.presentation.components.NotificationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackPressed: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    // Mark all as read when screen opens
    LaunchedEffect(uiState) {
        if (uiState is NotificationUiState.Success) {
            viewModel.onEvent(NotificationEvent.MarkAllRead)
        }
    }

    BackHandler(onBack = onBackPressed)

    Scaffold(
        topBar = {
            NotificationTopBar(
                isDark = isDark,
                onBackPressed = onBackPressed,
                hasUnread = uiState is NotificationUiState.Success &&
                        (uiState as NotificationUiState.Success).notifications.any { !it.isRead },
                onMarkAllRead = { viewModel.onEvent(NotificationEvent.MarkAllRead) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                DarkBackgroundGradientStart,
                                DarkBackgroundGradientCenter,
                                DarkBackgroundGradientEnd
                            )
                        } else {
                            listOf(
                                LightBackgroundGradientStart,
                                LightBackgroundGradientCenter,
                                LightBackgroundGradientEnd
                            )
                        },
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = if (isDark) DarkBrandOrange else BrandOrange
                    )
                }

                is NotificationUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Text(
                            text = state.message,
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        TextButton(
                            onClick = { viewModel.onEvent(NotificationEvent.LoadNotifications) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Retry",
                                fontFamily = FontFamily.Serif,
                                color = if (isDark) DarkBrandOrange else BrandRed
                            )
                        }
                    }
                }

                is NotificationUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyNotificationsState(isDark = isDark)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = state.notifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationCard(notification = notification)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    isDark: Boolean,
    onBackPressed: () -> Unit,
    hasUnread: Boolean,
    onMarkAllRead: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Notifications",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            if (hasUnread) {
                IconButton(onClick = onMarkAllRead) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all as read",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.background(
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(DarkBrandOrange, DarkBrandRed)
                } else {
                    listOf(BrandOrange, BrandRed)
                },
                start = Offset.Zero,
                end = Offset.Infinite
            )
        )
    )
}

@Composable
private fun EmptyNotificationsState(isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔔", fontSize = 56.sp)
        Text(
            text = "No Notifications",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "You're all caught up!\nNew notifications will appear here.",
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
