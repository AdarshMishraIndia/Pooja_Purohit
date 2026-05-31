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
    val activeBookings    : List<Booking> = emptyList(),
    val cancelledBookings : List<Booking> = emptyList(),
    val completedBookings : List<Booking> = emptyList(),
    val isLoading         : Boolean       = true,
    val error             : String?       = null,
    val highlightedBookingId: String?     = null,
    val requestedTabIndex : Int           = 0
)

sealed interface BookingsEffect {
    data class ShowToast   (val message: String) : BookingsEffect
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

    private val userBookings    = MutableStateFlow<List<Booking>>(emptyList())
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
                    .onFailure { e   -> _uiState.update { it.copy(error = e.message) } }
            }
        }
    }

    private fun observePurohitBookings() {
        viewModelScope.launch {
            repository.observePurohitBookings().collect { result ->
                result
                    .onSuccess { list -> purohitBookings.value = list }
                    .onFailure { e   -> _uiState.update { it.copy(error = e.message) } }
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
                        activeBookings    = grouped[BookingCategory.ACTIVE]    ?: emptyList(),
                        cancelledBookings = grouped[BookingCategory.CANCELLED] ?: emptyList(),
                        completedBookings = grouped[BookingCategory.COMPLETED] ?: emptyList(),
                        isLoading         = false,
                        error             = null
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
            current.activeBookings.any    { it.bookingId == bookingId } -> 0
            current.cancelledBookings.any { it.bookingId == bookingId } -> 1
            current.completedBookings.any { it.bookingId == bookingId } -> 2
            else -> 0
        }
        _uiState.update { it.copy(highlightedBookingId = bookingId, requestedTabIndex = tabIndex) }
    }

    fun clearHighlight() = _uiState.update { it.copy(highlightedBookingId = null) }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun acceptBooking(booking: Booking) =
        updateStatus(booking, BookingStatus.ACCEPTED, "Booking accepted")

    /**
     * All purohit rejections require a reason — regardless of current status.
     * PAYMENT_DONE and ACCEPTED both go through this path.
     */
    fun rejectBookingWithRemarks(booking: Booking, remarks: String) {
        val trimmed = remarks.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Please provide a reason for rejection."))
            }
            return
        }
        viewModelScope.launch {
            repository.rejectBookingWithRemarks(booking.bookingId, trimmed)
                .onSuccess {
                    applyLocalUpdate(
                        booking.copy(
                            status    = BookingStatus.REJECTED,
                            comments  = trimmed,
                            updatedAt = Timestamp.now()
                        )
                    )
                    _effect.emit(BookingsEffect.ShowSnackbar("Booking rejected."))
                }
                .onFailure { e ->
                    _effect.emit(BookingsEffect.ShowSnackbar(e.message ?: "Rejection failed. Please try again."))
                }
        }
    }

    /** User cancel — no remarks required. */
    fun cancelBooking(booking: Booking) =
        updateStatus(booking, BookingStatus.CANCELLED, "Booking cancelled")

    fun completeBooking(booking: Booking) =
        updateStatus(booking, BookingStatus.COMPLETED, "Booking marked as completed")

    fun processPaymentStub(booking: Booking, isSuccess: Boolean) {
        if (isSuccess) {
            updateStatus(booking, BookingStatus.PAYMENT_DONE, "Payment successful!")
        } else {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Payment failed or cancelled."))
            }
        }
    }

    /**
     * Customer edits address and/or scheduled date.
     * Blocked by the 1-day window upstream (UI enforces this before calling here).
     * Writes the booking update + sends a notification to the purohit.
     */
    fun updateBookingAddressAndTime(
        booking          : Booking,
        newAddress       : String,
        newScheduledDate : Timestamp,
        newCoordinates   : com.poojapurohit.booking.model.Coordinates? = null
    ) {
        val trimmedAddress = newAddress.trim()
        if (trimmedAddress.isBlank()) {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Address cannot be empty."))
            }
            return
        }
        viewModelScope.launch {
            repository.updateBookingAddressAndTime(booking, trimmedAddress, newScheduledDate, newCoordinates)
                .onSuccess {
                    applyLocalUpdate(
                        booking.copy(
                            address       = trimmedAddress,
                            scheduledDate = newScheduledDate,
                            coordinates   = newCoordinates ?: booking.coordinates,
                            updatedAt     = Timestamp.now()
                        )
                    )
                    _effect.emit(BookingsEffect.ShowSnackbar("Booking updated. Purohit has been notified."))
                }
                .onFailure { e ->
                    _effect.emit(BookingsEffect.ShowSnackbar(e.message ?: "Update failed. Please try again."))
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
        userBookings.update    { list -> list.map { if (it.bookingId == updated.bookingId) updated else it } }
        purohitBookings.update { list -> list.map { if (it.bookingId == updated.bookingId) updated else it } }
    }

    // ── Role ──────────────────────────────────────────────────────────────────

    fun currentUserIsPurohitFor(booking: Booking): Boolean {
        val uid = repository.currentUserId() ?: return false
        return uid == booking.purohitId
    }
}