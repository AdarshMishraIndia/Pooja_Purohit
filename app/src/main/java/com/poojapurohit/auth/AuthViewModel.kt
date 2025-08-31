package com.poojapurohit.auth

import android.app.Activity
import android.util.Log
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
    private set

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
        Log.d("AuthFlow", "nextStep - currentStep: $currentStep, isServicePartnerFlow: $isServicePartnerFlow")
        Log.d("AuthFlow", "nextStep - name: $name, phone: $phone, location: $location")
        
        // If we're at step 0, move to step 1 and process with the same parameters
        if (currentStep == 0) {
            Log.d("AuthFlow", "Handling step 0 - moving to step 1")
            currentStep = 1
            // Call nextStep again with the same parameters to process step 1
            return nextStep(name, phone, location)
        }
        
        when (currentStep) {
            1 -> {
                Log.d("AuthFlow", "Processing step 1 - Name and Phone")
                formData.name = name.orEmpty()
                formData.phone = phone.orEmpty()
                
                // Validate the input
                val error = AuthFormValidator().validateNameAndPhone(formData.name, formData.phone)
                if (error != null) {
                    Log.e("AuthFlow", "Validation error in step 1: $error")
                    _uiState.value = AuthUiState.Error(error)
                    return
                }
                
                Log.d("AuthFlow", "Step 1 validation passed")
                
                if (isServicePartnerFlow) {
                    // For service partner, move to step 2
                    Log.d("AuthFlow", "Moving to step 2 (service partner flow)")
                    currentStep = 2
                    _uiState.value = AuthUiState.ShowServicePartnerStep2
                    Log.d("AuthFlow", "UI state updated to ShowServicePartnerStep2")
                } else {
                    // For customer, proceed with registration
                    Log.d("AuthFlow", "Proceeding with customer registration")
                    registerUser()
                }
            }
            2 -> {
                Log.d("AuthFlow", "Processing step 2 - Location")
                formData.location = location.orEmpty()
                
                // Validate the location
                val error = AuthFormValidator().validateLocation(formData.location)
                if (error != null) {
                    Log.e("AuthFlow", "Validation error in step 2: $error")
                    _uiState.value = AuthUiState.Error(error)
                    return
                }
                
                Log.d("AuthFlow", "Step 2 validation passed")
                
                if (isServicePartnerFlow) {
                    // For service partner, load services for step 3
                    Log.d("AuthFlow", "Loading services for step 3")
                    loadServicesForStep3()
                } else {
                    // This should not happen as we don't show step 2 for customers
                    val errorMsg = "Invalid flow - reached step 2 in non-service partner flow"
                    Log.e("AuthFlow", errorMsg)
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
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
