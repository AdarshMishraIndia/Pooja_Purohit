package com.poojapurohit.bookpurohit.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
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
    val selectedService: String = "POOJA (Sri Ganesh, Sri Vishwakarma, Sri Satyanarayan, Maa Saraswati)",
    val address: String = "",
    val scheduledDateMillis: Long? = null,

    // Map pin — mandatory before payment can proceed
    val coordinates: LatLng? = null,

    // Cached eagerly so payment write needs zero extra reads (FIX #2)
    val cachedUserPhone: String = "",
    val cachedPurohitName: String = "",
    val isPrefetchComplete: Boolean = false
)

class CheckoutViewModel(
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
        "Gruha Pratistha",
        "Vaahaan Pooja",
        "Janeyu",
        "Shraddha",
        "Other Karma Kaanda",
        "Antyesthi / Asthi Sangraha"
    )

    init {
        val purohitId = savedStateHandle.get<String>("purohitId") ?: ""
        if (purohitId.isNotBlank()) prefetchData(purohitId)
    }

    // ─── FIX #2: EAGER PREFETCH ──────────────────────────────────────────────
    private fun prefetchData(purohitId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
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
                _uiState.update { it.copy(isPrefetchComplete = true) }
            }
        }
    }

    fun onServiceChange(service: String) = _uiState.update { it.copy(selectedService = service) }

    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address) }

    fun onCoordinatesSelected(latLng: LatLng) = _uiState.update { it.copy(coordinates = latLng) }

    fun onDateChange(dateMillis: Long) {
        if (dateMillis < System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Back-dated bookings are not allowed") }
            return
        }
        _uiState.update { it.copy(scheduledDateMillis = dateMillis, error = null) }
    }

    fun showPaymentDialog() {
        val state = _uiState.value
        when {
            state.address.isBlank() || state.scheduledDateMillis == null ->
                _uiState.update { it.copy(error = "Please fill in all details") }
            state.coordinates == null ->
                _uiState.update { it.copy(error = "Please pin your exact location on the map") }
            else ->
                _uiState.update { it.copy(isPaymentDialogVisible = true, error = null) }
        }
    }

    fun hidePaymentDialog() = _uiState.update { it.copy(isPaymentDialogVisible = false) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Writes booking doc to Firestore.
     *
     * FIX #1 — offline persistence (see BookPurohitApplication)
     * FIX #2 — zero reads at payment time; uses prefetched cache
     * FIX #3 — deterministic bookingId → idempotent upsert, no duplicates
     */
    fun processPaymentStub(purohitId: String, status: String) {
        val selectedDate = _uiState.value.scheduledDateMillis ?: return
        val state = _uiState.value

        if (!state.isPrefetchComplete) {
            _uiState.update { it.copy(error = "Still loading details, please wait...") }
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(error = "User not logged in.") }
            return
        }

        // Guard: coordinates must be present (enforced in showPaymentDialog too, but double-check)
        val coords = state.coordinates
        if (coords == null) {
            _uiState.update { it.copy(error = "Location pin is required") }
            return
        }

        // FIX #3 — DETERMINISTIC BOOKING ID
        val dateBucket = selectedDate / 60_000
        val bookingId = "${userId}_${purohitId}_$dateBucket"

        _uiState.update { it.copy(isLoading = true, isPaymentDialogVisible = false) }

        viewModelScope.launch {
            try {
                val currentTimestamp = Timestamp.now()

                val bookingData = hashMapOf(
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "purohitId" to purohitId,
                    "purohitName" to state.cachedPurohitName,
                    "userPhone" to state.cachedUserPhone,
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
                    // Stored as a nested map — GeoPoint is cleaner but requires
                    // Firestore GeoPoint type; using lat/lng map keeps it schema-agnostic
                    // and easy to consume from Cloud Functions / admin SDK.
                    "coordinates" to mapOf(
                        "latitude" to coords.latitude,
                        "longitude" to coords.longitude
                    ),
                    "createdAt" to currentTimestamp,
                    "updatedAt" to currentTimestamp
                )

                // FIX #1 + FIX #3 — upsert via explicit doc ID, offline-safe
                firestore.collection("bookings")
                    .document(bookingId)
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