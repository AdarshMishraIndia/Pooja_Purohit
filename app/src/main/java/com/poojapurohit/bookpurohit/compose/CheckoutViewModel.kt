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
    val completedBookingId: String? = null,

    // Service price from nav arg (set by admin in portal)
    val amount: Long = 0L,

    // Map pin — mandatory before payment can proceed
    val coordinates: LatLng? = null,

    // Cached eagerly so payment write needs zero extra reads
    val cachedUserPhone: String = "",
    val cachedUserName: String = "",
    val cachedPurohitName: String = "",
    val cachedPurohitPhone: String = "",
    val isPrefetchComplete: Boolean = false
)

class CheckoutViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        val purohitId = savedStateHandle.get<String>("purohitId") ?: ""
        val serviceName = savedStateHandle.get<String>("serviceName") ?: ""
        val servicePrice = savedStateHandle.get<Int>("servicePrice")?.toLong() ?: 0L
        _uiState.update { it.copy(selectedService = serviceName, amount = servicePrice) }
        if (purohitId.isNotBlank()) prefetchData(purohitId)
    }

    private fun prefetchData(purohitId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Fetch purohit being booked (unchanged)
                val purohitDoc = firestore.collection("purohits").document(purohitId).get().await()

                // Determine if logged-in user is in purohits or users collection
                val loggedInPurohitDoc = firestore.collection("purohits").document(userId).get().await()

                val (userName, userPhone) = if (loggedInPurohitDoc.exists()) {
                    // Logged-in user is a purohit — use purohit doc (superset)
                    Pair(
                        loggedInPurohitDoc.getString("name") ?: "",
                        loggedInPurohitDoc.getString("phone") ?: ""
                    )
                } else {
                    // Fall back to users collection
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    Pair(
                        userDoc.getString("name") ?: "",
                        userDoc.getString("phone") ?: ""
                    )
                }

                _uiState.update {
                    it.copy(
                        cachedUserName     = userName,
                        cachedUserPhone    = userPhone,
                        cachedPurohitName  = purohitDoc.getString("name")  ?: "",
                        cachedPurohitPhone = purohitDoc.getString("phone") ?: "",
                        isPrefetchComplete = true
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isPrefetchComplete = true) }
            }
        }
    }

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

        val coords = state.coordinates
        if (coords == null) {
            _uiState.update { it.copy(error = "Location pin is required") }
            return
        }

        // Deterministic booking ID — idempotent upsert, no duplicates
        val dateBucket = selectedDate / 60_000
        val bookingId  = "${userId}_${purohitId}_$dateBucket"

        _uiState.update { it.copy(isLoading = true, isPaymentDialogVisible = false) }

        viewModelScope.launch {
            try {
                val currentTimestamp = Timestamp.now()

                val bookingData = hashMapOf(
                    "bookingId"         to bookingId,
                    "userId"            to userId,
                    "userName"          to state.cachedUserName,
                    "userPhone"         to state.cachedUserPhone,
                    "purohitId"         to purohitId,
                    "purohitName"       to state.cachedPurohitName,
                    "purohitPhone"      to state.cachedPurohitPhone,
                    "serviceName"       to state.selectedService,
                    "amount"            to state.amount,
                    "status"            to status,
                    "razorpayOrderId"   to "order_stub_${bookingId.takeLast(8)}",
                    "razorpayPaymentId" to if (status == "PAYMENT_DONE") {
                        "pay_stub_${bookingId.takeLast(8)}"
                    } else {
                        ""
                    },
                    "scheduledDate"     to Timestamp(Date(selectedDate)),
                    "address"           to state.address,
                    "coordinates"       to mapOf(
                        "latitude"  to coords.latitude,
                        "longitude" to coords.longitude
                    ),
                    "createdAt"         to currentTimestamp,
                    "updatedAt"         to currentTimestamp
                )

                firestore.collection("bookings")
                    .document(bookingId)
                    .set(bookingData)
                    .await()

                _uiState.update {
                    it.copy(
                        isLoading          = false,
                        bookingComplete    = true,
                        completedBookingId = bookingId
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Booking failed. Please retry."
                    )
                }
            }
        }
    }
}