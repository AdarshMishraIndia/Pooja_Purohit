package com.poojapurohit.notification.compose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poojapurohit.notification.NotificationGroup
import com.poojapurohit.notification.NotificationRepository
import com.poojapurohit.notification.NotificationUiState
import com.poojapurohit.notification.compose.model.NotificationItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

sealed interface NotificationEvent {
    data object MarkAllRead : NotificationEvent
    data class MarkOneRead(val id: String) : NotificationEvent
    data class DeleteNotification(val id: String) : NotificationEvent
    data object DeleteAll : NotificationEvent
    data class NavigateDeepLink(val url: String) : NotificationEvent
}

sealed interface NotificationEffect {
    data object NavigateBack : NotificationEffect
    data class OpenDeepLink(val url: String) : NotificationEffect
    data class ShowSnackbar(val message: String) : NotificationEffect
}

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // SharedFlow so effects are consumed once and not replayed
    private val _effect = MutableSharedFlow<NotificationEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<NotificationEffect> = _effect.asSharedFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        repository.observeNotifications()
            .onEach { result ->
                result.fold(
                    onSuccess = { items ->
                        _uiState.value = NotificationUiState.Success(
                            groups = groupByDate(items),
                            unreadCount = items.count { !it.isRead }
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = NotificationUiState.Error(
                            e.message ?: "Failed to load notifications"
                        )
                    }
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.MarkAllRead -> markAllAsRead()
            is NotificationEvent.MarkOneRead -> markOneAsRead(event.id)
            is NotificationEvent.DeleteNotification -> deleteNotification(event.id)
            is NotificationEvent.DeleteAll -> deleteAll()
            is NotificationEvent.NavigateDeepLink -> {
                viewModelScope.launch {
                    _effect.emit(NotificationEffect.OpenDeepLink(event.url))
                }
            }
        }
    }

    private fun markOneAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id).onFailure {
                _effect.emit(NotificationEffect.ShowSnackbar("Failed to mark as read"))
            }
        }
    }

    private fun markAllAsRead() {
        val current = _uiState.value as? NotificationUiState.Success ?: return
        val unreadIds = current.allItems.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return

        viewModelScope.launch {
            repository.markAllAsRead(unreadIds).onFailure {
                _effect.emit(NotificationEffect.ShowSnackbar("Failed to mark all as read"))
            }
        }
    }

    private fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id).onFailure {
                _effect.emit(NotificationEffect.ShowSnackbar("Failed to delete notification"))
            }
        }
    }

    private fun deleteAll() {
        val current = _uiState.value as? NotificationUiState.Success ?: return
        val ids = current.allItems.map { it.id }
        if (ids.isEmpty()) return

        viewModelScope.launch {
            repository.deleteAllNotifications(ids).onFailure {
                _effect.emit(NotificationEffect.ShowSnackbar("Failed to clear notifications"))
            }
        }
    }

    /**
     * Groups notifications into Today / Yesterday / Older sections.
     * Firestore snapshot listener already returns items sorted by timestamp DESC,
     * so order within each group is preserved.
     */
    private fun groupByDate(items: List<NotificationItem>): List<NotificationGroup> {
        val today = startOfDay(0)
        val yesterday = startOfDay(-1)

        val todayItems = items.filter { it.timestamp.toDate() >= today }
        val yesterdayItems = items.filter {
            it.timestamp.toDate() in yesterday..<today
        }
        val olderItems = items.filter { it.timestamp.toDate() < yesterday }

        return buildList {
            if (todayItems.isNotEmpty()) add(NotificationGroup("Today", todayItems))
            if (yesterdayItems.isNotEmpty()) add(NotificationGroup("Yesterday", yesterdayItems))
            if (olderItems.isNotEmpty()) add(NotificationGroup("Older", olderItems))
        }
    }

    private fun startOfDay(offsetDays: Int): Date {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
