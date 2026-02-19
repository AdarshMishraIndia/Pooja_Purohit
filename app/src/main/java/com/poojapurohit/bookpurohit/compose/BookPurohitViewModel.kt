package com.poojapurohit.bookpurohit.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.poojapurohit.bookpurohit.compose.model.PurohitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    // Cached data for search filtering
    private var allLocations: List<LocationItem> = emptyList()
    private var allSubLocations: List<LocationItem> = emptyList()
    private var allPurohits: List<PurohitItem> = emptyList()

    // Listener management
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
        locationsListener = null
        subLocationsListener = null
        purohitsListener = null
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
        val filtered = if (query.isBlank()) {
            allLocations
        } else {
            allLocations.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(locations = filtered) }
    }

    private fun filterSubLocations(query: String) {
        val filtered = if (query.isBlank()) {
            allSubLocations
        } else {
            allSubLocations.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(subLocations = filtered) }
    }

    private fun filterPurohits(query: String) {
        val filtered = if (query.isBlank()) {
            allPurohits
        } else {
            allPurohits.filter { purohit ->
                purohit.name.contains(query, ignoreCase = true) ||
                        purohit.proficiency.any { skill -> skill.contains(query, ignoreCase = true) }
            }
        }
        _uiState.update { it.copy(purohits = filtered) }
    }

    // REALTIME LISTENERS

    fun attachLocationsListener() {
        if (activeListeners.contains(ListenerType.LOCATIONS)) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        locationsListener = firestore.collection("locations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load locations: ${error.message}"
                        )
                    }
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val locations = querySnapshot.documents.mapNotNull { doc ->
                        LocationItem(
                            id = doc.id,
                            name = doc.getString("name") ?: doc.id,
                            count = doc.getLong("count")?.toInt() ?: 0
                        )
                    }.sortedBy { it.name }

                    allLocations = locations
                    _uiState.update {
                        it.copy(
                            locations = locations,
                            isLoading = false,
                            currentLocationId = null
                        )
                    }
                }
            }

        activeListeners.add(ListenerType.LOCATIONS)
    }

    fun attachSubLocationsListener(locationId: String) {
        // Clean up previous sublocation listener
        detachSubLocationsListener()
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                searchQuery = "",
                subLocations = emptyList(),
                purohits = emptyList(),
                currentLocationId = locationId,
                currentSubLocationId = null
            )
        }

        subLocationsListener = firestore
            .collection("locations")
            .document(locationId)
            .collection("subLocations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load areas: ${error.message}"
                        )
                    }
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val subLocations = querySnapshot.documents.mapNotNull { doc ->
                        LocationItem(
                            id = doc.id,
                            name = doc.getString("name") ?: doc.id,
                            count = doc.getLong("count")?.toInt() ?: 0
                        )
                    }.sortedBy { it.name }

                    allSubLocations = subLocations
                    _uiState.update {
                        it.copy(
                            subLocations = subLocations,
                            isLoading = false
                        )
                    }
                }
            }

        activeListeners.add(ListenerType.SUBLOCATIONS)
    }

    fun attachPurohitsListener(locationId: String, subLocationId: String) {
        // Clean up previous purohit listener
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                searchQuery = "",
                purohits = emptyList(),
                currentLocationId = locationId,
                currentSubLocationId = subLocationId
            )
        }

        // Listen to sublocation document for servicePartners array changes
        purohitsListener = firestore
            .collection("locations")
            .document(locationId)
            .collection("subLocations")
            .document(subLocationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load service partners: ${error.message}"
                        )
                    }
                    return@addSnapshotListener
                }

                snapshot?.let { docSnapshot ->
                    viewModelScope.launch {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val servicePartners = (docSnapshot.get("servicePartners") as? List<*>)
                                ?.filterIsInstance<String>()
                                ?: emptyList()

                            if (servicePartners.isEmpty()) {
                                allPurohits = emptyList()
                                _uiState.update { it.copy(purohits = emptyList(), isLoading = false) }
                                return@launch
                            }

                            // Fetch user documents (this could also use listeners for individual users)
                            val purohitDocs = firestore
                                .collection("users")
                                .whereIn(FieldPath.documentId(), servicePartners)
                                .get()
                                .await()

                            val purohits = purohitDocs.documents.map { doc ->
                                PurohitItem.fromDocument(doc)
                            }.sortedBy { it.name }

                            allPurohits = purohits
                            _uiState.update {
                                it.copy(
                                    purohits = purohits,
                                    isLoading = false
                                )
                            }

                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to load service partners: ${e.message}"
                                )
                            }
                        }
                    }
                }
            }

        activeListeners.add(ListenerType.PUROHITS)
    }

    // LISTENER CLEANUP

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

    // NAVIGATION RESETS

    fun resetToLocations() {
        detachSubLocationsListener()
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                subLocations = emptyList(),
                purohits = emptyList(),
                searchQuery = "",
                currentLocationId = null,
                currentSubLocationId = null,
                locations = allLocations
            )
        }
    }

    fun resetToSubLocations() {
        detachPurohitsListener()

        _uiState.update {
            it.copy(
                purohits = emptyList(),
                searchQuery = "",
                currentSubLocationId = null,
                subLocations = allSubLocations
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}