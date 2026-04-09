package com.poojapurohit.notification.compose.model

import com.google.firebase.Timestamp

enum class NotificationType {
    ORDER_UPDATE,
    PROMO,
    ALERT,
    GENERAL;

    companion object {
        fun fromString(value: String?): NotificationType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.GENERAL,
    val deepLinkUrl: String? = null
)
