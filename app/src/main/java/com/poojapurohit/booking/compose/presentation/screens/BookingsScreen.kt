package com.poojapurohit.booking.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.poojapurohit.booking.compose.BookingsEffect
import com.poojapurohit.booking.compose.BookingsViewModel
import com.poojapurohit.booking.compose.components.BookingCard
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import com.poojapurohit.dashboard.compose.theme.DeleteRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bookings screen — three tabs: Active / Cancelled / Completed.
 *
 * Works for both user and purohit roles. The VM merges both Firestore queries
 * (whereEqualTo userId + whereEqualTo purohitId) so each party sees only their
 * own bookings. Per-card role is determined via [BookingsViewModel.currentUserIsPurohitFor].
 *
 * @param highlightBookingId  bookingId from a deep link (poojapurohit://bookings/{id}).
 *                            Passed by BookingsActivity after parsing the Intent URI.
 * @param initialTabIndex     Default tab when no deep link active (0 = Active).
 */
@Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel = viewModel(),
    highlightBookingId: String? = null,
    initialTabIndex: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("Active", "Cancelled", "Completed")
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size }
    )

    // Pass deep link bookingId to VM once bookings are loaded
    LaunchedEffect(
        highlightBookingId,
        uiState.activeBookings,
        uiState.cancelledBookings,
        uiState.completedBookings
    ) {
        if (highlightBookingId != null) {
            viewModel.handleDeepLink(highlightBookingId)
        }
    }

    // Jump pager to the tab the VM resolved for this booking
    LaunchedEffect(uiState.requestedTabIndex, uiState.highlightedBookingId) {
        if (uiState.highlightedBookingId != null) {
            pagerState.animateScrollToPage(uiState.requestedTabIndex)
        }
    }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BookingsEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is BookingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Confirmation dialogs — guard all destructive / irreversible actions
    var pendingCancel by remember { mutableStateOf<Booking?>(null) }
    var pendingReject by remember { mutableStateOf<Booking?>(null) }
    var pendingComplete by remember { mutableStateOf<Booking?>(null) }

    // ── Cancel dialog ─────────────────────────────────────────────────────────
    pendingCancel?.let { booking ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = {
                Text(
                    text = "Cancel Booking?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel your booking for ${booking.serviceName}? This cannot be undone.",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelBooking(booking); pendingCancel = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                ) {
                    Text("Yes, Cancel", color = Color.White, fontFamily = FontFamily.Serif)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) {
                    Text("Keep", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    // ── Reject dialog ─────────────────────────────────────────────────────────
    pendingReject?.let { booking ->
        AlertDialog(
            onDismissRequest = { pendingReject = null },
            title = {
                Text(
                    text = "Reject Booking?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Reject the booking for ${booking.serviceName}? The user will be notified.",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.rejectBooking(booking); pendingReject = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                ) {
                    Text("Reject", color = Color.White, fontFamily = FontFamily.Serif)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReject = null }) {
                    Text("Cancel", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    // ── Mark as Completed dialog ──────────────────────────────────────────────
    pendingComplete?.let { booking ->
        AlertDialog(
            onDismissRequest = { pendingComplete = null },
            title = {
                Text(
                    text = "Mark as Completed?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Confirm that you have successfully completed the pooja for ${booking.serviceName}. This action cannot be undone.",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeBooking(booking); pendingComplete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkBrandOrange else BrandOrange
                    )
                ) {
                    Text("Yes, Completed", color = Color.White, fontFamily = FontFamily.Serif)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingComplete = null }) {
                    Text("Cancel", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(scaffoldPadding)
        ) {
            // ── Header banner ─────────────────────────────────────────────────
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

            // ── Tab row ───────────────────────────────────────────────────────
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (isDark) DarkBrandOrange else BrandOrange,
                indicator = {
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
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                fontFamily = FontFamily.Serif,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold
                                else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = if (isDark) DarkBrandOrange else BrandOrange
                    )
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
                        BookingList(
                            bookings = bookings,
                            highlightedBookingId = uiState.highlightedBookingId,
                            isPurohitViewFor = { booking ->
                                viewModel.currentUserIsPurohitFor(booking)
                            },
                            onCompletePayment = { booking ->
                                // TODO: launch Razorpay flow for booking.bookingId.
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Open payment flow for ${booking.bookingId}"
                                    )
                                }
                            },
                            onAccept = { booking -> viewModel.acceptBooking(booking) },
                            onReject = { booking -> pendingReject = booking },
                            onComplete = { booking -> pendingComplete = booking },
                            onCancel = { booking -> pendingCancel = booking },
                            onHighlightConsumed = { viewModel.clearHighlight() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingList(
    bookings: List<Booking>,
    highlightedBookingId: String?,
    isPurohitViewFor: (Booking) -> Boolean,
    onCompletePayment: (Booking) -> Unit,
    onAccept: (Booking) -> Unit,
    onReject: (Booking) -> Unit,
    onComplete: (Booking) -> Unit,
    onCancel: (Booking) -> Unit,
    onHighlightConsumed: () -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to highlighted card, then clear after 2 s
    LaunchedEffect(highlightedBookingId, bookings) {
        if (highlightedBookingId == null) return@LaunchedEffect
        val index = bookings.indexOfFirst { it.bookingId == highlightedBookingId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(2000L)
            onHighlightConsumed()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = bookings, key = { it.bookingId }) { booking ->
            val purohitView = isPurohitViewFor(booking)
            BookingCard(
                booking = booking,
                isHighlighted = booking.bookingId == highlightedBookingId,
                isPurohitView = purohitView,
                onCompletePayment = onCompletePayment,
                onAccept = onAccept,
                onReject = onReject,
                onComplete = onComplete,
                onCancel = onCancel
            )
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