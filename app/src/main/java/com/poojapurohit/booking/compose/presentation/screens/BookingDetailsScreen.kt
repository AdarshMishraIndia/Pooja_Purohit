package com.poojapurohit.booking.compose.presentation.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.booking.compose.components.BookingActionButtons
import com.poojapurohit.booking.compose.components.LabelValueRow
import com.poojapurohit.booking.compose.components.StatusChip
import com.poojapurohit.booking.compose.components.bookingActionFlags
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Full-screen booking detail view.
 *
 * Shows every field of the booking document and renders the exact same action
 * buttons as [com.poojapurohit.booking.compose.components.BookingCard] via
 * [com.poojapurohit.booking.compose.components.BookingActionButtons] — single source of truth,
 * zero duplication between the card and this screen.
 *
 * Navigation: driven by `selectedBookingId` state in [com.poojapurohit.booking.compose.presentation.screens.BookingsScreen] — no NavHost needed.
 *
 * @param booking           Live booking to display (kept fresh by the caller).
 * @param isPurohitView     True when the signed-in user is the purohit on this booking.
 * @param onBack            Clears `selectedBookingId` in the parent.
 * @param onCompletePayment User-only, PENDING_PAYMENT state.
 * @param onAccept          Purohit-only, PAYMENT_DONE state.
 * @param onReject          Purohit-only, PAYMENT_DONE — fires the reject dialogue upstream.
 * @param onComplete        Purohit-only, ACCEPTED — fires the complete-confirm dialogue upstream.
 * @param onCancel          User cancel (no remarks dialogue needed here).
 * @param onPurohitCancel   Purohit cancel — fires the remarks dialogue upstream.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    booking          : Booking,
    isPurohitView    : Boolean,
    onBack           : () -> Unit,
    onCompletePayment: ((Booking) -> Unit)? = null,
    onAccept         : ((Booking) -> Unit)? = null,
    onReject         : ((Booking) -> Unit)? = null,
    onComplete       : ((Booking) -> Unit)? = null,
    onCancel         : ((Booking) -> Unit)? = null,
    onPurohitCancel  : ((Booking) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    // LocalConfiguration makes locale changes observable to the Compose runtime,
    // eliminating the "Reading locale in a non-observable way" lint warning.
    val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalConfiguration.current.locales[0]
    } else {
        TODO("VERSION.SDK_INT < N")
    }
    val dateFmt = remember(locale) { SimpleDateFormat("dd MMM yyyy, hh:mm a", locale) }

    val actionFlags = bookingActionFlags(
        booking           = booking,
        isPurohitView     = isPurohitView,
        onAccept          = onAccept,
        onReject          = onReject,
        onComplete        = onComplete,
        onCompletePayment = onCompletePayment,
        onCancel          = onCancel,
        onPurohitCancel   = onPurohitCancel
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Booking Details",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = if (isDark) DarkBrandRed else BrandRed,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = booking.serviceName.ifBlank { "Pooja Service" },
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f)
                )
                StatusChip(status = booking.status)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text       = "ID: ${booking.bookingId}",
                fontFamily = FontFamily.Serif,
                fontSize   = 11.sp,
                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )

            // ── Participants ──────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            SectionDivider("Participants")
            DetailRow("Purohit",    booking.purohitName.ifBlank { "—" })
            DetailRow("User Phone", booking.userPhone.ifBlank { "—" })

            // ── Schedule & Location ───────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            SectionDivider("Schedule & Location")
            booking.scheduledDate?.let { DetailRow("Scheduled", dateFmt.format(it.toDate())) }
            if (booking.address.isNotBlank()) DetailRow("Address", booking.address)

            // ── Payment ───────────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            SectionDivider("Payment")
            DetailRow("Amount", "₹${booking.amount}")
            if (booking.razorpayOrderId.isNotBlank())   DetailRow("Order ID",   booking.razorpayOrderId)
            if (booking.razorpayPaymentId.isNotBlank()) DetailRow("Payment ID", booking.razorpayPaymentId)

            // ── Timestamps ────────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            SectionDivider("Timestamps")
            booking.createdAt?.let { DetailRow("Booked On",    dateFmt.format(it.toDate())) }
            booking.updatedAt?.let { DetailRow("Last Updated", dateFmt.format(it.toDate())) }

            // ── Remarks ───────────────────────────────────────────────────────
            if (!booking.remarks.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                SectionDivider("Cancellation Reason")
                DetailRow("Reason", booking.remarks)
            }

            // ── Actions ───────────────────────────────────────────────────────
            if (actionFlags.hasAny) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                Spacer(Modifier.height(16.dp))
                BookingActionButtons(
                    booking           = booking,
                    isDark            = isDark,
                    flags             = actionFlags,
                    onCompletePayment = onCompletePayment,
                    onAccept          = onAccept,
                    onReject          = onReject,
                    onComplete        = onComplete,
                    onCancel          = onCancel,
                    onPurohitCancel   = onPurohitCancel
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun SectionDivider(label: String) {
    Text(
        text          = label.uppercase(),
        fontFamily    = FontFamily.Serif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 11.sp,
        color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        letterSpacing = 1.2.sp
    )
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(
        color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        thickness = 1.dp
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    LabelValueRow(label = label, value = value, valueFontSize = 14)
    Spacer(Modifier.height(4.dp))
}