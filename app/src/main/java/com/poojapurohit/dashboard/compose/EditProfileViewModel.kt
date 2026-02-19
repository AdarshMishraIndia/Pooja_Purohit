package com.poojapurohit.dashboard.compose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private var previousCity: String = ""
    private var previousLocality: String = ""

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

    private fun loadAvailableSkills() {
        viewModelScope.launch {
            try {
                val document = firestore
                    .collection("services")
                    .document("BookAPurohit")
                    .get()
                    .await()

                val skillsRaw = document.get("name") as? List<*>
                val skills = skillsRaw?.filterIsInstance<String>() ?: emptyList()

                Log.d(TAG, "Available skills loaded: ${skills.size}")
                _uiState.update { it.copy(availableSkills = skills) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading skills", e)
                _uiState.update { it.copy(availableSkills = emptyList()) }
                _effect.value = EditProfileEffect.ShowToast("Failed to load skills: ${e.message}")
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
        val value = filtered.toIntOrNull()
        val validExperience = if (value != null && value > 100) "100" else filtered

        _uiState.update { it.copy(experience = validExperience, experienceError = null) }
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
                val rawPhone = document.getString("phone") ?: ""
                val phone = rawPhone.removePrefix("+91").trim()
                val city = document.getString("city") ?: ""
                val locality = document.getString("locality") ?: ""
                val experience = document.getString("experience")

                val proficiencyRaw = document.get("proficiency") as? List<*>
                val selectedSkills = proficiencyRaw?.filterIsInstance<String>() ?: emptyList()

                val isServicePartner = city.isNotBlank() || experience != null || selectedSkills.isNotEmpty()

                previousCity = city
                previousLocality = locality

                Log.d(TAG, "User profile loaded - isServicePartner: $isServicePartner")

                _uiState.update {
                    it.copy(
                        name = name,
                        phone = phone,
                        city = city,
                        locality = locality,
                        experience = experience ?: "",
                        selectedSkills = selectedSkills,
                        isServicePartner = isServicePartner,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                _effect.value = EditProfileEffect.ShowToast("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _uiState.value
        var isValid = true

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

        if (currentState.isServicePartner) {
            when {
                currentState.city.isBlank() -> {
                    _uiState.update { it.copy(cityError = "City is required") }
                    isValid = false
                }
                currentState.city.length < 2 -> {
                    _uiState.update { it.copy(cityError = "City must be at least 2 characters") }
                    isValid = false
                }
            }

            when {
                currentState.locality.isBlank() -> {
                    _uiState.update { it.copy(localityError = "Locality is required") }
                    isValid = false
                }
                currentState.locality.length < 2 -> {
                    _uiState.update { it.copy(localityError = "Locality must be at least 2 characters") }
                    isValid = false
                }
            }

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
                val newCity = currentState.city.trim()
                val newLocality = currentState.locality.trim()

                // Update user document
                val updateData = buildUpdateData(currentState)
                firestore.collection("users")
                    .document(uid)
                    .update(updateData)
                    .await()

                // Handle location changes
                if (currentState.isServicePartner) {
                    updateLocationReferences(
                        uid = uid,
                        oldCity = previousCity,
                        oldLocality = previousLocality,
                        newCity = newCity,
                        newLocality = newLocality
                    )
                }

                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Profile updated successfully")

                kotlinx.coroutines.delay(500)
                _effect.value = EditProfileEffect.NavigateBack
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
                _uiState.update { it.copy(isSaving = false) }
                _effect.value = EditProfileEffect.ShowToast("Failed to update profile: ${e.message}")
            }
        }
    }

    private suspend fun updateLocationReferences(
        uid: String,
        oldCity: String,
        oldLocality: String,
        newCity: String,
        newLocality: String
    ) {
        val locationChanged = oldCity != newCity || oldLocality != newLocality

        if (!locationChanged) {
            Log.d(TAG, "Location unchanged, skipping location update")
            return
        }

        try {
            // Remove from old location
            if (oldCity.isNotBlank() && oldLocality.isNotBlank()) {
                removeFromLocation(uid, oldCity, oldLocality)
            }

            // Add to new location
            if (newCity.isNotBlank() && newLocality.isNotBlank()) {
                addToLocation(uid, newCity, newLocality)
            }

            Log.d(TAG, "Location references updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location references", e)
            // Don't throw - location sync can be eventually consistent
        }
    }

    private suspend fun removeFromLocation(uid: String, cityId: String, localityId: String) {
        try {
            val subLocationRef = firestore
                .collection("locations")
                .document(cityId)
                .collection("subLocations")
                .document(localityId)

            val subLocationDoc = subLocationRef.get().await()

            if (!subLocationDoc.exists()) {
                Log.w(TAG, "SubLocation not found: $cityId/$localityId")
                return
            }

            val servicePartners = subLocationDoc.get("servicePartners") as? List<*>
            val containsUid = servicePartners?.contains(uid) == true

            if (!containsUid) {
                Log.w(TAG, "UID not in servicePartners: $cityId/$localityId")
                return
            }

            // Atomic removal from sublocation
            subLocationRef.update(
                mapOf(
                    "servicePartners" to FieldValue.arrayRemove(uid),
                    "count" to FieldValue.increment(-1)
                )
            ).await()

            // Atomic decrement in parent location
            val locationRef = firestore.collection("locations").document(cityId)
            locationRef.update("count", FieldValue.increment(-1)).await()

            Log.d(TAG, "Removed UID from: $cityId/$localityId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from location: $cityId/$localityId", e)
            throw e
        }
    }

    private suspend fun addToLocation(uid: String, cityId: String, localityId: String) {
        try {
            val locationRef = firestore.collection("locations").document(cityId)
            val subLocationRef = locationRef.collection("subLocations").document(localityId)

            // Create location document if not exists (atomic)
            locationRef.set(
                mapOf("name" to cityId, "count" to 0),
                SetOptions.merge()
            ).await()

            // Create sublocation document if not exists (atomic)
            subLocationRef.set(
                mapOf(
                    "name" to localityId,
                    "count" to 0,
                    "servicePartners" to emptyList<String>()
                ),
                SetOptions.merge()
            ).await()

            // Check if UID already exists (prevent duplicates)
            val subLocationDoc = subLocationRef.get().await()
            val servicePartners = subLocationDoc.get("servicePartners") as? List<*>

            if (servicePartners?.contains(uid) == true) {
                Log.w(TAG, "UID already exists in: $cityId/$localityId")
                return
            }

            // Atomic addition to sublocation
            subLocationRef.update(
                mapOf(
                    "servicePartners" to FieldValue.arrayUnion(uid),
                    "count" to FieldValue.increment(1)
                )
            ).await()

            // Atomic increment in parent location
            locationRef.update("count", FieldValue.increment(1)).await()

            Log.d(TAG, "Added UID to: $cityId/$localityId")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to location: $cityId/$localityId", e)
            throw e
        }
    }

    private fun buildUpdateData(state: EditProfileUiState): Map<String, Any> {
        val phoneWithPrefix = "+91${state.phone}"

        val updateData = mutableMapOf<String, Any>(
            "name" to state.name.trim(),
            "phone" to phoneWithPrefix
        )

        if (state.isServicePartner) {
            updateData["city"] = state.city.trim()
            updateData["locality"] = state.locality.trim()
            updateData["experience"] = state.experience
            updateData["proficiency"] = state.selectedSkills
        }

        return updateData
    }
}