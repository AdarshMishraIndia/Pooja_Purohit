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
import androidx.compose.ui.draw.clip
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
import com.poojapurohit.bookpurohit.compose.model.PurohitItem
import com.poojapurohit.dashboard.compose.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(locationId, subLocationId) {
        viewModel.onEvent(
            BookPurohitEvent.SubLocationSelected(locationId, subLocationId)
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    BackHandler {
        viewModel.resetToSubLocations()
        onBackPressed()
    }

    Scaffold(
        topBar = {
            PurohitTopBar(
                onBackPressed = {
                    viewModel.resetToSubLocations()
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
            Column(modifier = Modifier.fillMaxSize()) {

                PurohitSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(BookPurohitEvent.SearchQueryChanged(it)) },
                    isDark = isDark
                )

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

                    uiState.purohits.isEmpty() -> {
                        PurohitEmptyState(
                            message = if (uiState.searchQuery.isBlank()) {
                                "No service partners available in this area"
                            } else {
                                "No service partners found for \"${uiState.searchQuery}\""
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.purohits) { purohit ->
                                PurohitCard(
                                    purohit = purohit,
                                    searchQuery = uiState.searchQuery,
                                    onBookClick = onBookClick,
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
private fun PurohitSearchBar(
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
                text = "Search by name or skills...",
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
private fun PurohitCard(
    purohit: PurohitItem,
    searchQuery: String,
    onBookClick: (PurohitItem) -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Name
            Text(
                text = highlightSearchQuery(
                    text = purohit.name,
                    query = searchQuery,
                    isDark = isDark
                ),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Proficiency
            if (purohit.proficiency.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "🕉️ ", fontSize = 16.sp)
                    Text(
                        text = highlightSearchQuery(
                            text = purohit.proficiency.joinToString(", "),
                            query = searchQuery,
                            isDark = isDark
                        ),
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Experience
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅 ", fontSize = 14.sp)
                Text(
                    text = "${purohit.experience} years of experience",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.7f)
                    else Color.Black.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.1f)
                else Color.Black.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Book button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) DarkBrandOrange else BrandOrange)
                    .clickable { onBookClick(purohit) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Book",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
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
            withStyle(style = SpanStyle(color = if (isDark) Color.White else Color.Black)) {
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
                withStyle(style = SpanStyle(color = if (isDark) Color.White else Color.Black)) {
                    append(text.substring(lastIndex))
                }
                break
            }

            if (index > lastIndex) {
                withStyle(style = SpanStyle(color = if (isDark) Color.White else Color.Black)) {
                    append(text.substring(lastIndex, index))
                }
            }

            withStyle(
                style = SpanStyle(
                    color = if (isDark) DarkBrandOrange else BrandOrange,
                    fontWeight = FontWeight.ExtraBold,
                    background = if (isDark) DarkBrandOrange.copy(alpha = 0.2f)
                    else BrandOrange.copy(alpha = 0.15f)
                )
            ) {
                append(text.substring(index, index + query.length))
            }

            lastIndex = index + query.length
        }
    }
}

@Composable
private fun PurohitEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🕉️", fontSize = 48.sp)
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
private fun PurohitTopBar(
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
                text = "Select Purohit",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange,
                textAlign = TextAlign.Center
            )
        }
    }
}