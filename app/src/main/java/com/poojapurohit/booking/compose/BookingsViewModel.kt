package com.poojapurohit.booking.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.poojapurohit.booking.data.BookingsRepository
import com.poojapurohit.booking.data.WrongOtpException
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
    val requestedTabIndex : Int           = 0,

    /**
     * Non-null when the purohit has clicked "Complete Order" and the OTP
     * has been successfully written to Firestore. The UI should show the
     * OTP input dialog at this point.
     *
     * Cleared on successful verification or explicit dismissal.
     */
    val otpPendingBooking : Booking?      = null,

    /**
     * True while [initiateCompletion] or [verifyOtpAndComplete] is in flight.
     * Used to show a loading indicator inside the OTP dialog.
     */
    val otpLoading        : Boolean       = false
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

    fun cancelBooking(booking: Booking) =
        updateStatus(booking, BookingStatus.CANCELLED, "Booking cancelled")

    fun processPaymentStub(booking: Booking, isSuccess: Boolean) {
        if (isSuccess) {
            updateStatus(booking, BookingStatus.PAYMENT_DONE, "Payment successful!")
        } else {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Payment failed or cancelled."))
            }
        }
    }

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

    // ── OTP completion flow ───────────────────────────────────────────────────

    /**
     * Step 1: Purohit taps "Complete Order".
     *
     * Generates a 6-digit OTP, writes it to [bookings/{id}.completionOtp],
     * then puts the booking into [otpPendingBooking] so the UI shows the
     * OTP input dialog. The OTP itself is never surfaced in UI state.
     */
    fun initiateCompletion(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(otpLoading = true) }
            repository.initiateCompletion(booking.bookingId)
                .onSuccess {
                    // OTP written successfully — show the input dialog
                    _uiState.update { it.copy(otpPendingBooking = booking, otpLoading = false) }
                    _effect.emit(
                        BookingsEffect.ShowSnackbar("OTP sent to customer. Ask them for the code.")
                    )
                }
                .onFailure { e ->
                    _uiState.update { it.copy(otpLoading = false) }
                    _effect.emit(BookingsEffect.ShowSnackbar(e.message ?: "Failed to initiate completion. Try again."))
                }
        }
    }

    /**
     * Step 2: Purohit enters the OTP received verbally from the customer.
     *
     * Fetches the stored OTP from Firestore, compares it, and on match
     * atomically marks the booking COMPLETED and deletes [completionOtp].
     * On mismatch, emits a snackbar — the dialog stays open for retry.
     */
    fun verifyOtpAndComplete(booking: Booking, inputOtp: String) {
        val trimmed = inputOtp.trim()
        if (trimmed.length != 6 || !trimmed.all { it.isDigit() }) {
            viewModelScope.launch {
                _effect.emit(BookingsEffect.ShowSnackbar("Enter the 6-digit code from the customer."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(otpLoading = true) }
            repository.verifyOtpAndComplete(booking.bookingId, trimmed)
                .onSuccess {
                    applyLocalUpdate(
                        booking.copy(
                            status        = BookingStatus.COMPLETED,
                            completionOtp = null,
                            updatedAt     = Timestamp.now()
                        )
                    )
                    _uiState.update { it.copy(otpPendingBooking = null, otpLoading = false) }
                    _effect.emit(BookingsEffect.ShowSnackbar("Booking marked as completed."))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(otpLoading = false) }
                    val message = when (e) {
                        is WrongOtpException -> e.message ?: "Incorrect code."
                        else                 -> e.message ?: "Verification failed. Try again."
                    }
                    _effect.emit(BookingsEffect.ShowSnackbar(message))
                }
        }
    }

    /** Called when the purohit explicitly dismisses the OTP dialog. */
    fun dismissOtpDialog() = _uiState.update { it.copy(otpPendingBooking = null, otpLoading = false) }

    // ── Internal helpers ──────────────────────────────────────────────────────

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