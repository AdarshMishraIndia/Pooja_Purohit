package com.poojapurohit.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Single source of truth for form
    val formData = AuthFormData()

    var currentStep = 0
    var isServicePartnerFlow = false

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    /** Google Sign-In flow that fetches credentials via CredentialManager (matches Activity usage) */
    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String,
        isServicePartner: Boolean = false
    ) {
        isServicePartnerFlow = isServicePartner
        _uiState.value = AuthUiState.Loading

        // viewModelScope.launch defaults to Main dispatcher, which is required for CredentialManager UI
        viewModelScope.launch {
            try {
                val result = repository.signInWithGoogle(activity, credentialManager, clientId)
                _uiState.value = result.fold(
                    onSuccess = { isNewUser ->
                        if (isNewUser) {
                            if (isServicePartner) AuthUiState.ShowServicePartnerStep1
                            else AuthUiState.ShowCustomerFields
                        } else {
                            AuthUiState.Success
                        }
                    },
                    onFailure = { AuthUiState.Error(it.message ?: "Sign-in failed") }
                )
            } catch (_: SecurityException) {
                _uiState.value = AuthUiState.Error("Google Sign-In unavailable")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }


    /** Check if user already signed in */
    fun checkIfUserSignedIn() {
        // Don't show loading animation at startup - check silently in background
        viewModelScope.launch {
            try {
                val registered = repository.isUserRegistered()
                _uiState.value = if (registered) AuthUiState.Success else AuthUiState.ShowInitialState
            } catch (_: Exception) {
                _uiState.value = AuthUiState.ShowInitialState
            }
        }
    }

    /** Proceed to next step in registration */
    fun nextStep(name: String? = null, phone: String? = null, location: String? = null) {
        when (currentStep) {
            0 -> {
                // Handle case where ViewModel step is 0 but UI is at step 1
                currentStep = 1
                nextStep(name, phone, location)
            }
            1 -> {
                formData.name = name.orEmpty()
                formData.phone = phone.orEmpty()
                val error = AuthFormValidator().validateNameAndPhone(formData.name, formData.phone)
                if (error != null) _uiState.value = AuthUiState.Error(error)
                else {
                    currentStep = 2
                    _uiState.value = AuthUiState.ShowServicePartnerStep2
                }
            }
            2 -> {
                formData.location = location.orEmpty()
                val error = AuthFormValidator().validateLocation(formData.location)
                if (error != null) _uiState.value = AuthUiState.Error(error)
                else loadServicesForStep3()
            }
        }
    }

    /** Load services for step 3 via repository */
    fun loadServicesForStep3() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            // Let the UI show the loader before the Firestore fetch
            yield()
            val result = repository.loadServices()
            currentStep = 3
            _uiState.value = result.fold(
                onSuccess = { services -> AuthUiState.ShowServicePartnerStep3(services) },
                onFailure = { AuthUiState.ShowServicePartnerStep3(emptyList()) }
            )
        }
    }

    /** Register a customer */
    fun registerUser() {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            _uiState.value = AuthUiState.Error("User not signed in")
            return
        }
        formData.apply {
            // optionally, ensure name and phone are up to date
        }
        val error = AuthFormValidator().validateNameAndPhone(formData.name, formData.phone)
        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.registerUser(
                uid = user.uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = user.email.orEmpty()
            )
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    /** Register a service partner */
    fun registerServicePartner(experience: String, services: List<String>) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            _uiState.value = AuthUiState.Error("User not signed in")
            return
        }

        formData.experience = experience
        formData.services = services

        val validator = AuthFormValidator()
        val error = validator.validateNameAndPhone(formData.name, formData.phone)
            ?: validator.validateLocation(formData.location)
            ?: validator.validateServices(formData.services)
            ?: validator.validateExperience(formData.experience)

        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.registerServicePartner(
                uid = user.uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = user.email.orEmpty(),
                location = formData.location,
                proficiency = formData.services,
                experience = formData.experience
            )
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Service partner registration failed") }
            )
        }
    }
}
