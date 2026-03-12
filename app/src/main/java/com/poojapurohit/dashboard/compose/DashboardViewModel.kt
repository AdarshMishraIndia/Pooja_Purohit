package com.poojapurohit.dashboard.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.R
import com.poojapurohit.dashboard.ServiceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DashboardUiState(
    val userName: String = "User",
    val userEmail: String = "",
    val isLoading: Boolean = true,
    val services: List<ServiceItem> = emptyList(),
    val error: String? = null
)

sealed interface DashboardEvent {
    data object SignOut : DashboardEvent
    data object DeleteAccountRequested : DashboardEvent
    data object DeleteAccountConfirmed : DashboardEvent
    data object NavigateToEditAccount : DashboardEvent
    data object NavigateToAboutUs : DashboardEvent
    data object NavigateToTerms : DashboardEvent
    data class ServiceClicked(val service: ServiceItem) : DashboardEvent
    data object CallContact : DashboardEvent
}

sealed interface DashboardEffect {
    data object NavigateToAuth : DashboardEffect
    data object NavigateToEditAccount : DashboardEffect
    data object NavigateToBookPurohit : DashboardEffect  // Added
    data class ShowToast(val message: String) : DashboardEffect
    data class NavigateToInfo(val title: String, val content: String) : DashboardEffect
    data class MakePhoneCall(val phoneNumber: String) : DashboardEffect
}

class DashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<DashboardEffect?>(null)
    val effect: StateFlow<DashboardEffect?> = _effect.asStateFlow()

    init {
        loadServices()
        observeUserProfile()
    }

    fun onEvent(event: DashboardEvent, context: Context? = null) {
        when (event) {
            is DashboardEvent.SignOut -> handleSignOut()
            is DashboardEvent.DeleteAccountRequested -> {}
            is DashboardEvent.DeleteAccountConfirmed -> handleDeleteAccount()
            is DashboardEvent.NavigateToEditAccount -> handleEditAccount()
            is DashboardEvent.NavigateToAboutUs -> handleAboutUs()
            is DashboardEvent.NavigateToTerms -> {
                context?.let { handleTermsConditions(it) }
            }
            is DashboardEvent.ServiceClicked -> handleServiceClick(event.service)
            is DashboardEvent.CallContact -> handleCallContact()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun loadServices() {
        val services = listOf(
            ServiceItem(
                name = "Book a Purohit",
                description = "Search and book a Purohit near your area.",
                iconResId = R.drawable.ic_service_placeholder_book
            ),
            ServiceItem(
                name = "Horoscope\n(ଜାତକ)",
                description = "Get your horoscope in Odia/South style.",
                iconResId = R.drawable.ic_service_placeholder_horoscope
            ),
            ServiceItem(
                name = "Match Making\n(ବିବାହ ମେଳକ)",
                description = "Match Horoscope before planning marriage.",
                iconResId = R.drawable.ic_service_placeholder_matchmaking
            )
        )
        _uiState.update { it.copy(services = services) }
    }

    private fun observeUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        // Check users collection first
        val userRef = firestore.collection("users").document(uid)
        val purohitRef = firestore.collection("purohits").document(uid)

        userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.update { it.copy(error = error.message) }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("name") ?: "User"
                val email = auth.currentUser?.email ?: ""
                _uiState.update {
                    it.copy(userName = name, userEmail = email, isLoading = false)
                }
            } else {
                // Not in users, check purohits
                purohitRef.addSnapshotListener { purohitSnapshot, purohitError ->
                    if (purohitError != null) {
                        _uiState.update { it.copy(error = purohitError.message) }
                        return@addSnapshotListener
                    }
                    if (purohitSnapshot != null && purohitSnapshot.exists()) {
                        val name = purohitSnapshot.getString("name") ?: "Purohit"
                        val email = auth.currentUser?.email ?: ""
                        _uiState.update {
                            it.copy(userName = name, userEmail = email, isLoading = false)
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun handleSignOut() {
        auth.signOut()
        _effect.value = DashboardEffect.NavigateToAuth
    }

    private fun handleDeleteAccount() {
        val user = auth.currentUser
        val uid = user?.uid ?: return

        viewModelScope.launch {
            try {
                // Delete from whichever collection they exist in
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    firestore.collection("users").document(uid).delete().await()
                } else {
                    firestore.collection("purohits").document(uid).delete().await()
                }
                user.delete().await()
                _effect.value = DashboardEffect.NavigateToAuth
            } catch (e: Exception) {
                _effect.value = DashboardEffect.ShowToast("Failed to delete account: ${e.message}")
            }
        }
    }

    private fun handleEditAccount() {
        _effect.value = DashboardEffect.NavigateToEditAccount
    }

    private fun handleAboutUs() {
        _effect.value = DashboardEffect.NavigateToInfo(
            title = "About Us",
            content = """
                About Us
                
                Pooja Purohit is a trusted digital platform that connects individuals and families with qualified Purohits (priests) for performing a wide range of Hindu rituals, ceremonies, and poojas.
                
                Our mission is to make spiritual and religious services more accessible, convenient, and transparent—bridging the gap between tradition and technology while empowering the Purohit community.
                
                We are committed to providing an authentic, seamless, and respectful experience for every devotee seeking divine services.
                
                Development Team Contact
                📧 Email: gdsorissa@gmail.com
                
                Note: Pooja Purohit is currently under active development. Some features are being improved, and we appreciate your patience and continued support as we enhance your experience.
            """.trimIndent()
        )
    }

    private fun handleTermsConditions(context: Context) {
        val termsText = context.resources.openRawResource(R.raw.terms)
            .bufferedReader().use { it.readText() }

        _effect.value = DashboardEffect.NavigateToInfo(
            title = "Terms & Conditions",
            content = termsText
        )
    }

    private fun handleServiceClick(service: ServiceItem) {
        // Check if it's "Book a Purohit" service
        if (service.name.contains("Book a Purohit", ignoreCase = true)) {
            _effect.value = DashboardEffect.NavigateToBookPurohit
        } else {
            _effect.value = DashboardEffect.ShowToast("${service.name} coming soon...")
        }
    }

    private fun handleCallContact() {
        _effect.value = DashboardEffect.MakePhoneCall("9438245904")
    }
}
