package com.poojapurohit.bookpurohit.compose.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poojapurohit.bookpurohit.compose.BookPurohitEvent
import com.poojapurohit.bookpurohit.compose.BookPurohitViewModel
import com.poojapurohit.bookpurohit.compose.LocationItem
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitDecorOverlay
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitTopBar
import com.poojapurohit.bookpurohit.compose.presentation.util.highlightSearchQuery
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface

// Orange / Amber — warmth of place
private val LightBgTop    = Color(0xFFFFF3E0)
private val LightBgMid    = Color(0xFFFFB74D)   // mandala accent light
private val LightBgBottom = Color(0xFFE65100)
private val DarkBgTop     = Color(0xFF1A0800)
private val DarkBgMid     = Color(0xFFE07820)   // mandala accent dark
private val DarkBgBottom  = Color(0xFF6E2200)

@Composable
fun LocationSelectionScreen(
    viewModel: BookPurohitViewModel,
    serviceSlug: String,
    onBackPressed: () -> Unit,
    onLocationClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val infiniteTransition = rememberInfiniteTransition(label = "locationBg")

    // Gradient sweeps top → bottom with horizontal drift
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "locationSweep"
    )

    // Mandala rotation — 25 s / revolution, starts at 45° offset vs Service screen
    val mandalaRotation by infiniteTransition.animateFloat(
        initialValue = 45f, targetValue = 405f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "locationMandala"
    )

    LaunchedEffect(serviceSlug) { viewModel.attachLocationsListener(serviceSlug) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearError() }
    }

    BackHandler(onBack = onBackPressed)

    Scaffold(
        topBar = { BookPurohitTopBar(bannerTitle = "Select Location", onBackPressed = onBackPressed, isDark = isDark) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(DarkBgTop, DarkBgMid, DarkBgBottom)
                        else listOf(LightBgTop, LightBgMid, LightBgBottom),
                        start = Offset(200f + sweep * 400f, 0f),
                        end = Offset(600f - sweep * 200f, 2000f)
                    )
                )
                .padding(paddingValues)
        ) {
            BookPurohitDecorOverlay(
                mandalaColor = if (isDark) DarkBgMid else LightBgMid,
                rotationDegrees = mandalaRotation,
                isDark = isDark
            )

            Column(modifier = Modifier.fillMaxSize()) {
                LocationSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(BookPurohitEvent.SearchQueryChanged(it)) },
                    isDark = isDark
                )
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = if (isDark) DarkBrandOrange else BrandOrange)
                    }
                    uiState.locations.isEmpty() -> LocationEmptyState(
                        message = if (uiState.searchQuery.isBlank()) "No locations available"
                        else "No locations found for \"${uiState.searchQuery}\""
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.locations) { location ->
                            LocationCard(location, uiState.searchQuery, { onLocationClick(location.id) }, isDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSearchBar(query: String, onQueryChange: (String) -> Unit, isDark: Boolean) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        placeholder = { Text("Search Locations", fontFamily = FontFamily.Serif, fontSize = 16.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = if (isDark) DarkBrandOrange else BrandOrange) },
        singleLine = true, shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = if (isDark) DarkBrandOrange else BrandOrange
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp)
    )
}

@Composable
private fun LocationCard(location: LocationItem, searchQuery: String, onClick: () -> Unit, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(highlightSearchQuery(location.name, searchQuery, isDark),
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${location.count} service partner${if (location.count != 1) "s" else ""} available",
                    fontFamily = FontFamily.Serif, fontSize = 14.sp,
                    color = if (isDark) DarkBrandOrange else BrandRed
                )
            }
            Text("›", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = if (isDark) DarkBrandOrange else BrandOrange)
        }
    }
}

@Composable
private fun LocationEmptyState(message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📍", fontSize = 48.sp)
            Text(message, fontFamily = FontFamily.Serif, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}
