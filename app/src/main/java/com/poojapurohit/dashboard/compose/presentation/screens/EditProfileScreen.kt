package com.poojapurohit.dashboard.compose.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.poojapurohit.dashboard.compose.EditProfileEffect
import com.poojapurohit.dashboard.compose.EditProfileEvent
import com.poojapurohit.dashboard.compose.EditProfileViewModel
import com.poojapurohit.dashboard.compose.presentation.components.DashboardPlacesAutocompleteField
import com.poojapurohit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackPressed: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    // City viewport — biases locality autocomplete predictions
    var cityBounds by remember { mutableStateOf<RectangularBounds?>(null) }

    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is EditProfileEffect.ShowToast -> {
                Toast.makeText(context, currentEffect.message, Toast.LENGTH_SHORT).show()
                viewModel.clearEffect()
            }
            is EditProfileEffect.NavigateBack -> {
                onBackPressed()
                viewModel.clearEffect()
            }
            null -> {}
        }
    }

    BackHandler(onBack = onBackPressed)

    Scaffold(
        topBar = {
            EditProfileTopBar(
                onBackPressed = onBackPressed,
                isDark = isDark
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                DarkBackgroundGradientStart,
                                DarkBackgroundGradientCenter,
                                DarkBackgroundGradientEnd
                            )
                        } else {
                            listOf(
                                LightBackgroundGradientStart,
                                LightBackgroundGradientCenter,
                                LightBackgroundGradientEnd
                            )
                        },
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Common fields ─────────────────────────────────────────
                    ProfileTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onEvent(EditProfileEvent.NameChanged(it)) },
                        label = "Full Name *",
                        placeholder = "Enter your full name",
                        error = uiState.nameError,
                        helperText = "Only letters and spaces allowed"
                    )

                    PhoneTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.onEvent(EditProfileEvent.PhoneChanged(it)) },
                        error = uiState.phoneError,
                        isDark = isDark
                    )

                    // ── Service partner exclusive fields ──────────────────────
                    if (uiState.isServicePartner) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (isDark) DarkBrandOrange else BrandRed
                        )

                        Text(
                            text = "Service Partner Information",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) DarkBrandOrange else BrandRed
                        )

                        // ── City — Places autocomplete ────────────────────────
                        DashboardPlacesAutocompleteField(
                            label = "City *",
                            value = uiState.city,
                            onValueChange = { input ->
                                viewModel.onEvent(EditProfileEvent.CityChanged(input))
                                // Manual edit — reset locality and bounds
                                viewModel.onEvent(EditProfileEvent.LocalityChanged(""))
                                cityBounds = null
                            },
                            onPlaceSelected = { placeId, displayName ->
                                viewModel.onEvent(EditProfileEvent.CityChanged(displayName))
                                viewModel.onEvent(EditProfileEvent.LocalityChanged(""))
                                cityBounds = null
                                // Fetch viewport to bias locality predictions
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
                                                    result.place.viewport?.let {
                                                        cityBounds = RectangularBounds.newInstance(it)
                                                    }
                                                }
                                        } catch (_: Exception) {
                                            // Bounds unavailable — locality autocomplete still works unbiased
                                        }
                                    }
                                }
                            },
                            placeholder = "Enter your city",
                            typesFilter = listOf("(cities)"),
                            isError = uiState.cityError != null,
                            isDark = isDark
                        )

                        if (uiState.cityError != null) {
                            Text(
                                text = uiState.cityError!!,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        // ── Locality — Places autocomplete ────────────────────
                        // No typesFilter — unfiltered + cityBounds bias surfaces
                        // Indian neighbourhoods and sub-areas correctly.
                        DashboardPlacesAutocompleteField(
                            label = "Locality *",
                            value = uiState.locality,
                            onValueChange = { input ->
                                viewModel.onEvent(EditProfileEvent.LocalityChanged(input))
                            },
                            onPlaceSelected = { _, displayName ->
                                viewModel.onEvent(EditProfileEvent.LocalityChanged(displayName))
                            },
                            placeholder = if (uiState.city.isBlank()) "Select a city first"
                            else "Enter your locality / area",
                            typesFilter = emptyList(),
                            locationBias = cityBounds,
                            enabled = uiState.city.isNotBlank(),
                            isError = uiState.localityError != null,
                            isDark = isDark
                        )

                        if (uiState.localityError != null) {
                            Text(
                                text = uiState.localityError!!,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        // ── Specialization ────────────────────────────────────
                        SkillsSelectionList(
                            availableSkills = uiState.availableSkills,
                            selectedSkills = uiState.selectedSkills,
                            onSkillToggled = { viewModel.onEvent(EditProfileEvent.SkillToggled(it)) },
                            error = uiState.skillsError,
                            isDark = isDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Save button ───────────────────────────────────────────
                    Button(
                        onClick = { viewModel.onEvent(EditProfileEvent.SaveProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) DarkBrandOrange else BrandRed
                        ),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                                Text(
                                    text = "Saving...",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Save Changes",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "* Required fields",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(
    onBackPressed: () -> Unit,
    isDark: Boolean
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "POOJA PUROHIT (ପୂଜା ପୁରୋହିତ)",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(DarkBrandOrange, DarkBrandRed)
                    else listOf(BrandOrange, BrandRed),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(DarkWelcomeBannerStart, DarkWelcomeBannerEnd)
                        else listOf(WelcomeBannerStart, WelcomeBannerEnd)
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Edit Profile",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Phone Number *",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            prefix = {
                Text(
                    text = "+91 ",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.7f)
                    else Color.Black.copy(alpha = 0.7f)
                )
            },
            placeholder = {
                Text(text = "10 digit mobile number", fontFamily = FontFamily.Serif, fontSize = 14.sp)
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(text = error, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = "10 digits only (without +91)",
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else BrandOrange,
                unfocusedBorderColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = placeholder, fontFamily = FontFamily.Serif, fontSize = 14.sp)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            ),
            minLines = minLines,
            maxLines = maxLines,
            singleLine = minLines == 1 && maxLines == 1,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(text = error, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.error)
                } else if (helperText != null) {
                    Text(
                        text = helperText,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else BrandOrange,
                unfocusedBorderColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

@Composable
private fun SkillsSelectionList(
    availableSkills: List<String>,
    selectedSkills: List<String>,
    onSkillToggled: (String) -> Unit,
    error: String?,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Specialization *",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Tap to select/deselect services",
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurface else Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (availableSkills.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No skills available. Please check your connection.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    availableSkills.forEachIndexed { index, skill ->
                        ServiceListItem(
                            service = skill,
                            isSelected = selectedSkills.contains(skill),
                            onToggle = { onSkillToggled(skill) },
                            isDark = isDark
                        )
                        if (index < availableSkills.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (selectedSkills.isNotEmpty()) {
            Text(
                text = "${selectedSkills.size} skill(s) selected",
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = if (isDark) DarkBrandOrange else BrandRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ServiceListItem(
    service: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = service,
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        if (isDark) DarkBrandOrange else BrandOrange
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) {
                        if (isDark) DarkBrandOrange else BrandOrange
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}