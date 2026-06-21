package com.poojapurohit.dashboard.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.R
import com.poojapurohit.dashboard.ServiceItem
import com.poojapurohit.notification.NotificationRepository
import com.poojapurohit.notification.compose.model.NotificationItem
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
    val unreadNotificationCount: Int = 0,
    val error: String? = null,
    val showDeleteDialog: Boolean = false
)

sealed interface DashboardEvent {
    data object SignOut : DashboardEvent
    data object DeleteAccountRequested : DashboardEvent
    data object DeleteAccountDismissed : DashboardEvent
    data object DeleteAccountConfirmed : DashboardEvent
    data object NavigateToEditAccount : DashboardEvent
    data object NavigateToAboutUs : DashboardEvent
    data object NavigateToTerms : DashboardEvent
    data object NavigateToPrivacyPolicy : DashboardEvent
    data object NavigateToNotifications : DashboardEvent
    data class ServiceClicked(val service: ServiceItem) : DashboardEvent
    data object CallContact : DashboardEvent
}

sealed interface DashboardEffect {
    data object NavigateToAuth : DashboardEffect
    data object NavigateToEditAccount : DashboardEffect
    data object NavigateToBookPurohit : DashboardEffect
    data object NavigateToNotifications : DashboardEffect
    data class ShowToast(val message: String) : DashboardEffect
    data class OpenUrl(val url: String) : DashboardEffect
    data class MakePhoneCall(val phoneNumber: String) : DashboardEffect
}

class DashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationRepository = NotificationRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<DashboardEffect?>(null)
    val effect: StateFlow<DashboardEffect?> = _effect.asStateFlow()

    init {
        loadServices()
        observeUserProfile()
        loadUnreadNotificationCount()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.SignOut -> handleSignOut()
            is DashboardEvent.DeleteAccountRequested -> _uiState.update { it.copy(showDeleteDialog = true) }
            is DashboardEvent.DeleteAccountDismissed -> _uiState.update { it.copy(showDeleteDialog = false) }
            is DashboardEvent.DeleteAccountConfirmed -> handleDeleteAccount()
            is DashboardEvent.NavigateToEditAccount -> handleEditAccount()
            is DashboardEvent.NavigateToAboutUs -> handleAboutUs()
            is DashboardEvent.NavigateToTerms -> handleTermsConditions()
            is DashboardEvent.NavigateToPrivacyPolicy -> handlePrivacyPolicy()
            is DashboardEvent.NavigateToNotifications -> {
                _effect.value = DashboardEffect.NavigateToNotifications
            }
            is DashboardEvent.ServiceClicked -> handleServiceClick(event.service)
            is DashboardEvent.CallContact -> handleCallContact()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun loadUnreadNotificationCount() {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collect { result ->
                result.onSuccess { items: List<NotificationItem> ->
                    val count = items.count { n: NotificationItem -> !n.isRead }
                    _uiState.update { it.copy(unreadNotificationCount = count) }
                }.onFailure {
                    // Fail silently for the badge
                }
            }
        }
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

        _uiState.update { it.copy(showDeleteDialog = false) }

        viewModelScope.launch {
            try {
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
        _effect.value = DashboardEffect.OpenUrl("https://about-pooja-purohit.netlify.app")
    }

    private fun handleTermsConditions() {
        _effect.value = DashboardEffect.OpenUrl("https://tnc-pooja-purohit.netlify.app")
    }

    private fun handlePrivacyPolicy() {
        _effect.value = DashboardEffect.OpenUrl("https://privacy-pooja-purohit.netlify.app")
    }

    private fun handleServiceClick(service: ServiceItem) {
        if (service.name.contains("Book a Purohit", ignoreCase = true)) {
            _effect.value = DashboardEffect.NavigateToBookPurohit
        } else {
            _effect.value = DashboardEffect.ShowToast("${service.name} coming soon...")
        }
    }

    private fun handleCallContact() {
        _effect.value = DashboardEffect.MakePhoneCall("9040292104")
    }
}