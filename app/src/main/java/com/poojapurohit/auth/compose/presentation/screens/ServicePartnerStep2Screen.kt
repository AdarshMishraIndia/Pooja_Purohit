package com.poojapurohit.auth.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.PlacesAutocompleteField
import com.poojapurohit.auth.compose.presentation.components.WelcomeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun ServicePartnerStep2Screen(
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var city by remember { mutableStateOf(viewModel.formData.city) }
    var locality by remember { mutableStateOf(viewModel.formData.locality) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Viewport of the selected city — used to bias locality predictions
    var cityBounds by remember { mutableStateOf<RectangularBounds?>(null) }

    val fontScale = LocalDensity.current.fontScale
    val adaptiveTopSpacing = (200 / min(fontScale, 1.5f)).dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AuthTitle()

            Spacer(modifier = Modifier.height(adaptiveTopSpacing))

            WelcomeText()

            Spacer(modifier = Modifier.height(16.dp))

            // ── City ──────────────────────────────────────────────────────────
            PlacesAutocompleteField(
                label = "City",
                value = city,
                onValueChange = { input ->
                    city = input
                    viewModel.formData.city = input
                    errorMessage = null
                    // Manual edit — reset locality and bounds
                    locality = ""
                    viewModel.formData.locality = ""
                    cityBounds = null
                },
                onPlaceSelected = { placeId, displayName ->
                    city = displayName
                    viewModel.formData.city = displayName
                    locality = ""
                    viewModel.formData.locality = ""
                    cityBounds = null
                    errorMessage = null
                    // Fetch city viewport to bias locality predictions
                    if (Places.isInitialized()) {
                        scope.launch {
                            try {
                                val request = FetchPlaceRequest.newInstance(
                                    placeId,
                                    listOf(Place.Field.VIEWPORT)
                                )
                                Places.createClient(context)
                                    .fetchPlace(request)
                                    .addOnSuccessListener { result ->
                                        cityBounds = result.place.viewport as RectangularBounds?
                                    }
                            } catch (_: Exception) {
                                // Bounds unavailable — locality autocomplete still works unbiased
                            }
                        }
                    }
                },
                placeholder = "Enter your city",
                typesFilter = listOf("(cities)")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Locality ──────────────────────────────────────────────────────
            // No typesFilter — "(regions)" misses many Indian localities; unfiltered
            // with cityBounds bias surfaces neighbourhoods and sub-areas correctly.
            PlacesAutocompleteField(
                label = "Locality",
                value = locality,
                onValueChange = { input ->
                    locality = input
                    viewModel.formData.locality = input
                    errorMessage = null
                },
                onPlaceSelected = { _, displayName ->
                    locality = displayName
                    viewModel.formData.locality = displayName
                    errorMessage = null
                },
                placeholder = if (city.isBlank()) "Select a city first" else "Enter your locality / area",
                typesFilter = emptyList(),
                locationBias = cityBounds,
                enabled = city.isNotBlank()
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthButton(
                text = "Next",
                onClick = {
                    when {
                        city.isBlank() -> errorMessage = "Please select your city"
                        city.length < 2 -> errorMessage = "City name must be at least 2 characters"
                        locality.isBlank() -> errorMessage = "Please select your locality"
                        locality.length < 2 -> errorMessage = "Locality must be at least 2 characters"
                        else -> {
                            errorMessage = null
                            viewModel.nextStep(city = city, locality = locality)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        errorMessage?.let { error ->
            ErrorDialog(
                message = error,
                onDismiss = { errorMessage = null }
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(3000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⚠️", fontSize = 40.sp)

                Text(
                    text = message,
                    color = Color(0xFFB71C1C),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                AuthButton(
                    text = "OK",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}