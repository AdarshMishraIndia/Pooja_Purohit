package com.poojapurohit.auth.compose.presentation

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.poojapurohit.auth.AuthFormData
import com.poojapurohit.auth.AuthFormValidator
import com.poojapurohit.auth.AuthRepository
import com.poojapurohit.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Migrated from LiveData to StateFlow
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Google Sign-In flow that fetches credentials via CredentialManager */
    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String,
        isServicePartner: Boolean = false
    ) {
        isServicePartnerFlow = isServicePartner
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val result = repository.signInWithGoogle(activity, credentialManager, clientId)
                _uiState.value = result.fold(
                    onSuccess = { isNewUser ->
                        if (isNewUser) {
                            currentStep = 1
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
    fun nextStep(name: String? = null, phone: String? = null, city: String? = null, locality: String? = null) {
        Log.d("AuthFlow", "nextStep - currentStep: $currentStep, isServicePartnerFlow: $isServicePartnerFlow")
        Log.d("AuthFlow", "nextStep - name: $name, phone: $phone, city: $city, locality: $locality")

        if (currentStep == 0) {
            Log.d("AuthFlow", "Handling step 0 - moving to step 1")
            currentStep = 1
            return nextStep(name, phone, city, locality)
        }

        when (currentStep) {
            1 -> {
                Log.d("AuthFlow", "Processing step 1 - Name and Phone")
                formData.name = name.orEmpty()
                formData.phone = phone.orEmpty()

                val error = AuthFormValidator().validateNameAndPhone(formData.name, formData.phone)
                if (error != null) {
                    Log.e("AuthFlow", "Validation error in step 1: $error")
                    return
                }

                Log.d("AuthFlow", "Step 1 validation passed")

                if (isServicePartnerFlow) {
                    Log.d("AuthFlow", "Moving to step 2 (service partner flow)")
                    currentStep = 2
                    _uiState.value = AuthUiState.ShowServicePartnerStep2
                    Log.d("AuthFlow", "UI state updated to ShowServicePartnerStep2")
                } else {
                    Log.d("AuthFlow", "Proceeding with customer registration")
                    registerUser()
                }
            }
            2 -> {
                Log.d("AuthFlow", "Processing step 2 - City and Locality")
                formData.city = city.orEmpty()
                formData.locality = locality.orEmpty()

                val error = AuthFormValidator().validateCityAndLocality(formData.city, formData.locality)
                if (error != null) {
                    Log.e("AuthFlow", "Validation error in step 2: $error")
                    return
                }

                Log.d("AuthFlow", "Step 2 validation passed")

                if (isServicePartnerFlow) {
                    Log.d("AuthFlow", "Loading services for step 3")
                    loadServicesForStep3()
                } else {
                    val errorMsg = "Invalid flow - reached step 2 in non-service partner flow"
                    Log.e("AuthFlow", errorMsg)
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            }
        }
    }

    /** Load services for step 3 via repository */
    private fun loadServicesForStep3() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
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

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.registerServicePartner(
                uid = user.uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = user.email.orEmpty(),
                city = formData.city,
                locality = formData.locality,
                proficiency = formData.services,
                experience = formData.experience
            )
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Service partner registration failed") }
            )
        }
    }

    /** Navigate back to previous step */
    fun goBackToPreviousStep() {
        Log.d("AuthFlow", "goBackToPreviousStep - currentStep: $currentStep")

        when (currentStep) {
            3 -> {
                currentStep = 2
                _uiState.value = AuthUiState.ShowServicePartnerStep2
                Log.d("AuthFlow", "Navigated back to step 2")
            }
            2 -> {
                currentStep = 1
                _uiState.value = AuthUiState.ShowServicePartnerStep1
                Log.d("AuthFlow", "Navigated back to step 1")
            }
            1 -> {
                currentStep = 0
                isServicePartnerFlow = false
                _uiState.value = AuthUiState.ShowInitialState
                Log.d("AuthFlow", "Navigated back to initial state")
            }
            else -> {
                Log.d("AuthFlow", "Already at initial step or invalid step")
            }
        }
    }
}