package com.poojapurohit.dashboard.compose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
    val city: String = "",
    val locality: String = "",
    val experience: String = "",
    val selectedSkills: List<String> = emptyList(),
    val availableSkills: List<String> = emptyList(),
    val isServicePartner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
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
    data object SaveProfile : EditProfileEvent
}

sealed interface EditProfileEffect {
    data class ShowToast(val message: String) : EditProfileEffect
    data object NavigateBack : EditProfileEffect
}

class EditProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
            is EditProfileEvent.SaveProfile -> saveProfile()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    // ─── Load Available Skills ────────────────────────────────────────────────
    // Queries services collection: isActive == true, ordered by displayOrder.
    // Each doc has a `name` string field — mirrors AuthRepository.loadServices().

    private fun loadAvailableSkills() {
        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("services")
                    .whereEqualTo("isActive", true)
                    .orderBy("displayOrder")
                    .get()
                    .await()

                val skills = snapshot.documents.mapNotNull { it.getString("name") }
                _uiState.update { it.copy(availableSkills = skills) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading skills", e)
            }
        }
    }

    private fun handleNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    private fun handlePhoneChanged(phone: String) {
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phone = filtered, phoneError = null) }
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
        val currentSkills = _uiState.value.selectedSkills
        val updatedSkills = if (currentSkills.contains(skill)) {
            currentSkills - skill
        } else {
            currentSkills + skill
        }
        _uiState.update { it.copy(selectedSkills = updatedSkills, skillsError = null) }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    _uiState.update {
                        it.copy(
                            name = userDoc.getString("name") ?: "",
                            phone = (userDoc.getString("phone") ?: "").removePrefix("+91"),
                            city = userDoc.getString("city") ?: "",
                            locality = userDoc.getString("locality") ?: "",
                            isServicePartner = false,
                            isLoading = false
                        )
                    }
                    return@launch
                }

                val purohitDoc = firestore.collection("purohits").document(uid).get().await()
                if (purohitDoc.exists()) {
                    val skills = (purohitDoc.get("proficiency") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            name = purohitDoc.getString("name") ?: "",
                            phone = (purohitDoc.getString("phone") ?: "").removePrefix("+91"),
                            city = purohitDoc.getString("city") ?: "",
                            locality = purohitDoc.getString("locality") ?: "",
                            experience = purohitDoc.getLong("experience")?.toString() ?: "",
                            selectedSkills = skills,
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

    private fun validateInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        if (currentState.name.trim().length < 3) {
            _uiState.update { it.copy(nameError = "Name too short") }
            isValid = false
        }
        if (currentState.phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Invalid phone number") }
            isValid = false
        }

        if (currentState.isServicePartner) {
            if (currentState.city.trim().isBlank()) {
                _uiState.update { it.copy(cityError = "Required") }
                isValid = false
            }
            if (currentState.locality.trim().isBlank()) {
                _uiState.update { it.copy(localityError = "Required") }
                isValid = false
            }
            if (currentState.selectedSkills.isEmpty()) {
                _uiState.update { it.copy(skillsError = "Select at least one skill") }
                isValid = false
            }
        }
        return isValid
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        if (!validateInputs()) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val updateData = buildUpdateData(_uiState.value)
                val collection = if (_uiState.value.isServicePartner) "purohits" else "users"

                firestore.collection(collection)
                    .document(uid)
                    .set(updateData, SetOptions.merge())
                    .await()

                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Profile updated")
                // Small yield to allow toast effect to be consumed before navigating back
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
            "name" to state.name.trim(),
            "phone" to "+91${state.phone.trim()}"
        )

        if (state.isServicePartner) {
            data["city"] = state.city.trim()
            data["locality"] = state.locality.trim()
            data["experience"] = state.experience.trim().toIntOrNull() ?: 0
            data["proficiency"] = state.selectedSkills
        }

        return data
    }
}