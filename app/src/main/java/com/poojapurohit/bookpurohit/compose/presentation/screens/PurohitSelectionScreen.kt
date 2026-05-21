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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
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
import com.poojapurohit.bookpurohit.compose.model.PurohitItem
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitDecorOverlay
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitTopBar
import com.poojapurohit.bookpurohit.compose.presentation.util.highlightSearchQuery
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface

// Deep Crimson / Maroon
private val LightGradTop = Color(0xFFFFFDF8)
private val LightGradBottom = Color(0xFFFFE8CC)
private val DarkGradTop = Color(0xFF0E0E0E)
private val DarkGradBottom = Color(0xFF1C1208)

@Composable
fun PurohitSelectionScreen(
    viewModel: BookPurohitViewModel,
    locationId: String,
    subLocationId: String,
    onBackPressed: () -> Unit,
    onBookClick: (PurohitItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Rotation drives overlay symbol drift

    LaunchedEffect(locationId, subLocationId) {
        viewModel.onEvent(BookPurohitEvent.SubLocationSelected(locationId, subLocationId))
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearError() }
    }

    BackHandler { viewModel.resetToSubLocations(); onBackPressed() }

    Scaffold(
        topBar = {
            BookPurohitTopBar(
                bannerTitle = "Select Purohit",
                onBackPressed = { viewModel.resetToSubLocations(); onBackPressed() },
                isDark = isDark
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(DarkGradTop, DarkGradBottom)
                        else listOf(LightGradTop, LightGradBottom)
                    )
                )
                .padding(paddingValues)
        ) {
            BookPurohitDecorOverlay(
                accentColor = if (isDark) Color(0xFFFFD090) else BrandOrange,
                isDark = isDark
            )

            Column(modifier = Modifier.fillMaxSize()) {
                PurohitSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(BookPurohitEvent.SearchQueryChanged(it)) },
                    isDark = isDark
                )
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = if (isDark) DarkBrandOrange else BrandOrange)
                    }
                    uiState.purohits.isEmpty() -> PurohitEmptyState(
                        message = if (uiState.searchQuery.isBlank()) "No service partners available in this area"
                        else "No service partners found for \"${uiState.searchQuery}\""
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.purohits) { purohit ->
                            PurohitCard(purohit, uiState.searchQuery, onBookClick, isDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurohitSearchBar(query: String, onQueryChange: (String) -> Unit, isDark: Boolean) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        placeholder = { Text("Search by name or skills...", fontFamily = FontFamily.Serif, fontSize = 16.sp) },
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
private fun PurohitCard(purohit: PurohitItem, searchQuery: String, onBookClick: (PurohitItem) -> Unit, isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

            // Name
            Text(
                highlightSearchQuery(purohit.name, searchQuery, isDark),
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, lineHeight = 26.sp
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(12.dp))

            // Experience + Location in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("📅 ", fontSize = 14.sp)
                    Text(
                        "${purohit.experience} yrs",
                        fontFamily = FontFamily.Serif, fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(2f)) {
                    Text("📍 ", fontSize = 14.sp)
                    Text(
                        listOfNotNull(
                            purohit.locality.takeIf { it.isNotBlank() },
                            purohit.city.takeIf { it.isNotBlank() }
                        ).joinToString(", "),
                        fontFamily = FontFamily.Serif, fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) DarkBrandOrange else BrandOrange)
                    .clickable { onBookClick(purohit) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Book", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun PurohitEmptyState(message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🕉️", fontSize = 48.sp)
            Text(message, fontFamily = FontFamily.Serif, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}