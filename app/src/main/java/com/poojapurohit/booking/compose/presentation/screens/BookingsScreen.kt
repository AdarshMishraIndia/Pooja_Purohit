package com.poojapurohit.booking.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.booking.compose.BookingsViewModel
import com.poojapurohit.booking.compose.components.BookingCard
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.dashboard.compose.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val tabs = listOf("Active", "Cancelled", "Completed")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(DarkBrandOrange, DarkBrandRed)
                        else listOf(BrandOrange, BrandRed),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "My Bookings",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        // UPDATED: Using SecondaryTabRow instead of deprecated TabRow
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = if (isDark) DarkBrandOrange else BrandOrange,
            indicator = {
                // UPDATED: Using TabRowDefaults.SecondaryIndicator with the new tabIndicatorOffset
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    color = if (isDark) DarkBrandOrange else BrandOrange
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val bookings = when (pageIndex) {
                    0 -> uiState.activeBookings
                    1 -> uiState.cancelledBookings
                    2 -> uiState.completedBookings
                    else -> emptyList()
                }

                if (bookings.isEmpty()) {
                    EmptyBookingsPlaceholder()
                } else {
                    BookingList(bookings = bookings)
                }
            }
        }
    }
}

@Composable
private fun BookingList(bookings: List<Booking>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Text(
                text = "No bookings here yet",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}