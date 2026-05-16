package com.poojapurohit.bookpurohit.compose

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.poojapurohit.bookpurohit.compose.model.PurohitItem
import com.poojapurohit.bookpurohit.compose.model.ServiceItem
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
    val services: List<ServiceItem> = emptyList(),
    val locations: List<LocationItem> = emptyList(),
    val subLocations: List<LocationItem> = emptyList(),
    val purohits: List<PurohitItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedServiceSlug: String = "",
    val currentLocationId: String? = null,
    val currentSubLocationId: String? = null
)

sealed interface BookPurohitEvent {
    data class SearchQueryChanged(val query: String) : BookPurohitEvent
    data class LocationSelected(val locationId: String) : BookPurohitEvent
    data class SubLocationSelected(val locationId: String, val subLocationId: String) : BookPurohitEvent
}

private enum class ListenerType {
    SERVICES, LOCATIONS, SUBLOCATIONS, PUROHITS
}

class BookPurohitViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(BookPurohitUiState())
    val uiState: StateFlow<BookPurohitUiState> = _uiState.asStateFlow()

    private var allServices: List<ServiceItem> = emptyList()
    private var allLocations: List<LocationItem> = emptyList()
    private var allSubLocations: List<LocationItem> = emptyList()
    private var allPurohits: List<PurohitItem> = emptyList()

    private var servicesListener: ListenerRegistration? = null
    private var locationsListener: ListenerRegistration? = null
    private var subLocationsListener: ListenerRegistration? = null
    private var purohitsListener: ListenerRegistration? = null
    private val activeListeners = mutableSetOf<ListenerType>()

    init {
        attachServicesListener()
    }

    override fun onCleared() {
        super.onCleared()
        removeAllListeners()
    }

    private fun removeAllListeners() {
        servicesListener?.remove()
        locationsListener?.remove()
        subLocationsListener?.remove()
        purohitsListener?.remove()
        activeListeners.clear()
    }

    fun onEvent(event: BookPurohitEvent) {
        when (event) {
            is BookPurohitEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is BookPurohitEvent.LocationSelected -> attachSubLocationsListener(event.locationId)
            is BookPurohitEvent.SubLocationSelected -> attachPurohitsListener(event.locationId, event.subLocationId)
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        when {
            _uiState.value.currentSubLocationId != null -> filterPurohits(query)
            _uiState.value.currentLocationId != null -> filterSubLocations(query)
            _uiState.value.selectedServiceSlug.isNotBlank() -> filterLocations(query)
            else -> filterServices(query)
        }
    }

    private fun filterServices(query: String) {
        val filtered = if (query.isBlank()) allServices
        else allServices.filter { it.name.contains(query, ignoreCase = true) }
        _uiState.update { it.copy(services = filtered) }
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

    // ── Services ──────────────────────────────────────────────────

    fun attachServicesListener() {
        if (activeListeners.contains(ListenerType.SERVICES)) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        servicesListener = firestore.collection("services")
            .whereEqualTo("isActive", true)
            .orderBy("displayOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }

                val services = snapshot?.documents
                    ?.mapNotNull { ServiceItem.fromDocument(it) }
                    ?.filter { it.isActive }
                    ?.sortedBy { it.displayOrder }
                    ?: emptyList()

                allServices = services
                _uiState.update { it.copy(services = services, isLoading = false) }

                servicesListener?.remove()
                servicesListener = null
                activeListeners.remove(ListenerType.SERVICES)
            }
        activeListeners.add(ListenerType.SERVICES)
    }

    // ── Locations (scoped to selected service) ────────────────────

    fun attachLocationsListener(serviceSlug: String) {
        detachLocationsListener()
        detachSubLocationsListener()
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true, error = null, searchQuery = "",
                selectedServiceSlug = serviceSlug,
                locations = emptyList(), subLocations = emptyList(), purohits = emptyList(),
                currentLocationId = null, currentSubLocationId = null
            )
        }

        locationsListener = firestore.collection("purohits")
            .whereEqualTo("isVerified", true)
            .whereEqualTo("isAvailable", true)
            .whereArrayContains("serviceIds", serviceSlug)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val cityMap = mutableMapOf<String, LocationItem>()
                snapshot?.documents?.forEach { doc ->
                    val cityId = doc.getString("city") ?: return@forEach
                    val existing = cityMap[cityId]
                    cityMap[cityId] = LocationItem(
                        id = cityId,
                        name = cityId,
                        count = (existing?.count ?: 0) + 1
                    )
                }

                val locations = cityMap.values.sortedBy { it.name }
                allLocations = locations
                _uiState.update { it.copy(locations = locations, isLoading = false) }
            }
        activeListeners.add(ListenerType.LOCATIONS)
    }

    // ── Sub-locations ─────────────────────────────────────────────

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

        subLocationsListener = firestore.collection("purohits")
            .whereEqualTo("isVerified", true)
            .whereEqualTo("isAvailable", true)
            .whereArrayContains("serviceIds", _uiState.value.selectedServiceSlug)
            .whereEqualTo("city", locationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val localityMap = mutableMapOf<String, LocationItem>()
                snapshot?.documents?.forEach { doc ->
                    val localityId = doc.getString("locality") ?: return@forEach
                    val existing = localityMap[localityId]
                    localityMap[localityId] = LocationItem(
                        id = localityId,
                        name = localityId,
                        count = (existing?.count ?: 0) + 1
                    )
                }

                val subLocations = localityMap.values.sortedBy { it.name }
                allSubLocations = subLocations
                _uiState.update { it.copy(subLocations = subLocations, isLoading = false) }
            }
        activeListeners.add(ListenerType.SUBLOCATIONS)
    }

    // ── Purohits ──────────────────────────────────────────────────

    fun attachPurohitsListener(locationId: String, subLocationId: String) {
        val serviceSlug = _uiState.value.selectedServiceSlug
        if (serviceSlug.isBlank()) {
            _uiState.update { it.copy(error = "Service not selected. Please go back and reselect.") }
            return
        }

        detachLocationsListener()
        detachSubLocationsListener()
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true, error = null, searchQuery = "", purohits = emptyList(),
                currentLocationId = locationId, currentSubLocationId = subLocationId
            )
        }

        purohitsListener = firestore.collection("purohits")
            .whereEqualTo("isVerified", true)
            .whereEqualTo("isAvailable", true)
            .whereArrayContains("serviceIds", _uiState.value.selectedServiceSlug)
            .whereEqualTo("city", locationId)
            .whereEqualTo("locality", subLocationId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BookPurohitVM", "attachPurohitsListener failed", error)
                    _uiState.update { it.copy(isLoading = false, error = "Query failed: ${error.message}") }
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { PurohitItem.fromDocument(it) } ?: emptyList()
                allPurohits = items
                _uiState.update { it.copy(purohits = items, isLoading = false) }
            }
        activeListeners.add(ListenerType.PUROHITS)
    }

    // ── Detach helpers ────────────────────────────────────────────

    private fun detachLocationsListener() {
        locationsListener?.remove()
        locationsListener = null
        activeListeners.remove(ListenerType.LOCATIONS)
        Log.d("BookPurohitVM", "detachLocationsListener: removed")
    }

    private fun detachSubLocationsListener() {
        subLocationsListener?.remove()
        subLocationsListener = null
        activeListeners.remove(ListenerType.SUBLOCATIONS)
        Log.d("BookPurohitVM", "detachSubLocationsListener: removed")
    }

    private fun detachPurohitsListener() {
        purohitsListener?.remove()
        purohitsListener = null
        activeListeners.remove(ListenerType.PUROHITS)
    }

    // ── Reset helpers ─────────────────────────────────────────────

    fun resetToServices() {
        detachLocationsListener()
        detachSubLocationsListener()
        detachPurohitsListener()
        _uiState.update {
            it.copy(
                locations = emptyList(), subLocations = emptyList(), purohits = emptyList(),
                searchQuery = "", selectedServiceSlug = "",
                currentLocationId = null, currentSubLocationId = null,
                services = allServices
            )
        }
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