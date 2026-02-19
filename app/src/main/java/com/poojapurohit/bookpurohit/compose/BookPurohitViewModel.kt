package com.poojapurohit.bookpurohit.compose

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.poojapurohit.bookpurohit.compose.model.PurohitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationItem(
    val id: String = "",
    val name: String = "",
    val count: Int = 0
)

data class BookPurohitUiState(
    val locations: List<LocationItem> = emptyList(),
    val subLocations: List<LocationItem> = emptyList(),
    val purohits: List<PurohitItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLocationId: String? = null,
    val currentSubLocationId: String? = null
)

sealed interface BookPurohitEvent {
    data class SearchQueryChanged(val query: String) : BookPurohitEvent
    data class LocationSelected(val locationId: String) : BookPurohitEvent
    data class SubLocationSelected(val locationId: String, val subLocationId: String) : BookPurohitEvent
}

private enum class ListenerType {
    LOCATIONS, SUBLOCATIONS, PUROHITS
}

class BookPurohitViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(BookPurohitUiState())
    val uiState: StateFlow<BookPurohitUiState> = _uiState.asStateFlow()

    private var allLocations: List<LocationItem> = emptyList()
    private var allSubLocations: List<LocationItem> = emptyList()
    private var allPurohits: List<PurohitItem> = emptyList()

    private var locationsListener: ListenerRegistration? = null
    private var subLocationsListener: ListenerRegistration? = null
    private var purohitsListener: ListenerRegistration? = null
    private val activeListeners = mutableSetOf<ListenerType>()

    init {
        attachLocationsListener()
    }

    override fun onCleared() {
        super.onCleared()
        removeAllListeners()
    }

    private fun removeAllListeners() {
        locationsListener?.remove()
        subLocationsListener?.remove()
        purohitsListener?.remove()
        activeListeners.clear()
    }

    fun onEvent(event: BookPurohitEvent) {
        when (event) {
            is BookPurohitEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is BookPurohitEvent.LocationSelected -> attachSubLocationsListener(event.locationId)
            is BookPurohitEvent.SubLocationSelected ->
                attachPurohitsListener(event.locationId, event.subLocationId)
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        when {
            _uiState.value.currentSubLocationId != null -> filterPurohits(query)
            _uiState.value.currentLocationId != null -> filterSubLocations(query)
            else -> filterLocations(query)
        }
    }

    private fun filterLocations(query: String) {
        val filtered = if (query.isBlank()) allLocations
        else allLocations.filter { it.name.contains(query, ignoreCase = true) }
        _uiState.update { it.copy(locations = filtered) }
    }

    private fun filterSubLocations(query: String) {
        val filtered = if (query.isBlank()) allSubLocations
        else allSubLocations.filter { it.name.contains(query, ignoreCase = true) }
        _uiState.update { it.copy(subLocations = filtered) }
    }

    private fun filterPurohits(query: String) {
        val filtered = if (query.isBlank()) allPurohits
        else allPurohits.filter { purohit ->
            purohit.name.contains(query, ignoreCase = true) ||
                    purohit.proficiency.any { it.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(purohits = filtered) }
    }

    fun attachLocationsListener() {
        if (activeListeners.contains(ListenerType.LOCATIONS)) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        // Fetch all service partners, aggregate city data client-side
        locationsListener = firestore.collection("users")
            .whereEqualTo("isServicePartner", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val cityMap = mutableMapOf<String, LocationItem>()

                snapshot?.documents?.forEach { doc ->
                    val cityId = doc.getString("city") ?: return@forEach
                    val cityName = doc.getString("cityName") ?: cityId
                    val existing = cityMap[cityId]
                    cityMap[cityId] = LocationItem(
                        id = cityId,
                        name = cityName,
                        count = (existing?.count ?: 0) + 1
                    )
                }

                val locations = cityMap.values.sortedBy { it.name }
                allLocations = locations
                _uiState.update {
                    it.copy(
                        locations = locations,
                        isLoading = false,
                        currentLocationId = null
                    )
                }
            }
        activeListeners.add(ListenerType.LOCATIONS)
    }

    fun attachSubLocationsListener(locationId: String) {
        detachSubLocationsListener()
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true, error = null, searchQuery = "",
                subLocations = emptyList(), purohits = emptyList(),
                currentLocationId = locationId, currentSubLocationId = null
            )
        }

        // Query service partners in selected city, aggregate locality data client-side
        subLocationsListener = firestore.collection("users")
            .whereEqualTo("isServicePartner", true)
            .whereEqualTo("city", locationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val localityMap = mutableMapOf<String, LocationItem>()

                snapshot?.documents?.forEach { doc ->
                    val localityId = doc.getString("locality") ?: return@forEach
                    val localityName = doc.getString("localityName") ?: localityId
                    val existing = localityMap[localityId]
                    localityMap[localityId] = LocationItem(
                        id = localityId,
                        name = localityName,
                        count = (existing?.count ?: 0) + 1
                    )
                }

                val subLocations = localityMap.values.sortedBy { it.name }
                allSubLocations = subLocations
                _uiState.update { it.copy(subLocations = subLocations, isLoading = false) }
            }
        activeListeners.add(ListenerType.SUBLOCATIONS)
    }

    fun attachPurohitsListener(locationId: String, subLocationId: String) {
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true, error = null, searchQuery = "", purohits = emptyList(),
                currentLocationId = locationId, currentSubLocationId = subLocationId
            )
        }

        purohitsListener = firestore.collection("users")
            .whereEqualTo("isServicePartner", true)
            .whereEqualTo("city", locationId)
            .whereEqualTo("locality", subLocationId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = "Query failed: ${error.message}") }
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    PurohitItem.fromDocument(doc)
                } ?: emptyList()

                allPurohits = items
                _uiState.update { it.copy(purohits = items, isLoading = false) }
            }

        activeListeners.add(ListenerType.PUROHITS)
    }

    private fun detachSubLocationsListener() {
        subLocationsListener?.remove()
        subLocationsListener = null
        activeListeners.remove(ListenerType.SUBLOCATIONS)
    }

    private fun detachPurohitsListener() {
        purohitsListener?.remove()
        purohitsListener = null
        activeListeners.remove(ListenerType.PUROHITS)
    }

    fun resetToLocations() {
        detachSubLocationsListener()
        detachPurohitsListener()
        _uiState.update {
            it.copy(
                subLocations = emptyList(), purohits = emptyList(), searchQuery = "",
                currentLocationId = null, currentSubLocationId = null, locations = allLocations
            )
        }
    }

    fun resetToSubLocations() {
        detachPurohitsListener()
        _uiState.update {
            it.copy(
                purohits = emptyList(), searchQuery = "",
                currentSubLocationId = null, subLocations = allSubLocations
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}