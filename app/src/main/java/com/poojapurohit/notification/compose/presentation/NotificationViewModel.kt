package com.poojapurohit.notification.compose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poojapurohit.notification.NotificationRepository
import com.poojapurohit.notification.NotificationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationEvent {
    data object LoadNotifications : NotificationEvent
    data object MarkAllRead : NotificationEvent
}

sealed interface NotificationEffect {
    data object NavigateBack : NotificationEffect
}

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<NotificationEffect?>(null)
    val effect: StateFlow<NotificationEffect?> = _effect.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        onEvent(NotificationEvent.LoadNotifications)
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.LoadNotifications -> loadNotifications()
            is NotificationEvent.MarkAllRead -> markAllAsRead()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun loadNotifications() {
        _uiState.value = NotificationUiState.Loading
        viewModelScope.launch {
            repository.fetchNotifications().fold(
                onSuccess = { items ->
                    _uiState.value = NotificationUiState.Success(items)
                    _unreadCount.value = items.count { !it.isRead }
                },
                onFailure = { e ->
                    _uiState.value = NotificationUiState.Error(
                        e.message ?: "Failed to load notifications"
                    )
                }
            )
        }
    }

    private fun markAllAsRead() {
        val current = _uiState.value
        if (current !is NotificationUiState.Success) return

        val unreadIds = current.notifications
            .filter { !it.isRead }
            .map { it.id }

        if (unreadIds.isEmpty()) return

        // Optimistically update UI
        _uiState.value = NotificationUiState.Success(
            current.notifications.map { it.copy(isRead = true) }
        )
        _unreadCount.value = 0

        viewModelScope.launch {
            repository.markAllAsRead(unreadIds)
        }
    }
}
