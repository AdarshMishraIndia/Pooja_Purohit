package com.poojapurohit.dashboard.compose

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
    data object NavigateToEditAccount : DashboardEffect  // Added this
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
        loadUserProfile()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.SignOut -> handleSignOut()
            is DashboardEvent.DeleteAccountRequested -> {} // Handled by UI dialog
            is DashboardEvent.DeleteAccountConfirmed -> handleDeleteAccount()
            is DashboardEvent.NavigateToEditAccount -> handleEditAccount()
            is DashboardEvent.NavigateToAboutUs -> handleAboutUs()
            is DashboardEvent.NavigateToTerms -> handleTermsConditions()
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

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    val document = firestore.collection("users").document(uid).get().await()
                    val userName = document.getString("name") ?: "User"
                    val userEmail = document.getString("email") ?: auth.currentUser?.email ?: ""

                    _uiState.update {
                        it.copy(
                            userName = userName,
                            userEmail = userEmail,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleSignOut() {
        auth.signOut()
        _effect.value = DashboardEffect.NavigateToAuth
    }

    private fun handleDeleteAccount() {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid != null) {
            viewModelScope.launch {
                try {
                    firestore.collection("users").document(uid).delete().await()
                    user.delete().await()
                    _effect.value = DashboardEffect.NavigateToAuth
                } catch (e: Exception) {
                    _effect.value = DashboardEffect.ShowToast(
                        "Failed to delete account: ${e.message}"
                    )
                }
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

    private fun handleTermsConditions() {
        _effect.value = DashboardEffect.NavigateToInfo(
            title = "Terms & Conditions",
            content = """
            **TERMS & CONDITIONS — Puja Purohit Mobile Application**

            **Effective Date:** 1 January 2026  
            **Last Updated:** 1 December 2025

            These Terms & Conditions (“Terms”) govern the use of the Puja Purohit mobile application (“App”), owned and operated by the App Developer (“Developer”). By installing, registering, or using the App, all users agree to abide by these Terms. Users who do not agree should discontinue use immediately.

            ---

            ## 1. Definitions

            **1. Service Provider (Purohit):**  
            A registered individual offering religious, spiritual, or ritual-related services through the App.

            **2. Service Receiver (Jajman):**  
            An individual seeking or booking services from a Service Provider through the App.

            **3. Developer:**  
            The creator and operator of the Puja Purohit App, providing the digital platform.

            ---

            ## 2. User Categories & Eligibility

            The App offers two types of user accounts:

            **2.1 Service Providers (Purohits):**  
            Must complete registration and agree to the applicable fees, rules, and verification process.

            **2.2 Service Receivers (Jajmans):**  
            May use the App free of charge and must provide accurate information while booking any service.

            ---

            ## 3. Registration Charges & Renewal (For Service Providers)

            1. A one-time registration fee of ₹1001/- is applicable for Service Providers.  
            2. This registration is free up to **31 January 2024**.  
            3. An annual renewal fee of ₹1001/- is required to maintain unrestricted access.  
            4. Failure to pay the renewal fee may result in restricted access or reduced visibility.  
            5. All fees are **non-refundable** and **non-transferable**.

            ---

            ## 4. Charges for Service Receivers

            Service Receivers do **not** pay any registration or usage fee for accessing or booking through the App.

            ---

            ## 5. Role of the Developer

            1. The App acts solely as a digital platform enabling communication between Purohits and Jajmans.  
            2. The Developer is not a party to any agreement, communication, or transaction between users.  
            3. The Developer does **not** guarantee, control, or validate:  
               - Quality of services  
               - Conduct of users  
               - Fulfillment of any booking or ritual

            ---

            ## 6. No Financial Liability

            1. All financial transactions between Service Providers and Service Receivers occur **outside** the App.  
            2. The Developer is not responsible for payments, cancellations, disputes, or losses.  
            3. The Developer is not liable for refunds, compensation, or damages related to services booked.

            ---

            ## 7. Communication Between Users

            1. Communication between Purohits and Jajmans occurs directly (inside or outside the App).  
            2. The Developer does not monitor or interfere with user communication.  
            3. Users must maintain respectful and lawful communication.

            ---

            ## 8. User Obligations

            1. Provide accurate and genuine information during registration and usage.  
            2. Comply with applicable laws and ethical guidelines.  
            3. Avoid misuse, including abuse, fraud, impersonation, or unauthorized commercial use.

            ---

            ## 9. Suspension & Termination

            The Developer may restrict or terminate access if a user:

            1. Violates these Terms,  
            2. Provides false information,  
            3. Engages in harmful or fraudulent activities,  
            4. Fails to pay applicable fees (for Purohits).

            ---

            ## 10. Limitation of Liability

            The App is provided on an **“as-is”** and **“as-available”** basis.  
            The Developer is not liable for:

            1. Service failures, delays, or inaccuracies  
            2. Losses arising from user interactions  
            3. Technical issues, downtime, or data loss  
            4. Indirect, incidental, or consequential damages

            ---

            ## 11. Privacy

            User data is handled according to the App’s Privacy Policy.  
            Users should review the Privacy Policy for full details.

            ---

            ## 12. Modifications to Terms

            The Developer may update or revise these Terms at any time.  
            Continued use of the App indicates acceptance of the latest Terms.

            ---

            ## 13. Governing Law

            These Terms are governed by the laws of India.  
            All disputes fall under the jurisdiction of courts in **Sundargarh District**.

            ---

            ## 14. Contact Information

            **Email:** gdsrourkela@gmail.com  
            **Phone:** 9040292104  
            **Address:** Vedvyas Mandir, Rourkela – 769004
        """.trimIndent()
        )
    }


    private fun handleServiceClick(service: ServiceItem) {
        _effect.value = DashboardEffect.ShowToast("Selected: ${service.name}")
    }

    private fun handleCallContact() {
        _effect.value = DashboardEffect.MakePhoneCall("9438245904")
    }
}