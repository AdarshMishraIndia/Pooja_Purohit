package com.poojapurohit.dashboard.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EditProfileUiState(
    val name: String = "",
    val phone: String = "",
    val location: String = "",
    val experience: String = "",
    val selectedSkills: List<String> = emptyList(),
    val availableSkills: List<String> = emptyList(),
    val isServicePartner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val locationError: String? = null,
    val experienceError: String? = null,
    val skillsError: String? = null
)

sealed interface EditProfileEvent {
    data class NameChanged(val name: String) : EditProfileEvent
    data class PhoneChanged(val phone: String) : EditProfileEvent
    data class LocationChanged(val location: String) : EditProfileEvent
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
            is EditProfileEvent.LocationChanged -> handleLocationChanged(event.location)
            is EditProfileEvent.ExperienceChanged -> handleExperienceChanged(event.experience)
            is EditProfileEvent.SkillToggled -> handleSkillToggled(event.skill)
            is EditProfileEvent.SaveProfile -> saveProfile()
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun loadAvailableSkills() {
        viewModelScope.launch {
            try {
                // Fetch skills from Firestore BookAPurohit document
                val document = firestore
                    .collection("services")
                    .document("BookAPurohit")
                    .get()
                    .await()

                // Changed from "services" to "name"
                val skillsRaw = document.get("name") as? List<*>
                val skills = skillsRaw?.filterIsInstance<String>() ?: emptyList()

                _uiState.update { it.copy(availableSkills = skills) }
            } catch (e: Exception) {
                _uiState.update { it.copy(availableSkills = emptyList()) }
                _effect.value = EditProfileEffect.ShowToast("Failed to load skills: ${e.message}")
            }
        }
    }

    private fun handleNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = null
            )
        }
    }

    private fun handlePhoneChanged(phone: String) {
        // Only allow digits, max 10
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update {
            it.copy(
                phone = filtered,
                phoneError = null
            )
        }
    }

    private fun handleLocationChanged(location: String) {
        _uiState.update {
            it.copy(
                location = location,
                locationError = null
            )
        }
    }

    private fun handleExperienceChanged(experience: String) {
        // Only allow digits, max 3 digits (0-100)
        val filtered = experience.filter { it.isDigit() }.take(3)

        // Additional validation: don't allow values > 100
        val value = filtered.toIntOrNull()
        val validExperience = if (value != null && value > 100) {
            "100"
        } else {
            filtered
        }

        _uiState.update {
            it.copy(
                experience = validExperience,
                experienceError = null
            )
        }
    }

    private fun handleSkillToggled(skill: String) {
        val currentSkills = _uiState.value.selectedSkills
        val updatedSkills = if (currentSkills.contains(skill)) {
            currentSkills - skill
        } else {
            currentSkills + skill
        }
        _uiState.update {
            it.copy(
                selectedSkills = updatedSkills,
                skillsError = null
            )
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
            _effect.value = EditProfileEffect.ShowToast("User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()

                val name = document.getString("name") ?: ""

                // Remove +91 prefix if present
                val rawPhone = document.getString("phone") ?: ""
                val phone = rawPhone.removePrefix("+91").trim()

                // Check if service partner by presence of service partner fields
                val location = document.getString("location")
                val experience = document.getString("experience")

                // Get skills from "proficiency" field (string array) - PRIMARY SOURCE
                val proficiencyRaw = document.get("proficiency") as? List<*>
                val selectedSkills = proficiencyRaw?.filterIsInstance<String>() ?: emptyList()

                // Determine if user is service partner
                val isServicePartner = location != null || experience != null || selectedSkills.isNotEmpty()

                _uiState.update {
                    it.copy(
                        name = name,
                        phone = phone,
                        location = location ?: "",
                        experience = experience ?: "",
                        selectedSkills = selectedSkills,
                        isServicePartner = isServicePartner,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                _effect.value = EditProfileEffect.ShowToast("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true

        // Name validation
        when {
            currentState.name.isBlank() -> {
                _uiState.update { it.copy(nameError = "Name is required") }
                isValid = false
            }
            currentState.name.length < 3 -> {
                _uiState.update { it.copy(nameError = "Name must be at least 3 characters") }
                isValid = false
            }
            !currentState.name.all { it.isLetter() || it.isWhitespace() } -> {
                _uiState.update { it.copy(nameError = "Name can only contain letters and spaces") }
                isValid = false
            }
        }

        // Phone validation
        when {
            currentState.phone.isBlank() -> {
                _uiState.update { it.copy(phoneError = "Phone number is required") }
                isValid = false
            }
            currentState.phone.length != 10 -> {
                _uiState.update { it.copy(phoneError = "Phone number must be exactly 10 digits") }
                isValid = false
            }
        }

        // Service partner specific validations
        if (currentState.isServicePartner) {
            // Location validation
            when {
                currentState.location.isBlank() -> {
                    _uiState.update { it.copy(locationError = "Location is required") }
                    isValid = false
                }
                currentState.location.length < 3 -> {
                    _uiState.update { it.copy(locationError = "Location must be at least 3 characters") }
                    isValid = false
                }
            }

            // Experience validation
            when {
                currentState.experience.isBlank() -> {
                    _uiState.update { it.copy(experienceError = "Experience is required") }
                    isValid = false
                }
                else -> {
                    val expValue = currentState.experience.toIntOrNull()
                    when {
                        expValue == null || expValue < 0 -> {
                            _uiState.update { it.copy(experienceError = "Enter valid years of experience") }
                            isValid = false
                        }
                        expValue > 100 -> {
                            _uiState.update { it.copy(experienceError = "Experience cannot exceed 100 years") }
                            isValid = false
                        }
                    }
                }
            }

            // Skills validation
            if (currentState.selectedSkills.isEmpty()) {
                _uiState.update { it.copy(skillsError = "Please select at least one skill") }
                isValid = false
            }
        }

        return isValid
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _effect.value = EditProfileEffect.ShowToast("User not authenticated")
            return
        }

        if (!validateInputs()) {
            return
        }

        val currentState = _uiState.value
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val updateData = buildUpdateData(currentState)

                firestore.collection("users")
                    .document(uid)
                    .update(updateData)
                    .await()

                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Profile updated successfully")

                // Navigate back after short delay
                kotlinx.coroutines.delay(500)
                _effect.value = EditProfileEffect.NavigateBack
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Failed to update profile: ${e.message}")
            }
        }
    }

    private fun buildUpdateData(state: EditProfileUiState): Map<String, Any> {
        // Store phone with +91 prefix
        val phoneWithPrefix = "+91${state.phone}"

        val updateData = mutableMapOf<String, Any>(
            "name" to state.name.trim(),
            "phone" to phoneWithPrefix
        )

        if (state.isServicePartner) {
            updateData["location"] = state.location.trim()
            updateData["experience"] = state.experience

            // Update proficiency field as string array
            updateData["proficiency"] = state.selectedSkills
        }

        return updateData
    }
}