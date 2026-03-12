package com.poojapurohit.bookpurohit.compose.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poojapurohit.bookpurohit.compose.BookPurohitEvent
import com.poojapurohit.bookpurohit.compose.BookPurohitViewModel
import com.poojapurohit.bookpurohit.compose.LocationItem
import com.poojapurohit.dashboard.compose.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    viewModel: BookPurohitViewModel,
    onBackPressed: () -> Unit,
    onLocationClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    BackHandler(onBack = onBackPressed)

    Scaffold(
        topBar = {
            BookPurohitTopBar(
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search bar
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(BookPurohitEvent.SearchQueryChanged(it)) },
                    isDark = isDark
                )

                // Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = if (isDark) DarkBrandOrange else BrandOrange
                            )
                        }
                    }
                    uiState.locations.isEmpty() -> {
                        EmptyState(
                            message = if (uiState.searchQuery.isBlank()) {
                                "No locations available"
                            } else {
                                "No locations found for \"${uiState.searchQuery}\""
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.locations) { location ->
                                LocationCard(
                                    location = location,
                                    searchQuery = uiState.searchQuery,
                                    onClick = { onLocationClick(location.id) },
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = {
            Text(
                text = "Search Locations",
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isDark) DarkBrandOrange else BrandOrange
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = if (isDark) DarkBrandOrange else BrandOrange
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp
        )
    )
}

@Composable
private fun LocationCard(
    location: LocationItem,
    searchQuery: String,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = highlightSearchQuery(
                        text = location.name,
                        query = searchQuery,
                        isDark = isDark
                    ),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${location.count} service partner${if (location.count != 1) "s" else ""} available",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = if (isDark) DarkBrandOrange else BrandRed
                )
            }

            // Arrow indicator
            Text(
                text = "›",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkBrandOrange else BrandOrange
            )
        }
    }
}

@Composable
private fun highlightSearchQuery(
    text: String,
    query: String,
    isDark: Boolean
): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = if (isDark) Color.White else Color.Black
                )
            ) {
                append(text)
            }
        }
    }

    return buildAnnotatedString {
        val lowercaseText = text.lowercase()
        val lowercaseQuery = query.lowercase()
        var lastIndex = 0

        while (lastIndex < text.length) {
            val index = lowercaseText.indexOf(lowercaseQuery, lastIndex)

            if (index == -1) {
                // No more matches, append the rest
                withStyle(
                    style = SpanStyle(
                        color = if (isDark) Color.White else Color.Black
                    )
                ) {
                    append(text.substring(lastIndex))
                }
                break
            }

            // Append text before match
            if (index > lastIndex) {
                withStyle(
                    style = SpanStyle(
                        color = if (isDark) Color.White else Color.Black
                    )
                ) {
                    append(text.substring(lastIndex, index))
                }
            }

            // Append highlighted match
            withStyle(
                style = SpanStyle(
                    color = if (isDark) DarkBrandOrange else BrandOrange,
                    fontWeight = FontWeight.ExtraBold,
                    background = if (isDark) {
                        DarkBrandOrange.copy(alpha = 0.2f)
                    } else {
                        BrandOrange.copy(alpha = 0.15f)
                    }
                )
            ) {
                append(text.substring(index, index + query.length))
            }

            lastIndex = index + query.length
        }
    }
}

@Composable
private fun EmptyState(
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📍",
                fontSize = 48.sp
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPurohitTopBar(
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(DarkBrandOrange, DarkBrandRed)
                    } else {
                        listOf(BrandOrange, BrandRed)
                    },
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
        )

        // Page title banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(DarkWelcomeBannerStart, DarkWelcomeBannerEnd)
                        } else {
                            listOf(WelcomeBannerStart, WelcomeBannerEnd)
                        }
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select Location",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange,
                textAlign = TextAlign.Center
            )
        }
    }
}