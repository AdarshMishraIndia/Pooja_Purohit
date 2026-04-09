package com.poojapurohit.notification.compose.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface
import com.poojapurohit.notification.compose.model.NotificationItem
import com.poojapurohit.notification.compose.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onTap: (NotificationItem) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Fixed: Using the non-deprecated rememberSwipeToDismissBoxState constructor
    val dismissState = rememberSwipeToDismissBoxState()

    // Handle the dismissal logic reactively instead of using the deprecated confirmValueChange
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismiss(notification.id)
            // Optional: reset the state if you aren't immediately removing the item from the list
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    // Reset state if the item changes (ID based) to prevent UI glitches during list updates
    LaunchedEffect(notification.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(
                        color = Color(0xFFD32F2F),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        NotificationCardContent(
            notification = notification,
            isDark = isDark,
            onTap = onTap
        )
    }
}

@Composable
private fun NotificationCardContent(
    notification: NotificationItem,
    isDark: Boolean,
    onTap: (NotificationItem) -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (notification.isRead) {
            if (isDark) DarkSurface else Color(0xFFF5EDE2)
        } else {
            if (isDark) Color(0xFF3A2A10) else Color(0xFFFFF8E7)
        },
        animationSpec = tween(300),
        label = "card_color"
    )

    val formattedTime = remember(notification.timestamp) {
        formatTimestamp(notification.timestamp.toDate())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onTap(notification) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 2.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = notification.type.icon(),
                contentDescription = notification.type.name,
                tint = if (isDark) DarkBrandOrange else BrandOrange,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color.Black,
                    lineHeight = 20.sp
                )

                if (notification.body.isNotBlank()) {
                    Text(
                        text = notification.body,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = formattedTime,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp,
                    color = if (isDark) DarkBrandOrange.copy(alpha = 0.8f)
                    else BrandRed.copy(alpha = 0.7f)
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp)
                        .size(8.dp)
                        .background(
                            color = if (isDark) DarkBrandOrange else BrandOrange,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.ORDER_UPDATE -> Icons.Default.LocalShipping
    NotificationType.PROMO -> Icons.Default.Campaign
    NotificationType.ALERT -> Icons.Default.Warning
    NotificationType.GENERAL -> Icons.Default.Notifications
}

private fun formatTimestamp(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time

    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    }
}