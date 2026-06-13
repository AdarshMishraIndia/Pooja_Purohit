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
import com.poojapurohit.auth.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"

        /** Number of auto-retry attempts before giving up. */
        private const val MAX_RETRY_ATTEMPTS = 3

        /** Initial delay before first retry (ms). Doubles on each attempt (exponential backoff). */
        private const val RETRY_BASE_DELAY_MS = 3_000L
    }

    // ─── Form State ──────────────────────────────────────────────────────────

    val formData = AuthFormData()
    var currentStep = 0
    var isServicePartnerFlow = false
        private set

    // ─── UI State ────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ─── Active Auth Job (cancellable via back key) ───────────────────────────

    /**
     * Holds the currently running sign-in coroutine.
     * Cancelled when user presses back during Loading/RetryingConnection.
     */
    private var activeAuthJob: Job? = null

    // ─── Sign In With Google ─────────────────────────────────────────────────

    /**
     * Entry point for Google Sign-In.
     * Flow:
     *   1. Fast connectivity check (ConnectivityManager)
     *   2. Deep reachability ping (HTTP 204)
     *   3. Attempt sign-in with timeout
     *   4. On timeout/failure → exponential backoff retry up to MAX_RETRY_ATTEMPTS
     *   5. On NoCredentials → immediate typed error (no retry)
     *   6. On Cancelled → silently return to initial state
     */
    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String,
        isServicePartner: Boolean = false
    ) {
        isServicePartnerFlow = isServicePartner

        // Cancel any previous in-flight auth attempt
        activeAuthJob?.cancel()

        activeAuthJob = viewModelScope.launch {
            // ── Step 1: Fast network check ──────────────────────────────────
            val context = activity.applicationContext
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "No network reported by ConnectivityManager")
                _uiState.value = AuthUiState.NetworkError
                return@launch
            }

            // ── Step 2: Deep reachability ping ──────────────────────────────
            _uiState.value = AuthUiState.Loading
            val isReachable = NetworkUtils.isInternetReachable()
            if (!isReachable) {
                Log.w(TAG, "Internet unreachable — ping failed")
                _uiState.value = AuthUiState.NetworkError
                return@launch
            }

            // ── Step 3 + 4: Attempt with retry loop ─────────────────────────
            var lastFailureCause = "Sign-in failed"

            for (attempt in 1..MAX_RETRY_ATTEMPTS) {
                if (attempt > 1) {
                    val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 2)) // 3s, 6s, 12s
                    Log.d(TAG, "Retry attempt $attempt — waiting ${delayMs}ms")
                    _uiState.value = AuthUiState.RetryingConnection(
                        attempt = attempt,
                        maxAttempts = MAX_RETRY_ATTEMPTS,
                        statusMessage = "Slow connection. Retrying ($attempt/$MAX_RETRY_ATTEMPTS)…"
                    )
                    delay(delayMs.milliseconds)

                    // Re-check network before each retry
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        _uiState.value = AuthUiState.NetworkError
                        return@launch
                    }
                }

                when (val result = repository.signInWithGoogle(activity, credentialManager, clientId)) {

                    is AuthRepository.AuthResult.Success -> {
                        handleSignInSuccess(result.isNewUser, isServicePartner)
                        return@launch
                    }

                    is AuthRepository.AuthResult.NoCredentials -> {
                        // Device has no Google account — retrying won't help
                        Log.w(TAG, "No Google account found on device")
                        _uiState.value = AuthUiState.Error(
                            "No Google account found on this device. " +
                            "Please add a Google account in your device Settings and try again."
                        )
                        return@launch
                    }

                    is AuthRepository.AuthResult.Cancelled -> {
                        // User deliberately cancelled — restore initial screen silently
                        Log.d(TAG, "User cancelled sign-in picker")
                        _uiState.value = AuthUiState.ShowInitialState
                        return@launch
                    }

                    is AuthRepository.AuthResult.Timeout -> {
                        Log.w(TAG, "Attempt $attempt timed out")
                        lastFailureCause = "Connection timed out"
                        // Continue retry loop
                    }

                    is AuthRepository.AuthResult.Failure -> {
                        Log.e(TAG, "Attempt $attempt failed: ${result.cause.message}")
                        lastFailureCause = result.cause.message ?: "Sign-in failed"
                        // Continue retry loop
                    }
                }
            }

            // ── All retries exhausted ────────────────────────────────────────
            Log.e(TAG, "All $MAX_RETRY_ATTEMPTS attempts failed. Last cause: $lastFailureCause")
            _uiState.value = AuthUiState.Error(
                "Unable to connect after $MAX_RETRY_ATTEMPTS attempts. " +
                "Please check your internet connection and try again."
            )
        }
    }

    /**
     * Cancels the active auth job and returns to initial state.
     * Called when user presses back during Loading or RetryingConnection.
     */
    fun cancelSignIn() {
        Log.d(TAG, "User cancelled sign-in — cancelling active auth job")
        activeAuthJob?.cancel()
        activeAuthJob = null
        _uiState.value = AuthUiState.ShowInitialState
    }

    // ─── Sign In Success Handler ─────────────────────────────────────────────

    private fun handleSignInSuccess(isNewUser: Boolean, isServicePartner: Boolean) {
        _uiState.value = if (isNewUser) {
            currentStep = 1
            if (isServicePartner) AuthUiState.ShowServicePartnerStep1
            else AuthUiState.ShowCustomerFields
        } else {
            AuthUiState.Success
        }
    }

    // ─── Check If Already Signed In ──────────────────────────────────────────

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

    // ─── Multi-Step Navigation ───────────────────────────────────────────────

    fun nextStep(
        name: String? = null,
        phone: String? = null,
        city: String? = null,
        locality: String? = null
    ) {
        Log.d(TAG, "nextStep — currentStep: $currentStep, isServicePartnerFlow: $isServicePartnerFlow")

        if (currentStep == 0) {
            currentStep = 1
            return nextStep(name, phone, city, locality)
        }

        when (currentStep) {
            1 -> {
                formData.name = name.orEmpty()
                formData.phone = phone.orEmpty()

                val error = AuthFormValidator().validateNameAndPhone(formData.name, formData.phone)
                if (error != null) {
                    Log.e(TAG, "Validation error in step 1: $error")
                    return
                }

                if (isServicePartnerFlow) {
                    currentStep = 2
                    _uiState.value = AuthUiState.ShowServicePartnerStep2
                } else {
                    registerUser()
                }
            }
            2 -> {
                formData.city = city.orEmpty()
                formData.locality = locality.orEmpty()

                val error = AuthFormValidator().validateCityAndLocality(formData.city, formData.locality)
                if (error != null) {
                    Log.e(TAG, "Validation error in step 2: $error")
                    return
                }

                if (isServicePartnerFlow) {
                    loadServicesForStep3()
                } else {
                    _uiState.value = AuthUiState.Error("Invalid flow state")
                }
            }
        }
    }

    // ─── Load Services ───────────────────────────────────────────────────────
    
    private fun loadServicesForStep3() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            yield()
            // Call the new Map-based repository function
            val result = repository.loadServicesMap()
            currentStep = 3
            _uiState.value = result.fold(
                // Successfully pass the Map<String, String> to the UI State
                onSuccess = { servicesMap -> AuthUiState.ShowServicePartnerStep3(servicesMap) },
                // Use emptyMap() here instead of emptyList()
                onFailure = { AuthUiState.ShowServicePartnerStep3(emptyMap()) }
            )
        }
    }

    // ─── Register Customer ───────────────────────────────────────────────────

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

// ─── Register Service Partner ────────────────────────────────────────────

    /**
     * Handles Service Partner submission.
     * @param experience years of experience as a string input.
     * @param selectedServices Map containing the user-selected items formatted as Map<Slug, DisplayName>
     */

    fun registerServicePartner(experience: String, selectedServices: Map<String, String>) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            _uiState.value = AuthUiState.Error("User session expired. Please sign in again.")
            return
        }

        // Split map keys and values to feed the distinct arrays required by the schema
        val serviceIds = selectedServices.keys.toList()       // matches schema: "serviceIds": ["string"]
        val proficiency = selectedServices.values.toList()    // matches schema: "proficiency": ["string"]

        formData.experience = experience
        // Note: If formData requires storage updates, assign these lists to your Form Data container object.

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.registerServicePartner(
                uid = user.uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = user.email.orEmpty(),
                city = formData.city,
                locality = formData.locality,
                proficiency = proficiency,
                serviceIds = serviceIds, // Injected structural array fix
                experience = formData.experience
            )

            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = {
                    Log.e(TAG, "Registration execution caught error", it)
                    AuthUiState.Error(it.localizedMessage ?: "Service partner registration failed")
                }
            )
        }
    }

    // ─── Back Navigation ─────────────────────────────────────────────────────

    fun goBackToPreviousStep() {
        Log.d(TAG, "goBackToPreviousStep — currentStep: $currentStep")

        // If in loading/retrying state, cancel the operation instead of navigating
        val current = _uiState.value
        if (current is AuthUiState.Loading || current is AuthUiState.RetryingConnection) {
            cancelSignIn()
            return
        }

        when (currentStep) {
            3 -> {
                currentStep = 2
                _uiState.value = AuthUiState.ShowServicePartnerStep2
            }
            2 -> {
                currentStep = 1
                _uiState.value = AuthUiState.ShowServicePartnerStep1
            }
            1 -> {
                currentStep = 0
                isServicePartnerFlow = false
                _uiState.value = AuthUiState.ShowInitialState
            }
        }
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        activeAuthJob?.cancel()
    }
}
