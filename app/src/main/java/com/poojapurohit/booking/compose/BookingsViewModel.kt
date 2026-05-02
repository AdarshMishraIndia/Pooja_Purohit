package com.poojapurohit.booking.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.poojapurohit.booking.data.BookingsRepository
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingCategory
import com.poojapurohit.booking.model.BookingStatus
import com.poojapurohit.booking.model.category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingsUiState(
    val activeBookings: List<Booking> = emptyList(),
    val cancelledBookings: List<Booking> = emptyList(),
    val completedBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val highlightedBookingId: String? = null,
    val requestedTabIndex: Int = 0
)

sealed interface BookingsEffect {
    data class ShowToast(val message: String) : BookingsEffect
    data class ShowSnackbar(val message: String) : BookingsEffect
}

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val repository: BookingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BookingsEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<BookingsEffect> = _effect.asSharedFlow()

    private val userBookings = MutableStateFlow<List<Booking>>(emptyList())
    private val purohitBookings = MutableStateFlow<List<Booking>>(emptyList())

    init {
        observeUserBookings()
        observePurohitBookings()
        observeMerged()
    }

    private fun observeUserBookings() {
        viewModelScope.launch {
            repository.observeUserBookings().collect { result ->
                result
                    .onSuccess { list -> userBookings.value = list }
                    .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            }
        }
    }

    private fun observePurohitBookings() {
        viewModelScope.launch {
            repository.observePurohitBookings().collect { result ->
                result
                    .onSuccess { list -> purohitBookings.value = list }
                    .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            }
        }
    }

    private fun observeMerged() {
        viewModelScope.launch {
            combine(userBookings, purohitBookings) { fromUser, fromPurohit ->
                (fromUser + fromPurohit)
                    .distinctBy { it.bookingId }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }.collect { merged ->
                val grouped = merged.groupBy { it.status.category }
                _uiState.update { state ->
                    state.copy(
                        activeBookings = grouped[BookingCategory.ACTIVE] ?: emptyList(),
                        cancelledBookings = grouped[BookingCategory.CANCELLED] ?: emptyList(),
                        completedBookings = grouped[BookingCategory.COMPLETED] ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    // ── Deep link ─────────────────────────────────────────────────────────────

    fun handleDeepLink(bookingId: String) {
        if (bookingId.isBlank()) return
        val current = _uiState.value
        val tabIndex = when {
            current.activeBookings.any { it.bookingId == bookingId } -> 0
            current.cancelledBookings.any { it.bookingId == bookingId } -> 1
            current.completedBookings.any { it.bookingId == bookingId } -> 2
            else -> 0
        }
        _uiState.update { it.copy(highlightedBookingId = bookingId, requestedTabIndex = tabIndex) }
    }

    fun clearHighlight() {
        _uiState.update { it.copy(highlightedBookingId = null) }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun acceptBooking(booking: Booking) = updateStatus(booking, BookingStatus.ACCEPTED, "Booking accepted")
    fun rejectBooking(booking: Booking) = updateStatus(booking, BookingStatus.REJECTED, "Booking rejected")
    fun cancelBooking(booking: Booking) = updateStatus(booking, BookingStatus.CANCELLED, "Booking cancelled")
    fun completeBooking(booking: Booking) = updateStatus(booking, BookingStatus.COMPLETED, "Booking marked as completed")

    fun processPaymentStub(booking: Booking, isSuccess: Boolean) {
        if (isSuccess) {
            updateStatus(booking, BookingStatus.PAYMENT_DONE, "Payment successful!")
        } else {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Payment failed or cancelled."))
            }
        }
    }

    private fun updateStatus(booking: Booking, newStatus: BookingStatus, successMessage: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(booking.bookingId, newStatus)
                .onSuccess {
                    applyLocalUpdate(booking.copy(status = newStatus, updatedAt = Timestamp.now()))
                    _effect.emit(BookingsEffect.ShowSnackbar(successMessage))
                }
                .onFailure { e ->
                    _effect.emit(BookingsEffect.ShowSnackbar(e.message ?: "Action failed. Please try again."))
                }
        }
    }

    private fun applyLocalUpdate(updated: Booking) {
        userBookings.update { list -> list.map { if (it.bookingId == updated.bookingId) updated else it } }
        purohitBookings.update { list -> list.map { if (it.bookingId == updated.bookingId) updated else it } }
    }

    // ── Role ──────────────────────────────────────────────────────────────────

    fun currentUserIsPurohitFor(booking: Booking): Boolean {
        val uid = repository.currentUserId() ?: return false
        return uid == booking.purohitId
    }
}