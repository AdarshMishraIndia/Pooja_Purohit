package com.poojapurohit.booking.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.booking.compose.BookingsViewModel
import com.poojapurohit.booking.compose.components.BookingCard
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientStart
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientStart

private val TABS = listOf("Active", "Cancelled", "Completed")

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val backgroundBrush = Brush.linearGradient(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isDark) DarkBrandOrange else BrandOrange
                )
            }

            uiState.error != null -> {
                Text(
                    text = "Failed to load bookings.\n${uiState.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    BookingsTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        counts = listOf(
                            uiState.activeBookings.size,
                            uiState.cancelledBookings.size,
                            uiState.completedBookings.size
                        )
                    )

                    val bookings = when (selectedTab) {
                        0 -> uiState.activeBookings
                        1 -> uiState.cancelledBookings
                        else -> uiState.completedBookings
                    }

                    BookingList(bookings = bookings)
                }
            }
        }
    }
}

@Composable
private fun BookingsTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    counts: List<Int>
) {
    val isDark = isSystemInDarkTheme()
    val activeColor = if (isDark) DarkBrandOrange else BrandOrange

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = activeColor,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = activeColor
            )
        }
    ) {
        TABS.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    val count = counts.getOrElse(index) { 0 }
                    val label = if (count > 0) "$title ($count)" else title
                    Text(
                        text = label,
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selectedTab == index) activeColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}

@Composable
private fun BookingList(bookings: List<Booking>) {
    if (bookings.isEmpty()) {
        EmptyBookingsPlaceholder()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = bookings,
            key = { it.bookingId }
        ) { booking ->
            BookingCard(booking = booking)
        }
    }
}

@Composable
private fun EmptyBookingsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Text(
                text = "No bookings here yet",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your bookings will appear once you book a Purohit",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
