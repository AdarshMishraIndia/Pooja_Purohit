package com.poojapurohit.notification

import com.poojapurohit.notification.compose.model.NotificationItem

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(val notifications: List<NotificationItem>) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}
