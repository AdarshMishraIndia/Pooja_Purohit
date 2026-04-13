package com.poojapurohit.booking.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poojapurohit.booking.data.BookingRepository
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingCategory
import com.poojapurohit.booking.model.category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingsUiState(
    val activeBookings: List<Booking> = emptyList(),
    val cancelledBookings: List<Booking> = emptyList(),
    val completedBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface BookingsEffect {
    data class ShowToast(val message: String) : BookingsEffect
}

class BookingsViewModel(
    private val repository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<BookingsEffect?>(null)
    val effect: StateFlow<BookingsEffect?> = _effect.asStateFlow()

    init {
        observeBookings()
    }

    private fun observeBookings() {
        viewModelScope.launch {
            repository.observeUserBookings().collect { result ->
                result.onSuccess { bookings ->
                    val grouped = bookings.groupBy { it.status.category }
                    _uiState.update {
                        it.copy(
                            activeBookings = grouped[BookingCategory.ACTIVE] ?: emptyList(),
                            cancelledBookings = grouped[BookingCategory.CANCELLED] ?: emptyList(),
                            completedBookings = grouped[BookingCategory.COMPLETED] ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            }
        }
    }

    fun clearEffect() {
        _effect.value = null
    }
}
