package com.poojapurohit.bookpurohit.compose.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitTopBar
import com.poojapurohit.bookpurohit.compose.presentation.util.highlightSearchQuery
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientStart
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientStart

@Composable
fun SubLocationSelectionScreen(
    viewModel: BookPurohitViewModel,
    locationId: String,
    onBackPressed: () -> Unit,
    onSubLocationClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(locationId) {
        viewModel.attachSubLocationsListener(locationId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    BackHandler {
        viewModel.resetToLocations()
        onBackPressed()
    }

    Scaffold(
        topBar = {
            BookPurohitTopBar(
                bannerTitle = "Select Area",
                onBackPressed = {
                    viewModel.resetToLocations()
                    onBackPressed()
                },
                isDark = isDark
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(
                            DarkBackgroundGradientStart,
                            DarkBackgroundGradientCenter,
                            DarkBackgroundGradientEnd
                        ) else listOf(
                            LightBackgroundGradientStart,
                            LightBackgroundGradientCenter,
                            LightBackgroundGradientEnd
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SubLocationSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(BookPurohitEvent.SearchQueryChanged(it)) },
                    isDark = isDark
                )

                when {
                    uiState.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = if (isDark) DarkBrandOrange else BrandOrange)
                    }

                    uiState.subLocations.isEmpty() -> SubLocationEmptyState(
                        message = if (uiState.searchQuery.isBlank()) "No areas available in this location"
                        else "No areas found for \"${uiState.searchQuery}\""
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.subLocations) { subLocation ->
                            SubLocationCard(
                                subLocation = subLocation,
                                searchQuery = uiState.searchQuery,
                                onClick = { onSubLocationClick(locationId, subLocation.id) },
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubLocationSearchBar(query: String, onQueryChange: (String) -> Unit, isDark: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        placeholder = {
            Text("Search areas...", fontFamily = FontFamily.Serif, fontSize = 16.sp)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search",
                tint = if (isDark) DarkBrandOrange else BrandOrange)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = if (isDark) DarkBrandOrange else BrandOrange
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp)
    )
}

@Composable
private fun SubLocationCard(subLocation: LocationItem, searchQuery: String, onClick: () -> Unit, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightSearchQuery(subLocation.name, searchQuery, isDark),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${subLocation.count} service partner${if (subLocation.count != 1) "s" else ""} available",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = if (isDark) DarkBrandOrange else BrandRed
                )
            }
            Text("›", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = if (isDark) DarkBrandOrange else BrandOrange)
        }
    }
}

@Composable
private fun SubLocationEmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🏘️", fontSize = 48.sp)
            Text(
                text = message,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
