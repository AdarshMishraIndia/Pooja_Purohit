package com.poojapurohit.notification.compose.model

import com.google.firebase.Timestamp

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)
