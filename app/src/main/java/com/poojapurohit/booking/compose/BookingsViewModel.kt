package com.poojapurohit.booking.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.booking.data.BookingRepository
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingCategory
import com.poojapurohit.booking.model.BookingStatus
import com.poojapurohit.booking.model.category
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

class BookingsViewModel(
    private val repository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BookingsEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<BookingsEffect> = _effect.asSharedFlow()

    // Backing lists updated independently by each flow collector.
    // MutableStateFlow ensures the merge always uses the latest snapshot from both.
    private val userBookings = MutableStateFlow<List<Booking>>(emptyList())
    private val purohitBookings = MutableStateFlow<List<Booking>>(emptyList())

    init {
        observeUserBookings()
        observePurohitBookings()
        observeMerged()
    }

    /**
     * Collects the user-side query (whereEqualTo "userId") into [userBookings].
     */
    private fun observeUserBookings() {
        viewModelScope.launch {
            repository.observeUserBookings().collect { result: Result<List<Booking>> ->
                result.onSuccess { list: List<Booking> ->
                    userBookings.value = list
                }
            }
        }
    }

    /**
     * Collects the purohit-side query (whereEqualTo "purohitId") into [purohitBookings].
     */
    private fun observePurohitBookings() {
        viewModelScope.launch {
            repository.observePurohitBookings().collect { result: Result<List<Booking>> ->
                result.onSuccess { list: List<Booking> ->
                    purohitBookings.value = list
                }
            }
        }
    }

    /**
     * Merges [userBookings] and [purohitBookings] into a single deduplicated list
     * and pushes the grouped result into [_uiState].
     *
     * Uses kotlinx.coroutines.flow.combine with explicit types on the lambda
     * to avoid Kotlin's type inference ambiguity when both flows share the same type.
     */
    private fun observeMerged() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                userBookings,
                purohitBookings
            ) { fromUser: List<Booking>, fromPurohit: List<Booking> ->
                (fromUser + fromPurohit)
                    .distinctBy { booking: Booking -> booking.bookingId }
                    .sortedByDescending { booking: Booking -> booking.createdAt?.seconds ?: 0L }
            }.collect { merged: List<Booking> ->
                val grouped: Map<BookingCategory, List<Booking>> = merged.groupBy { it.status.category }
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

    // ── Deep link handling ────────────────────────────────────────────────────

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

    // ── Status update actions ─────────────────────────────────────────────────

    fun acceptBooking(booking: Booking) {
        updateStatus(booking, BookingStatus.ACCEPTED, "Booking accepted")
    }

    fun rejectBooking(booking: Booking) {
        updateStatus(booking, BookingStatus.REJECTED, "Booking rejected")
    }

    fun cancelBooking(booking: Booking) {
        updateStatus(booking, BookingStatus.CANCELLED, "Booking cancelled")
    }

    private fun updateStatus(booking: Booking, newStatus: BookingStatus, successMessage: String) {
        viewModelScope.launch {
            try {
                firestore.collection("bookings")
                    .document(booking.bookingId)
                    .update(
                        mapOf(
                            "status" to newStatus.name,
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()
                _effect.emit(BookingsEffect.ShowSnackbar(successMessage))
            } catch (e: Exception) {
                _effect.emit(
                    BookingsEffect.ShowSnackbar(e.message ?: "Action failed. Please try again.")
                )
            }
        }
    }

    // ── Role helpers ──────────────────────────────────────────────────────────

    fun currentUserIsPurohitFor(booking: Booking): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return uid == booking.purohitId
    }
}
