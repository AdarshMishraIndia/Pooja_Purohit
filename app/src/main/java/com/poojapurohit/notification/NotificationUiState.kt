package com.poojapurohit.notification

import com.poojapurohit.data.model.NotificationDoc

data class NotificationGroup(
    val label: String,
    val items: List<NotificationDoc>
)

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(
        val groups: List<NotificationGroup>,
        val unreadCount: Int
    ) : NotificationUiState {
        val allItems: List<NotificationDoc> get() = groups.flatMap { it.items }
        val isEmpty: Boolean get() = groups.all { it.items.isEmpty() }
    }
    data class Error(val message: String) : NotificationUiState
}
