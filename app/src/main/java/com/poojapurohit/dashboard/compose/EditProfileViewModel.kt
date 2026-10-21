package com.poojapurohit.dashboard.compose

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.poojapurohit.auth.AuthRepository
import com.poojapurohit.data.model.PurohitDoc
import com.poojapurohit.data.model.ServiceDoc
import com.poojapurohit.data.model.UserDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

data class EditProfileUiState(
    val name: String = "",
    val phone: String = "",
    val originalPhone: String = "",        // loaded from Firestore, never mutated after load
    val city: String = "",
    val locality: String = "",
    val experience: String = "",
    val selectedSkills: List<String> = emptyList(),
    val availableSkills: List<String> = emptyList(),
    val isServicePartner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    // OTP state
    val isOtpSent: Boolean = false,
    val otpVerificationId: String? = null,
    val isPhoneVerified: Boolean = false,
    // Field errors
    val nameError: String? = null,
    val phoneError: String? = null,
    val cityError: String? = null,
    val localityError: String? = null,
    val experienceError: String? = null,
    val skillsError: String? = null
)

sealed interface EditProfileEvent {
    data class NameChanged(val name: String) : EditProfileEvent
    data class PhoneChanged(val phone: String) : EditProfileEvent
    data class CityChanged(val city: String) : EditProfileEvent
    data class LocalityChanged(val locality: String) : EditProfileEvent
    data class ExperienceChanged(val experience: String) : EditProfileEvent
    data class SkillToggled(val skill: String) : EditProfileEvent
    data class SendPhoneOtp(val activity: Activity) : EditProfileEvent
    data class VerifyPhoneOtp(val otp: String) : EditProfileEvent
    data object SaveProfile : EditProfileEvent
}

sealed interface EditProfileEffect {
    data class ShowToast(val message: String) : EditProfileEffect
    data object NavigateBack : EditProfileEffect
}

class EditProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()

    companion object {
        private const val TAG = "EditProfileViewModel"
    }

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<EditProfileEffect?>(null)
    val effect: StateFlow<EditProfileEffect?> = _effect.asStateFlow()

    init {
        loadAvailableSkills()
        loadUserProfile()
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.NameChanged -> handleNameChanged(event.name)
            is EditProfileEvent.PhoneChanged -> handlePhoneChanged(event.phone)
            is EditProfileEvent.CityChanged -> handleCityChanged(event.city)
            is EditProfileEvent.LocalityChanged -> handleLocalityChanged(event.locality)
            is EditProfileEvent.ExperienceChanged -> handleExperienceChanged(event.experience)
            is EditProfileEvent.SkillToggled -> handleSkillToggled(event.skill)
            is EditProfileEvent.SendPhoneOtp -> handleSendPhoneOtp(event.activity)
            is EditProfileEvent.VerifyPhoneOtp -> handleVerifyPhoneOtp(event.otp)
            is EditProfileEvent.SaveProfile -> saveProfile()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    // ─── Load Available Skills ────────────────────────────────────────────────

    private fun loadAvailableSkills() {
        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection(ServiceDoc.COLLECTION)
                    .whereEqualTo(ServiceDoc.Fields.IS_ACTIVE, true)
                    .orderBy(ServiceDoc.Fields.DISPLAY_ORDER)
                    .get()
                    .await()

                val skills = snapshot.documents
                    .map { ServiceDoc.fromDocument(it).name }
                    .filter { it.isNotEmpty() }
                _uiState.update { it.copy(availableSkills = skills) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading skills", e)
            }
        }
    }

    // ─── Load User Profile ────────────────────────────────────────────────────

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userSnapshot = firestore.collection(UserDoc.COLLECTION).document(uid).get().await()
                if (userSnapshot.exists()) {
                    val user = UserDoc.fromDocument(userSnapshot)
                    val phone = user.phone.removePrefix("+91")
                    _uiState.update {
                        it.copy(
                            name = user.name,
                            phone = phone,
                            originalPhone = phone,
                            isServicePartner = false,
                            isLoading = false
                        )
                    }
                    return@launch
                }

                val purohitSnapshot = firestore.collection(PurohitDoc.COLLECTION).document(uid).get().await()
                if (purohitSnapshot.exists()) {
                    val purohit = PurohitDoc.fromDocument(purohitSnapshot)
                    val phone = purohit.phone.removePrefix("+91")
                    _uiState.update {
                        it.copy(
                            name = purohit.name,
                            phone = phone,
                            originalPhone = phone,
                            city = purohit.city,
                            locality = purohit.locality,
                            experience = purohit.experience?.toString() ?: "",
                            selectedSkills = purohit.proficiency,
                            isServicePartner = true,
                            isLoading = false
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ─── Field Handlers ───────────────────────────────────────────────────────

    private fun handleNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    private fun handlePhoneChanged(phone: String) {
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update {
            it.copy(
                phone = filtered,
                phoneError = null,
                // Reset OTP state whenever phone number is edited
                isPhoneVerified = false,
                isOtpSent = false,
                otpVerificationId = null
            )
        }
    }

    private fun handleCityChanged(city: String) {
        _uiState.update { it.copy(city = city, cityError = null) }
    }

    private fun handleLocalityChanged(locality: String) {
        _uiState.update { it.copy(locality = locality, localityError = null) }
    }

    private fun handleExperienceChanged(experience: String) {
        val filtered = experience.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(experience = filtered, experienceError = null) }
    }

    private fun handleSkillToggled(skill: String) {
        val updated = _uiState.value.selectedSkills.toMutableList().apply {
            if (contains(skill)) remove(skill) else add(skill)
        }
        _uiState.update { it.copy(selectedSkills = updated, skillsError = null) }
    }

    // ─── Phone OTP ────────────────────────────────────────────────────────────

    private fun handleSendPhoneOtp(activity: Activity) {
        val phone = _uiState.value.phone
        if (phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Enter a valid 10-digit number") }
            return
        }

        // Reset prior OTP state before sending
        _uiState.update { it.copy(isOtpSent = false, otpVerificationId = null, isPhoneVerified = false) }

        repository.sendOtp(
            phoneNumber = "+91$phone",
            activity = activity,
            onCodeSent = { verificationId ->
                _uiState.update { it.copy(isOtpSent = true, otpVerificationId = verificationId) }
            },
            onVerified = {
                // Auto-verified (instant verification / SIM auto-retrieval)
                _uiState.update { it.copy(isPhoneVerified = true, isOtpSent = false, otpVerificationId = null) }
            },
            onError = { msg ->
                _effect.value = EditProfileEffect.ShowToast(msg)
            }
        )
    }

    private fun handleVerifyPhoneOtp(otp: String) {
        val verificationId = _uiState.value.otpVerificationId ?: return

        repository.verifyOtp(
            verificationId = verificationId,
            otp = otp,
            onVerified = {
                _uiState.update {
                    it.copy(isPhoneVerified = true, isOtpSent = false, otpVerificationId = null)
                }
            },
            onError = { msg ->
                _effect.value = EditProfileEffect.ShowToast(msg)
            }
        )
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var valid = true

        if (state.name.trim().length < 3) {
            _uiState.update { it.copy(nameError = "Name too short") }
            valid = false
        }
        if (state.phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Invalid phone number") }
            valid = false
        }

        // Require OTP verification only if phone was changed
        if (state.phone != state.originalPhone && !state.isPhoneVerified) {
            _uiState.update { it.copy(phoneError = "Verify new phone number first") }
            valid = false
        }

        if (state.isServicePartner) {
            if (state.city.trim().isBlank()) {
                _uiState.update { it.copy(cityError = "Required") }
                valid = false
            }
            if (state.locality.trim().isBlank()) {
                _uiState.update { it.copy(localityError = "Required") }
                valid = false
            }
            if (state.selectedSkills.isEmpty()) {
                _uiState.update { it.copy(skillsError = "Select at least one skill") }
                valid = false
            }
        }

        return valid
    }

    // ─── Save Profile ─────────────────────────────────────────────────────────

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        if (!validateInputs()) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val updateData = buildUpdateData(_uiState.value)
                val collection = if (_uiState.value.isServicePartner) PurohitDoc.COLLECTION else UserDoc.COLLECTION

                firestore.collection(collection)
                    .document(uid)
                    .set(updateData, SetOptions.merge())
                    .await()

                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Profile updated")
                kotlinx.coroutines.delay(300.milliseconds)
                _effect.value = EditProfileEffect.NavigateBack
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Error: ${e.message}")
            }
        }
    }

    private fun buildUpdateData(state: EditProfileUiState): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            UserDoc.Fields.NAME to state.name.trim(),
            UserDoc.Fields.PHONE to "+91${state.phone.trim()}"
        )

        if (state.isServicePartner) {
            data[PurohitDoc.Fields.CITY] = state.city.trim()
            data[PurohitDoc.Fields.LOCALITY] = state.locality.trim()
            data[PurohitDoc.Fields.EXPERIENCE] = state.experience.trim().toIntOrNull() ?: 0
            data[PurohitDoc.Fields.PROFICIENCY] = state.selectedSkills
        }

        return data
    }
}