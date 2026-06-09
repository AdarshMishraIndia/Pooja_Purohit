package com.poojapurohit.dashboard.compose.presentation.components

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dashboard-scoped Places autocomplete field.
 * Matches EditProfileScreen OutlinedTextField styling — brand-aware, dark/light theme.
 *
 * Key behaviour:
 * - Suggestions only appear after the user actively types (userHasTyped gate).
 * - suppressNextFetch prevents re-fetch when onValueChange is called programmatically
 *   from a selection tap.
 * - After selection, dropdown is dismissed and does not reappear until user types again.
 * - On initial load (Firestore value population), no fetch is triggered.
 *
 * @param typesFilter    Passed to FindAutocompletePredictionsRequest.setTypesFilter().
 * @param locationBias   City viewport bounds to bias locality predictions.
 * @param enabled        False while a prerequisite field (e.g. city) is not filled.
 * @param isError        Propagates error border colour from parent validation.
 */
@Composable
fun DashboardPlacesAutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (placeId: String, displayName: String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    typesFilter: List<String> = emptyList(),
    locationBias: RectangularBounds? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    isDark: Boolean = false
) {
    val context = LocalContext.current

    val placesClient: PlacesClient = remember(context) {
        if (!Places.isInitialized()) {
            val meta = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
            val key = meta?.getString("com.google.android.geo.API_KEY") ?: ""
            Places.initializeWithNewPlacesApiEnabled(context.applicationContext, key)
        }
        Places.createClient(context)
    }

    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var dropdownVisible by remember { mutableStateOf(false) }

    // Gates fetch — only true after the user physically types in the field.
    // Prevents Firestore-loaded values from triggering autocomplete on first render.
    var userHasTyped by remember { mutableStateOf(false) }

    // Set true before programmatic onValueChange in selection handler so the
    // LaunchedEffect re-trigger does not fire a network fetch.
    var suppressNextFetch by remember { mutableStateOf(false) }

    // Debounced fetch — only runs when user has actively typed
    LaunchedEffect(value, locationBias) {
        if (suppressNextFetch) {
            suppressNextFetch = false
            return@LaunchedEffect
        }
        if (!userHasTyped) {
            return@LaunchedEffect
        }
        if (value.length < 2) {
            predictions = emptyList()
            dropdownVisible = false
            return@LaunchedEffect
        }
        delay(350.milliseconds)
        try {
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(value)
                .setCountries("IN")
            if (typesFilter.isNotEmpty()) {
                requestBuilder.typesFilter = typesFilter
            }
            locationBias?.let { requestBuilder.setLocationBias(it) }

            val response = placesClient.findAutocompletePredictions(requestBuilder.build()).await()
            predictions = response.autocompletePredictions
            dropdownVisible = predictions.isNotEmpty()
        } catch (_: Exception) {
            predictions = emptyList()
            dropdownVisible = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                userHasTyped = true
                onValueChange(input)
                if (input.length >= 2) dropdownVisible = true
                else {
                    predictions = emptyList()
                    dropdownVisible = false
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            ),
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else BrandOrange,
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                disabledTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )

        if (dropdownVisible && predictions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    itemsIndexed(predictions) { index, prediction ->
                        TextButton(
                            onClick = {
                                val primary = prediction.getPrimaryText(null).toString()
                                // Suppress fetch triggered by programmatic value change
                                suppressNextFetch = true
                                // Reset typed flag so suggestions don't reappear
                                // until user types again
                                userHasTyped = false
                                onValueChange(primary)
                                onPlaceSelected(prediction.placeId, primary)
                                predictions = emptyList()
                                dropdownVisible = false
                                sessionToken = AutocompleteSessionToken.newInstance()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = prediction.getPrimaryText(null).toString(),
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = prediction.getSecondaryText(null).toString(),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.White.copy(alpha = 0.6f)
                                    else Color(0xFF777777)
                                )
                            }
                        }
                        if (index < predictions.lastIndex) {
                            HorizontalDivider(
                                color = if (isDark) Color.White.copy(alpha = 0.08f)
                                else Color(0xFFEEEEEE),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}