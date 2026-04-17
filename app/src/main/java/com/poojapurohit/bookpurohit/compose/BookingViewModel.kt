package com.poojapurohit.bookpurohit.compose

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
import java.util.UUID

data class BookingUiState(
    val isLoading: Boolean = false,
    val isPaymentDialogVisible: Boolean = false,
    val bookingComplete: Boolean = false,
    val error: String? = null,
    val selectedService: String = "POOJA (Sri Ganesh, Sri Vishwakarma...)",
    val address: String = "",
    val scheduledDateMillis: Long? = null // Stored as Long for internal logic
)

class BookingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    val availableServices = listOf(
        "POOJA (Sri Ganesh, Sri Vishwakarma, Sri Satyanarayan, Maa Saraswati)",
        "DEVI PUJANA (Maa Durga, Maa Kali)",
        "Rudrabhishek Shivratri",
        "Janamastami",
        "Chhat Pooja",
        "Gruha Pratistha",
        "Vaahaan Pooja"
    )

    fun onServiceChange(service: String) {
        _uiState.update { it.copy(selectedService = service) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    fun onDateChange(dateMillis: Long) {
        // Prevent back-dating by comparing with current time
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

    fun processPaymentStub(purohitId: String, status: String) {
        val selectedDate = _uiState.value.scheduledDateMillis ?: return

        _uiState.update { it.copy(isLoading = true, isPaymentDialogVisible = false) }

        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                    ?: throw Exception("User is not logged in.")

                val userSnapshot = firestore.collection("users").document(userId).get().await()
                if (!userSnapshot.exists()) {
                    throw Exception("User profile not found in database.")
                }
                val userPhone = userSnapshot.getString("phone") ?: ""

                val purohitSnapshot = firestore.collection("purohits").document(purohitId).get().await()
                if (!purohitSnapshot.exists()) {
                    throw Exception("Purohit details not found in database.")
                }
                val purohitName = purohitSnapshot.getString("name") ?: "Unknown Purohit"

                val bookingId = UUID.randomUUID().toString()
                val currentTimestamp = Timestamp.now()

                val bookingData = hashMapOf(
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "purohitId" to purohitId,
                    "purohitName" to purohitName,
                    "userPhone" to userPhone,
                    "serviceName" to _uiState.value.selectedService,
                    "amount" to 1500,
                    "status" to status,
                    "razorpayOrderId" to "order_stub_${UUID.randomUUID().toString().take(8)}",
                    "razorpayPaymentId" to if (status == "PAYMENT_DONE") "pay_stub_${UUID.randomUUID().toString().take(8)}" else "",
                    // FIX: Stored as Timestamp object
                    "scheduledDate" to Timestamp(Date(selectedDate)),
                    "address" to _uiState.value.address,
                    "createdAt" to currentTimestamp,
                    "updatedAt" to currentTimestamp
                )

                firestore.collection("bookings")
                    .document(bookingId)
                    .set(bookingData)
                    .await()

                _uiState.update { it.copy(isLoading = false, bookingComplete = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "An error occurred during booking.") }
            }
        }
    }
}