package com.poojapurohit.bookpurohit.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
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

class BookPurohitViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(BookPurohitUiState())
    val uiState: StateFlow<BookPurohitUiState> = _uiState.asStateFlow()

    private var allLocations: List<LocationItem> = emptyList()
    private var allSubLocations: List<LocationItem> = emptyList()
    private var allPurohits: List<PurohitItem> = emptyList()

    init {
        loadLocations()
    }

    fun onEvent(event: BookPurohitEvent) {
        when (event) {
            is BookPurohitEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is BookPurohitEvent.LocationSelected -> loadSubLocations(event.locationId)
            is BookPurohitEvent.SubLocationSelected ->
                loadPurohits(event.locationId, event.subLocationId)
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (_uiState.value.currentLocationId == null) {
            filterLocations(query)
        } else {
            filterSubLocations(query)
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

    private fun loadLocations() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("locations").get().await()

                val locations = snapshot.documents.mapNotNull { doc ->
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

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load locations: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadSubLocations(locationId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                searchQuery = "",
                subLocations = emptyList(),
                currentLocationId = locationId
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("locations")
                    .document(locationId)
                    .collection("subLocations")
                    .get()
                    .await()

                val subLocations = snapshot.documents.mapNotNull { doc ->
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

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load sublocations: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetToLocations() {
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
        _uiState.update {
            it.copy(
                purohits = emptyList(),
                searchQuery = "",
                currentSubLocationId = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadPurohits(locationId: String, subLocationId: String) {
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

        viewModelScope.launch {
            try {
                // 1. Get the sublocation document
                val subLocationDoc = firestore
                    .collection("locations")
                    .document(locationId)
                    .collection("subLocations")
                    .document(subLocationId)
                    .get()
                    .await()

                // 2. Get the servicePartners array
                val servicePartners = subLocationDoc.get("servicePartners") as? List<String> 
                    ?: emptyList()

                if (servicePartners.isEmpty()) {
                    _uiState.update { it.copy(purohits = emptyList(), isLoading = false) }
                    return@launch
                }

                // 3. Query the users collection with document IDs from servicePartners array
                val purohitDocs = firestore
                    .collection("users")
                    .whereIn(FieldPath.documentId(), servicePartners)
                    .get()
                    .await()

                // 4. Map documents to PurohitItem objects
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
                        error = "Failed to load purohits: ${e.message}"
                    )
                }
            }
        }
    }
}
