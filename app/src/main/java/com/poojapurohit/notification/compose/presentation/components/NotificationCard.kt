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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.ui.theme.DarkSurface
import com.poojapurohit.notification.compose.model.NotificationItem
import com.poojapurohit.notification.compose.model.NotificationTemplateRegistry
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

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismiss(notification.id)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

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
    // Single registry lookup — all display decisions come from here
    val template = remember(notification.type) {
        NotificationTemplateRegistry.get(notification.type)
    }
    val accentColor = if (isDark) template.accentDark else template.accentLight

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
            .then(
                // Only attach clickable if actionable or unread (mark-read on tap)
                if (template.isActionable || !notification.isRead) {
                    Modifier.clickable { onTap(notification) }
                } else {
                    Modifier.clickable { onTap(notification) } // always tappable for mark-read
                }
            ),
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
            // Type icon — color comes from template, not hardcoded
            Icon(
                imageVector = template.icon,
                contentDescription = template.category.label,
                tint = accentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Category chip — tiny label above title
                Text(
                    text = template.category.label.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = notification.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color.Black,
                    lineHeight = 20.sp
                )

                if (notification.body.isNotBlank()) {
                    Text(
                        text = notification.body,
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = formattedTime,
                    fontFamily = FontFamily.Serif,
                    fontSize = 11.sp,
                    color = accentColor.copy(alpha = 0.75f)
                )
            }

            // Unread dot — always brand-accented per type
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp)
                        .size(8.dp)
                        .background(color = accentColor, shape = CircleShape)
                )
            }
        }
    }
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
