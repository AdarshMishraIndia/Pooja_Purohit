package com.poojapurohit.booking.compose.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.poojapurohit.booking.compose.BookingsEffect
import com.poojapurohit.booking.compose.BookingsUiState
import com.poojapurohit.booking.compose.BookingsViewModel
import com.poojapurohit.booking.compose.components.BookingCard
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingStatus
import com.poojapurohit.bookpurohit.compose.presentation.screens.RazorpayStubDialog
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import com.poojapurohit.dashboard.compose.theme.DeleteRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── List item model ───────────────────────────────────────────────────────────

private sealed interface BookingListItem {
    data class Header(val label: String) : BookingListItem
    data class Card(val booking: Booking) : BookingListItem
}

private fun groupByMonth(bookings: List<Booking>): List<BookingListItem> {
    val fmt    = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val result = mutableListOf<BookingListItem>()
    var lastLabel: String? = null
    for (booking in bookings) {
        val label = booking.createdAt?.toDate()?.let { fmt.format(it) } ?: fmt.format(Date())
        if (label != lastLabel) {
            result += BookingListItem.Header(label)
            lastLabel = label
        }
        result += BookingListItem.Card(booking)
    }
    return result
}

// ── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel         : BookingsViewModel = hiltViewModel(),
    highlightBookingId: String?           = null,
    initialTabIndex   : Int               = 0
) {
    val uiState      by viewModel.uiState.collectAsState()
    val isDark        = isSystemInDarkTheme()
    val scope         = rememberCoroutineScope()
    val snackbarHost  = remember { SnackbarHostState() }
    val tabs          = listOf("Active", "Cancelled", "Completed")
    val pagerState    = rememberPagerState(initialPage = initialTabIndex, pageCount = { 3 })

    // Store the ID, not the object — detail screen always gets the freshest Booking from uiState.
    var selectedBookingId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedBooking: Booking? = remember(
        selectedBookingId,
        uiState.activeBookings,
        uiState.cancelledBookings,
        uiState.completedBookings
    ) {
        selectedBookingId?.let { id ->
            (uiState.activeBookings + uiState.cancelledBookings + uiState.completedBookings)
                .firstOrNull { it.bookingId == id }
        }
    }

    // ── Deep link ─────────────────────────────────────────────────────────────
    LaunchedEffect(
        highlightBookingId,
        uiState.activeBookings,
        uiState.cancelledBookings,
        uiState.completedBookings
    ) {
        if (!highlightBookingId.isNullOrBlank()) viewModel.handleDeepLink(highlightBookingId)
    }

    LaunchedEffect(uiState.requestedTabIndex, uiState.highlightedBookingId) {
        if (uiState.highlightedBookingId != null) {
            pagerState.animateScrollToPage(uiState.requestedTabIndex)
        }
    }

    LaunchedEffect(uiState.highlightedBookingId, uiState.activeBookings,
        uiState.cancelledBookings, uiState.completedBookings) {
        val id = uiState.highlightedBookingId ?: return@LaunchedEffect
        val all = uiState.activeBookings + uiState.cancelledBookings + uiState.completedBookings
        if (all.any { it.bookingId == id }) {
            selectedBookingId = id
        }
    }

    // ── One-shot effects ──────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BookingsEffect.ShowToast    -> snackbarHost.showSnackbar(effect.message)
                is BookingsEffect.ShowSnackbar -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var pendingCancel         by remember { mutableStateOf<Booking?>(null) }
    var pendingReject         by remember { mutableStateOf<Booking?>(null) }
    var pendingPaymentBooking by remember { mutableStateOf<Booking?>(null) }
    // ── Back: dismiss detail before popping activity ──────────────────────────
    BackHandler(enabled = selectedBookingId != null) { selectedBookingId = null }

    // ── Action callbacks — single definition, shared by list + detail ──────────
    val onCompletePayment: (Booking) -> Unit = { pendingPaymentBooking = it }
    val onAccept:          (Booking) -> Unit = { viewModel.acceptBooking(it) }
    val onReject:          (Booking) -> Unit = { pendingReject = it }
    val onComplete:        (Booking) -> Unit = { viewModel.initiateCompletion(it) }
    val onCancel:          (Booking) -> Unit = { pendingCancel = it }
    val onEditBooking: (Booking, String, Timestamp, com.poojapurohit.booking.model.Coordinates?) -> Unit =
        { booking, addr, date, coords -> viewModel.updateBookingAddressAndTime(booking, addr, date, coords) }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    BookingDialogs(
        isDark                = isDark,
        pendingPaymentBooking = pendingPaymentBooking,
        pendingCancel         = pendingCancel,
        pendingReject         = pendingReject,
        otpPendingBooking     = uiState.otpPendingBooking,
        otpLoading            = uiState.otpLoading,
        onDismissPayment      = { pendingPaymentBooking = null },
        onSimulateSuccess     = { b -> viewModel.processPaymentStub(b, true);  pendingPaymentBooking = null },
        onSimulateFailure     = { b -> viewModel.processPaymentStub(b, false); pendingPaymentBooking = null },
        onConfirmCancel       = { b -> viewModel.cancelBooking(b);   pendingCancel = null },
        onDismissCancel       = { pendingCancel = null },
        onConfirmRejectWithRemarks = { b, r -> viewModel.rejectBookingWithRemarks(b, r); pendingReject = null },
        onDismissReject       = { pendingReject = null },
        onVerifyOtp           = { b, otp -> viewModel.verifyOtpAndComplete(b, otp) },
        onDismissOtp          = { viewModel.dismissOtpDialog() }
    )

    // ── Animated list ↔ detail ────────────────────────────────────────────────
    AnimatedContent(
        targetState    = selectedBooking,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(180)))
            }
        },
        label = "list_detail_transition"
    ) { activeBooking ->
        if (activeBooking != null) {
            BookingDetailScreen(
                booking           = activeBooking,
                isPurohitView     = viewModel.currentUserIsPurohitFor(activeBooking),
                onBack            = { selectedBookingId = null },
                onCompletePayment = onCompletePayment,
                onAccept          = onAccept,
                onReject          = onReject,
                onComplete        = onComplete,
                onCancel          = onCancel,
                onEditBooking     = { addr, date, coords -> onEditBooking(activeBooking, addr, date, coords) }
            )
        } else {
            BookingListContent(
                uiState             = uiState,
                isDark              = isDark,
                pagerState          = pagerState,
                tabs                = tabs,
                scope               = scope,
                snackbarHost        = snackbarHost,
                onCardClick         = { selectedBookingId = it.bookingId },
                isPurohitViewFor    = { viewModel.currentUserIsPurohitFor(it) },
                highlightedId       = uiState.highlightedBookingId,
                onHighlightConsumed = { viewModel.clearHighlight() },
                onCompletePayment   = onCompletePayment,
                onAccept            = onAccept,
                onReject            = onReject,
                onComplete          = onComplete,
                onCancel            = onCancel
            )
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun BookingDialogs(
    isDark                    : Boolean,
    pendingPaymentBooking     : Booking?,
    pendingCancel             : Booking?,
    pendingReject             : Booking?,
    otpPendingBooking         : Booking?,
    otpLoading                : Boolean,
    onDismissPayment          : () -> Unit,
    onSimulateSuccess         : (Booking) -> Unit,
    onSimulateFailure         : (Booking) -> Unit,
    onConfirmCancel           : (Booking) -> Unit,
    onDismissCancel           : () -> Unit,
    onConfirmRejectWithRemarks: (Booking, String) -> Unit,
    onDismissReject           : () -> Unit,
    onVerifyOtp               : (Booking, String) -> Unit,
    onDismissOtp              : () -> Unit
) {
    pendingPaymentBooking?.let { booking ->
        RazorpayStubDialog(
            isDark            = isDark,
            onDismiss         = onDismissPayment,
            onSimulateSuccess = { onSimulateSuccess(booking) },
            onSimulateFailure = { onSimulateFailure(booking) }
        )
    }

    pendingCancel?.let { booking ->
        AlertDialog(
            onDismissRequest = onDismissCancel,
            title   = { Text("Cancel Booking?", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text    = {
                Text(
                    "Are you sure you want to cancel your booking for ${booking.serviceName}? This cannot be undone.",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmCancel(booking) },
                    colors  = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                ) { Text("Yes, Cancel", color = Color.White, fontFamily = FontFamily.Serif) }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancel) {
                    Text("Keep", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    pendingReject?.let { booking ->
        var remarks by rememberSaveable { mutableStateOf("") }
        val isAccepted = booking.status == BookingStatus.ACCEPTED
        AlertDialog(
            onDismissRequest = onDismissReject,
            title = {
                Text(
                    if (isAccepted) "Reject Accepted Booking?" else "Reject Booking?",
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        if (isAccepted)
                            "You have already accepted this booking. Please provide a reason — the customer will see it."
                        else
                            "Please provide a reason for rejecting this booking — the customer will be notified.",
                        fontFamily = FontFamily.Serif, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = remarks,
                        onValueChange = { remarks = it },
                        label         = { Text("Reason for rejection", fontFamily = FontFamily.Serif, fontSize = 13.sp) },
                        placeholder   = { Text("e.g. Unavailable on this date", fontFamily = FontFamily.Serif, fontSize = 12.sp) },
                        isError        = remarks.isNotEmpty() && remarks.isBlank(),
                        supportingText = if (remarks.isNotEmpty() && remarks.isBlank()) {
                            { Text("Reason cannot be blank.", fontFamily = FontFamily.Serif, fontSize = 11.sp) }
                        } else null,
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange,
                            focusedLabelColor  = if (isDark) DarkBrandOrange else BrandOrange,
                            errorBorderColor   = DeleteRed,
                            errorLabelColor    = DeleteRed
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = { if (remarks.isNotBlank()) onConfirmRejectWithRemarks(booking, remarks) },
                    enabled  = remarks.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                ) { Text("Confirm Reject", color = Color.White, fontFamily = FontFamily.Serif) }
            },
            dismissButton = {
                TextButton(onClick = onDismissReject) {
                    Text("Go Back", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    // ── OTP verification dialog (purohit side) ────────────────────────────────
    otpPendingBooking?.let { booking ->
        var otpInput by rememberSaveable(booking.bookingId) { mutableStateOf("") }
        val accentColor = if (isDark) DarkBrandOrange else BrandOrange
        AlertDialog(
            onDismissRequest = { if (!otpLoading) onDismissOtp() },
            title = {
                Text(
                    "Enter Completion Code",
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "A 6-digit code has been sent to the customer. Ask them for the code and enter it below to complete this order.",
                        fontFamily = FontFamily.Serif, fontSize = 13.sp,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    OutlinedTextField(
                        value         = otpInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpInput = it },
                        label         = { Text("6-digit code", fontFamily = FontFamily.Serif, fontSize = 13.sp) },
                        placeholder   = { Text("• • • • • •", fontFamily = FontFamily.Serif, fontSize = 18.sp,
                            textAlign = TextAlign.Center) },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier      = Modifier.fillMaxWidth(),
                        enabled       = !otpLoading,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor  = accentColor
                        )
                    )
                    if (otpLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                            color    = accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { onVerifyOtp(booking, otpInput) },
                    enabled  = otpInput.length == 6 && !otpLoading,
                    colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Verify & Complete", color = Color.White,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick  = { if (!otpLoading) onDismissOtp() },
                    enabled  = !otpLoading
                ) {
                    Text("Cancel", fontFamily = FontFamily.Serif)
                }
            }
        )
    }
}

// ── List content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingListContent(
    uiState            : BookingsUiState,
    isDark             : Boolean,
    pagerState         : PagerState,
    tabs               : List<String>,
    scope              : CoroutineScope,
    snackbarHost       : SnackbarHostState,
    onCardClick        : (Booking) -> Unit,
    isPurohitViewFor   : (Booking) -> Boolean,
    highlightedId      : String?,
    onHighlightConsumed: () -> Unit,
    onCompletePayment  : (Booking) -> Unit,
    onAccept           : (Booking) -> Unit,
    onReject           : (Booking) -> Unit,
    onComplete         : (Booking) -> Unit,
    onCancel           : (Booking) -> Unit
) {
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHost) }) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(scaffoldPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) listOf(DarkBrandOrange, DarkBrandRed)
                            else listOf(BrandOrange, BrandRed),
                            start  = Offset.Zero,
                            end    = Offset.Infinite
                        )
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "My Bookings",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = Color.White
                )
            }

            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = if (isDark) DarkBrandOrange else BrandOrange,
                indicator        = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                        color    = if (isDark) DarkBrandOrange else BrandOrange
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text     = {
                            Text(
                                text       = title,
                                fontFamily = FontFamily.Serif,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold
                                else FontWeight.Normal,
                                fontSize   = 14.sp
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (isDark) DarkBrandOrange else BrandOrange)
                }
            } else {
                HorizontalPager(
                    state             = pagerState,
                    modifier          = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    val bookings = when (pageIndex) {
                        0    -> uiState.activeBookings
                        1    -> uiState.cancelledBookings
                        2    -> uiState.completedBookings
                        else -> emptyList()
                    }
                    if (bookings.isEmpty()) {
                        EmptyBookingsPlaceholder()
                    } else {
                        BookingList(
                            bookings             = bookings,
                            highlightedBookingId = highlightedId,
                            isPurohitViewFor     = isPurohitViewFor,
                            onCardClick          = onCardClick,
                            onCompletePayment    = onCompletePayment,
                            onAccept             = onAccept,
                            onReject             = onReject,
                            onComplete           = onComplete,
                            onCancel             = onCancel,
                            onHighlightConsumed  = onHighlightConsumed
                        )
                    }
                }
            }
        }
    }
}

// ── Booking lazy list ─────────────────────────────────────────────────────────

@Composable
private fun BookingList(
    bookings            : List<Booking>,
    highlightedBookingId: String?,
    isPurohitViewFor    : (Booking) -> Boolean,
    onCardClick         : (Booking) -> Unit,
    onCompletePayment   : (Booking) -> Unit,
    onAccept            : (Booking) -> Unit,
    onReject            : (Booking) -> Unit,
    onComplete          : (Booking) -> Unit,
    onCancel            : (Booking) -> Unit,
    onHighlightConsumed : () -> Unit
) {
    val listState = rememberLazyListState()
    val items     = remember(bookings) { groupByMonth(bookings) }

    LaunchedEffect(highlightedBookingId, bookings) {
        if (highlightedBookingId == null) return@LaunchedEffect
        val index = items.indexOfFirst { item ->
            item is BookingListItem.Card && item.booking.bookingId == highlightedBookingId
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(4_000L)
            onHighlightConsumed()
        }
    }

    LazyColumn(
        state               = listState,
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is BookingListItem.Header -> {
                    stickyHeader(key = "header_${item.label}") {
                        MonthSeparator(label = item.label)
                    }
                }
                is BookingListItem.Card -> {
                    item(key = item.booking.bookingId) {
                        val booking = item.booking
                        BookingCard(
                            booking           = booking,
                            isHighlighted     = booking.bookingId == highlightedBookingId,
                            isPurohitView     = isPurohitViewFor(booking),
                            onCardClick       = onCardClick,
                            onCompletePayment = onCompletePayment,
                            onAccept          = onAccept,
                            onReject          = onReject,
                            onComplete        = onComplete,
                            onCancel          = onCancel
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun MonthSeparator(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text       = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 12.sp,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color     = Color.White.copy(alpha = 0.15f),
            thickness = 0.8.dp
        )
    }
}

@Composable
private fun EmptyBookingsPlaceholder() {
    Box(
        modifier         = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.EventBusy,
                contentDescription = null,
                modifier           = Modifier.size(64.dp).padding(bottom = 16.dp),
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Text(
                text       = "No bookings here yet",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign  = TextAlign.Center
            )
        }
    }
}