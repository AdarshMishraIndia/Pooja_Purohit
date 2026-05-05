package com.poojapurohit.bookpurohit.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Full-screen dialogue for picking a map pin.
 *
 * Features:
 *  - Debounced Places autocomplete search (350ms) with full-text suggestions
 *  - Camera animates to selected place and auto-drops pin
 *  - Map layer toggle: Street / Satellite (Hybrid)
 *  - Tap-to-pin fallback
 *
 * Prerequisites:
 *  1. build.gradle: implementation("com.google.android.libraries.places:places:4.1.0")
 *  2. Application.onCreate(): Places.initializeWithNewPlacesApiEnabled(this, MAPS_API_KEY)
 *  3. AndroidManifest.xml: android:name=".YourApplication" on <application> tag
 *  4. Google Cloud Console: Maps SDK for Android + Places API (New) both enabled
 */
@Composable
fun MapPinPickerScreen(
    initialPin: LatLng?,
    isDark: Boolean,
    onConfirm: (LatLng) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Camera & pin ──────────────────────────────────────────────────────────
    val defaultPosition = LatLng(20.2961, 85.8245) // Bhubaneswar
    val startPosition = initialPin ?: defaultPosition

    var pinPosition by remember { mutableStateOf(initialPin) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            startPosition,
            if (initialPin != null) 16f else 12f
        )
    }

    // ── Map layer ─────────────────────────────────────────────────────────────
    // remember(mapType) — new MapProperties only when type actually changes,
    // preventing spurious Google Maps recompositions that break layer switching.
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val mapProperties = remember(mapType) {
        MapProperties(isMyLocationEnabled = false, mapType = mapType)
    }
    val mapUiSettings = remember {
        MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
    }

    // ── Places client ─────────────────────────────────────────────────────────
    // Initialisation guard: Application.onCreate() should call
    // Places.initializeWithNewPlacesApiEnabled() before this screen is shown.
    // The fallback here handles edge cases (process restart, test environments).
    val placesClient = remember(context) {
        if (!Places.isInitialized()) {
            val meta = context.packageManager
                .getApplicationInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_META_DATA
                ).metaData
            val key = meta?.getString("com.google.android.geo.API_KEY") ?: ""
            // Use context.applicationContext — not bare applicationContext,
            // which is unavailable outside Activity/Application scope.
            Places.initializeWithNewPlacesApiEnabled(context.applicationContext, key)
        }
        Places.createClient(context)
    }

    // Session token groups autocomplete + fetch into one billing event.
    // Rotated after each completed place selection.
    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }

    // ── Search state ──────────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Debounced autocomplete — cancels and restarts on every keystroke,
    // only fires the network call 350ms after the user stops typing.
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }
        delay(350L)
        isSearching = true
        searchError = null
        try {
            val req = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(searchQuery)
                .build()
            val res = placesClient.findAutocompletePredictions(req).await()
            // getFullText: "Place Name, City, Country" — unambiguous vs primaryText
            suggestions = res.autocompletePredictions.map { p ->
                p.placeId to p.getFullText(null).toString()
            }
            showSuggestions = suggestions.isNotEmpty()
        } catch (e: Exception) {
            suggestions = emptyList()
            showSuggestions = false
            android.util.Log.e("MapPinPicker", "Autocomplete failed", e)
            searchError = e.message ?: "Search unavailable"
        } finally {
            isSearching = false
        }
    }

    // Fetches LatLng for selected prediction, animates camera, drops pin.
    fun onSuggestionSelected(placeId: String, displayText: String) {
        searchQuery = displayText
        showSuggestions = false
        scope.launch {
            isSearching = true
            try {
                // Places SDK 4.x: LAT_LNG was renamed to LOCATION
                val req = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.LOCATION))
                val res = placesClient.fetchPlace(req).await()
                res.place.location?.let { latLng ->
                    pinPosition = latLng
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                        durationMs = 600
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MapPinPicker", "FetchPlace failed", e)
                // Silent to user — they can still tap the map manually
            } finally {
                isSearching = false
                // Rotate token after each completed autocomplete+fetch pair
                sessionToken = AutocompleteSessionToken.newInstance()
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF1A1A1A) else Color.White)
        ) {

            // Map — renders full screen behind all overlays
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = { latLng ->
                    pinPosition = latLng
                    showSuggestions = false
                }
            ) {
                pinPosition?.let { pos ->
                    val markerState = remember(pos) { MarkerState(position = pos) }
                    Marker(state = markerState, title = "Booking Location")
                }
            }

            // Top overlay: instruction banner + search field + suggestion dropdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Instruction banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.85f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search a place or tap the map to pin your location",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) DarkBrandOrange else BrandOrange
                    )
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search places…", fontSize = 14.sp, color = Color.Gray)
                    },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) DarkBrandOrange else BrandOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDark) DarkBrandOrange else BrandOrange
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange,
                        unfocusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = if (isDark) Color(0xFF2A2A2A) else Color.White,
                        focusedContainerColor = if (isDark) Color(0xFF2A2A2A) else Color.White
                    )
                )

                // Inline error — shows actual API error message for easier debugging
                searchError?.let { error ->
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                    )
                }

                // Suggestion dropdown — capped at 220dp, scrollable
                if (showSuggestions && suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF2A2A2A) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.Gray.copy(alpha = 0.3f)
                                else Color.LightGray,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        items(suggestions, key = { it.first }) { (placeId, fullText) ->
                            Text(
                                text = fullText,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionSelected(placeId, fullText) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            )
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                        }
                    }
                }
            }

            // Layer toggle — bottom-left, above action row
            MapLayerToggle(
                currentType = mapType,
                isDark = isDark,
                onTypeSelected = { mapType = it },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 90.dp)
            )

            // Bottom action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.8f)
                        else Color.White.copy(alpha = 0.9f)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                val pinReady = pinPosition != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (pinReady) Color(0xFF4CAF50)
                            else Color.Gray.copy(alpha = 0.4f)
                        )
                        .then(
                            if (pinReady) Modifier.clickable { onConfirm(pinPosition!!) }
                            else Modifier
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pinReady) "Confirm Location" else "Tap map first",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── Layer toggle pill ─────────────────────────────────────────────────────────

private data class LayerOption(val type: MapType, val label: String)

private val LAYER_OPTIONS = listOf(
    LayerOption(MapType.NORMAL, "Street"),
    LayerOption(MapType.HYBRID, "Satellite") // HYBRID = satellite imagery + road/label overlay
)

@Composable
private fun MapLayerToggle(
    currentType: MapType,
    isDark: Boolean,
    onTypeSelected: (MapType) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isDark) DarkBrandOrange else BrandOrange
    val pillBg = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.92f)
    else Color.White.copy(alpha = 0.92f)
    val inactiveText = if (isDark) Color.LightGray else Color.DarkGray

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(pillBg)
            .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LAYER_OPTIONS.forEach { (type, label) ->
            val isActive = currentType == type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) activeColor else Color.Transparent)
                    .clickable { onTypeSelected(type) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color.White else inactiveText
                )
            }
        }
    }
}