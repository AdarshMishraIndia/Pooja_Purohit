package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Styled autocomplete field backed by the Places SDK 3.x API.
 *
 * TypeFilter was removed in SDK 3.x. Use [typesFilter] with place type
 * collection strings instead:
 *   - "(cities)"   → cities / towns
 *   - "(regions)"  → administrative regions
 *   - emptyList()  → no filter (surfaces neighbourhoods, localities, etc.)
 *
 * @param typesFilter     Passed directly to FindAutocompletePredictionsRequest.setTypesFilter().
 * @param locationBias    City viewport bounds to bias locality predictions.
 * @param enabled         False while a prerequisite field (e.g. city) is not yet filled.
 */
@Composable
fun PlacesAutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (placeId: String, displayName: String) -> Unit,
    placeholder: String,
    typesFilter: List<String> = emptyList(),
    locationBias: RectangularBounds? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var dropdownVisible by remember { mutableStateOf(false) }

    // Debounced autocomplete fetch
    LaunchedEffect(value, locationBias) {
        if (value.length < 2) {
            predictions = emptyList()
            dropdownVisible = false
            return@LaunchedEffect
        }
        delay(350)
        if (!Places.isInitialized()) return@LaunchedEffect
        try {
            val client = Places.createClient(context)
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(value)
                .setCountries("IN")
            if (typesFilter.isNotEmpty()) {
                requestBuilder.setTypesFilter(typesFilter)
            }
            locationBias?.let { requestBuilder.setLocationBias(it) }

            val response = client.findAutocompletePredictions(requestBuilder.build()).await()
            predictions = response.autocompletePredictions
            dropdownVisible = predictions.isNotEmpty()
        } catch (_: Exception) {
            predictions = emptyList()
            dropdownVisible = false
        }
    }

    Column(modifier = modifier) {

        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            thickness = 2.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(input)
                if (input.length >= 2) dropdownVisible = true
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) Color.White else Color(0xFFE0E0E0),
                    RoundedCornerShape(12.dp)
                ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFC3C2C2),
                    fontSize = 15.sp,
                    maxLines = 1
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF811C01),
                unfocusedBorderColor = Color(0xFFAAAAAA),
                disabledBorderColor = Color(0xFFCCCCCC),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFE0E0E0),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color(0xFF888888)
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        )

        if (dropdownVisible && predictions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    itemsIndexed(predictions) { index, prediction ->
                        TextButton(
                            onClick = {
                                val primary = prediction.getPrimaryText(null).toString()
                                onValueChange(primary)
                                onPlaceSelected(prediction.placeId, primary)
                                predictions = emptyList()
                                dropdownVisible = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = prediction.getPrimaryText(null).toString(),
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = prediction.getSecondaryText(null).toString(),
                                    color = Color(0xFF777777),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (index < predictions.lastIndex) {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}