package com.poojapurohit.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Single source of truth for form
    val formData = AuthFormData()

    var currentStep = 0
    var isServicePartnerFlow = false

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    /** Google Sign-In flow */
    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        clientId: String,
        isServicePartner: Boolean = false
    ) {
        isServicePartnerFlow = isServicePartner
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context, credentialManager, clientId)
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
        }
    }

    /** Check if user already signed in */
    fun checkIfUserSignedIn() {
        _uiState.value = AuthUiState.Loading
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
                else loadServices()
            }
        }
    }

    /** Load services for step 3 */
    private fun loadServices() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val docRef = FirebaseFirestore.getInstance()
                .collection("services")
                .document("BookAPurohit")
            docRef.get()
                .addOnSuccessListener { doc ->
                    val services = (doc.get("name") as? List<*>)?.filterIsInstance<String>()?.sorted() ?: emptyList()
                    currentStep = 3
                    _uiState.value = AuthUiState.ShowServicePartnerStep3(services)
                }
                .addOnFailureListener {
                    currentStep = 3
                    _uiState.value = AuthUiState.ShowServicePartnerStep3(emptyList())
                }
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

    /** Navigate back a step */
    fun goBack() {
        when (currentStep) {
            3 -> _uiState.value = AuthUiState.ShowServicePartnerStep2.also { currentStep = 2 }
            2 -> _uiState.value = AuthUiState.ShowServicePartnerStep1.also { currentStep = 1 }
            1 -> _uiState.value = AuthUiState.ShowInitialState.also { currentStep = 0 }
        }
    }
}
