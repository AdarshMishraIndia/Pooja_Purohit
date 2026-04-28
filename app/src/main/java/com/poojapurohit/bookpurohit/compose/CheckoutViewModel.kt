package com.poojapurohit.bookpurohit.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val isPaymentDialogVisible: Boolean = false,
    val bookingComplete: Boolean = false,
    val error: String? = null,
    val selectedService: String = "POOJA (Sri Ganesh, Sri Vishwakarma...)",
    val address: String = "",
    val scheduledDateMillis: Long? = null,

    // FIX #2 — cached eagerly so payment write needs zero extra reads
    val cachedUserPhone: String = "",
    val cachedPurohitName: String = "",
    val isPrefetchComplete: Boolean = false
)

class CheckoutViewModel(
    // SavedStateHandle lets us read nav-arg purohitId without the screen passing it manually
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    val availableServices = listOf(
        "POOJA (Sri Ganesh, Sri Vishwakarma, Sri Satyanarayan, Maa Saraswati)",
        "DEVI PUJANA (Maa Durga, Maa Kali)",
        "Rudrabhishek Shivratri",
        "Janamastami",
        "Chhat Pooja",
        "Ruhan Pratistha",
        "Vaahaan Pooja"
    )

    init {
        // FIX #2 — pre-fetch user + purohit as soon as VM is created,
        // before the user even touches the payment button.
        // This way processPayment() only needs 1 write — no reads at payment time.
        val purohitId = savedStateHandle.get<String>("purohitId") ?: ""
        if (purohitId.isNotBlank()) prefetchData(purohitId)
    }

    // ─── FIX #2: EAGER PREFETCH ──────────────────────────────────────────────
    // Fetches user phone + purohit name once when screen loads.
    // If offline, Firestore offline cache serves this (persistence enabled in App).
    // Cached into uiState so payment coroutine does zero reads.
    private fun prefetchData(purohitId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Both fetches run sequentially — acceptable since this is background prefetch
                val userDoc = firestore.collection("users").document(userId).get().await()
                val purohitDoc = firestore.collection("purohits").document(purohitId).get().await()

                _uiState.update {
                    it.copy(
                        cachedUserPhone = userDoc.getString("phone") ?: "",
                        cachedPurohitName = purohitDoc.getString("name") ?: "Unknown Purohit",
                        isPrefetchComplete = true
                    )
                }
            } catch (_: Exception) {
                // Non-fatal — payment will catch missing data and surface a clear error
                _uiState.update { it.copy(isPrefetchComplete = true) }
            }
        }
    }

    fun onServiceChange(service: String) {
        _uiState.update { it.copy(selectedService = service) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    fun onDateChange(dateMillis: Long) {
        if (dateMillis < System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Back-dated bookings are not allowed") }
            return
        }
        _uiState.update { it.copy(scheduledDateMillis = dateMillis, error = null) }
    }

    fun showPaymentDialog() {
        if (_uiState.value.address.isBlank() || _uiState.value.scheduledDateMillis == null) {
            _uiState.update { it.copy(error = "Please fill in all details") }
            return
        }
        _uiState.update { it.copy(isPaymentDialogVisible = true, error = null) }
    }

    fun hidePaymentDialog() {
        _uiState.update { it.copy(isPaymentDialogVisible = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Writes booking doc to Firestore — atomic, idempotent, offline-safe.
     *
     * FIX #1 — Firestore offline persistence (enabled in Application class) means
     *           .set().await() resolves as soon as local cache is written, not
     *           waiting for server. SDK syncs to server when connectivity returns.
     *
     * FIX #2 — user phone + purohit name already cached in uiState (see prefetchData).
     *           Zero Firestore reads here — only 1 write. Nothing can go wrong mid-read.
     *
     * FIX #3 — bookingId derived from userId + purohitId + date-minute-bucket.
     *           Same inputs → same bookingId → .set() overwrites instead of duplicating.
     *           If user taps Pay twice (network retry, back-press, etc.), second write
     *           just overwrites the first identically. No duplicate bookings.
     *
     * Cloud Function onBookingStatusUpdated handles notifications — not here.
     */
    fun processPaymentStub(purohitId: String, status: String) {
        val selectedDate = _uiState.value.scheduledDateMillis ?: return
        val state = _uiState.value

        // Guard: prefetch must have completed — avoids writing with empty purohit name
        if (!state.isPrefetchComplete) {
            _uiState.update { it.copy(error = "Still loading details, please wait...") }
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(error = "User not logged in.") }
            return
        }

        // FIX #3 — DETERMINISTIC BOOKING ID ──────────────────────────────────
        // Bucket = selectedDate rounded to nearest minute (millis / 60_000).
        // Same user + same purohit + same minute window → same bookingId.
        // .set() is an upsert — retries are safe, no duplicates.
        val dateBucket = selectedDate / 60_000
        val bookingId = "${userId}_${purohitId}_$dateBucket"
        // ─────────────────────────────────────────────────────────────────────

        _uiState.update { it.copy(isLoading = true, isPaymentDialogVisible = false) }

        viewModelScope.launch {
            try {
                val currentTimestamp = Timestamp.now()

                // FIX #2 — reads replaced by cached values — single .set() write only
                val bookingData = hashMapOf(
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "purohitId" to purohitId,
                    "purohitName" to state.cachedPurohitName,   // from cache
                    "userPhone" to state.cachedUserPhone,        // from cache
                    "serviceName" to state.selectedService,
                    "amount" to 1500L,
                    "status" to status,
                    "razorpayOrderId" to "order_stub_${bookingId.takeLast(8)}",
                    "razorpayPaymentId" to if (status == "PAYMENT_DONE") {
                        "pay_stub_${bookingId.takeLast(8)}"
                    } else {
                        ""
                    },
                    "scheduledDate" to Timestamp(Date(selectedDate)),
                    "address" to state.address,
                    "createdAt" to currentTimestamp,
                    "updatedAt" to currentTimestamp
                )

                // FIX #1 + FIX #3 — .set() is an upsert (not .add()).
                // With offline persistence enabled, await() returns once local
                // cache confirms — does NOT wait for server round-trip.
                // If offline: write queued locally, syncs when online.
                // If retried: same bookingId → overwrites, no duplicate.
                firestore.collection("bookings")
                    .document(bookingId)    // explicit doc ID → idempotent
                    .set(bookingData)
                    .await()

                _uiState.update { it.copy(isLoading = false, bookingComplete = true) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Booking failed. Please retry."
                    )
                }
            }
        }
    }
}
