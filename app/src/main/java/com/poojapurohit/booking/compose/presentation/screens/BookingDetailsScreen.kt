package com.poojapurohit.booking.compose.presentation.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.poojapurohit.booking.compose.components.BookingActionButtons
import com.poojapurohit.booking.compose.components.SUPPORT_PHONE
import com.poojapurohit.booking.compose.components.StatusChip
import com.poojapurohit.booking.compose.components.bookingActionFlags
import com.poojapurohit.booking.compose.components.graceRemainingText
import com.poojapurohit.booking.compose.components.isWithinGracePeriod
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingStatus
import com.poojapurohit.booking.model.Coordinates
import com.poojapurohit.bookpurohit.compose.presentation.screens.MapPinPickerScreen
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.poojapurohit.booking.compose.components.isCancellationWindowClosed
import kotlin.time.Duration.Companion.milliseconds

// ── Section accent colours ─────────────────────────────────────────────────────

private fun purohitColor(dark: Boolean)  = if (dark) Color(0xFFEF9A9A) else Color(0xFF8D1F06)
private fun customerColor(dark: Boolean) = if (dark) Color(0xFFFFCC80) else Color(0xFFBF360C)
private fun scheduleColor(dark: Boolean) = if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)
private fun paymentColor(dark: Boolean)  = if (dark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
private fun reasonColor(dark: Boolean)   = if (dark) Color(0xFFCE93D8) else Color(0xFF6A1B9A)

// ─────────────────────────────────────────────────────────────────────────────

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
    onEditBooking    : ((newAddress: String, newDate: Timestamp, newCoordinates: Coordinates?) -> Unit)? = null
) {
    val isDark  = isSystemInDarkTheme()
    val context = LocalContext.current

    // ── Hoisted edit state — survives map picker overlay ──────────────────────
    // Seeded once from the live booking; not reset when dialog re-mounts.
    val seedCal = remember(booking.bookingId) {
        java.util.Calendar.getInstance().apply {
            time = booking.scheduledDate?.toDate() ?: java.util.Date()
        }
    }
    var editAddress by rememberSaveable(booking.bookingId) { mutableStateOf(booking.address) }
    var editYear    by rememberSaveable(booking.bookingId) { mutableIntStateOf(seedCal.get(java.util.Calendar.YEAR)) }
    var editMonth   by rememberSaveable(booking.bookingId) { mutableIntStateOf(seedCal.get(java.util.Calendar.MONTH)) }
    var editDay     by rememberSaveable(booking.bookingId) { mutableIntStateOf(seedCal.get(java.util.Calendar.DAY_OF_MONTH)) }
    var editHour    by rememberSaveable(booking.bookingId) { mutableIntStateOf(seedCal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var editMinute  by rememberSaveable(booking.bookingId) { mutableIntStateOf(seedCal.get(java.util.Calendar.MINUTE)) }
    var editPickedCoords by remember(booking.bookingId) {
        mutableStateOf(booking.coordinates?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 })
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showMapPicker  by remember { mutableStateOf(false) }

    if (showMapPicker) {
        MapPinPickerScreen(
            initialPin = editPickedCoords?.let {
                com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
            },
            isDark     = isDark,
            onConfirm  = { latLng ->
                editPickedCoords = Coordinates(latLng.latitude, latLng.longitude)
                showMapPicker    = false
            },
            onDismiss  = { showMapPicker = false }
        )
        return
    }

    // Edit is allowed when: customer view + editable status + free cancellation window
    val canEdit = !isPurohitView &&
            booking.status in setOf(BookingStatus.PAYMENT_DONE, BookingStatus.ACCEPTED) &&
            isWithinGracePeriod(booking) &&
            onEditBooking != null

    if (showEditDialog) {
        EditBookingDialog(
            isDark          = isDark,
            address         = editAddress,
            onAddressChange = { editAddress = it },
            selYear         = editYear,
            selMonth        = editMonth,
            selDay          = editDay,
            selHour         = editHour,
            selMinute       = editMinute,
            onDateTimeChange = { y, mo, d, h, mi ->
                editYear = y; editMonth = mo; editDay = d; editHour = h; editMinute = mi
            },
            pickedCoords    = editPickedCoords,
            onOpenMapPicker = { showMapPicker = true },
            onDismiss       = { showEditDialog = false },
            onConfirm       = { addr, date, coords ->
                onEditBooking?.invoke(addr, date, coords)
                showEditDialog = false
                // Stay on detail screen — no navigation
            }
        )
    }

    val locale: Locale = LocalConfiguration.current.locales[0]
    val dateFmt = remember(locale) { SimpleDateFormat("dd MMM yyyy, hh:mm a", locale) }

    val actionFlags = bookingActionFlags(
        booking = booking, isPurohitView = isPurohitView,
        onAccept = onAccept, onReject = onReject, onComplete = onComplete,
        onCompletePayment = onCompletePayment, onCancel = onCancel
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Booking Details", fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. Header ─────────────────────────────────────────────────────
            HeaderCard(booking = booking, isDark = isDark)

            // ── 2. Grace period banner (customer view, grace still active) ────
            if (!isPurohitView) {
                val withinGrace = isWithinGracePeriod(booking)
                val isActive    = booking.status in setOf(
                    BookingStatus.PAYMENT_DONE, BookingStatus.ACCEPTED
                )

                if (isActive) {
                    when {
                        withinGrace  -> GracePeriodBanner(booking = booking, isDark = isDark)
                        else         -> CancellationLockedBanner(isDark = isDark)
                    }
                }
            }

            // ── 3. Edit button (customer only, window open) ───────────────────
            if (canEdit) {
                EditBookingButton(isDark = isDark) { showEditDialog = true }
            }

            // ── 4. Counterparty Details ───────────────────────────────────────
            val accentCol = if (isPurohitView) customerColor(isDark) else purohitColor(isDark)
            val sectionTitle = if (isPurohitView) "Customer" else "Purohit"
            val partyName = if (isPurohitView) booking.userName else booking.purohitName
            val partyPhone = if (isPurohitView) booking.userPhone else booking.purohitPhone

            SectionCard(accentColor = accentCol, isDark = isDark) {
                SectionHeader(sectionTitle, accentCol)
                InfoRow("Name", partyName.ifBlank { "—" })

                if (partyPhone.isNotBlank()) {
                    ProtectedPhoneRow(
                        phone = partyPhone,
                        isRevealed = isCancellationWindowClosed(booking) && booking.status == BookingStatus.ACCEPTED
                    )
                }
            }

            // ── 6. Schedule & Location ─────────────────────────────────────────
            SectionCard(accentColor = scheduleColor(isDark), isDark = isDark) {
                SectionHeader("Schedule & Location", scheduleColor(isDark))
                booking.scheduledDate?.let {
                    InfoRow("Date & Time", dateFmt.format(it.toDate()))
                }
                if (booking.address.isNotBlank()) {
                    AddressRow(
                        address    = booking.address,
                        isDark     = isDark,
                        onMapClick = {
                            val coords = booking.coordinates
                            val geoUri = if (coords != null &&
                                (coords.latitude != 0.0 || coords.longitude != 0.0)) {
                                "geo:${coords.latitude},${coords.longitude}?q=${coords.latitude},${coords.longitude}(${Uri.encode(booking.address)})".toUri()
                            } else {
                                "geo:0,0?q=${Uri.encode(booking.address)}".toUri()
                            }
                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                .setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                val webUrl = if (coords != null &&
                                    (coords.latitude != 0.0 || coords.longitude != 0.0))
                                    "https://maps.google.com/?q=${coords.latitude},${coords.longitude}"
                                else
                                    "https://maps.google.com/?q=${Uri.encode(booking.address)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
                            }
                        }
                    )
                }
            }

            // ── 7. Payment ────────────────────────────────────────────────────
            // Amount shown only here — not repeated in header for detail screen
            SectionCard(accentColor = paymentColor(isDark), isDark = isDark) {
                SectionHeader("Payment", paymentColor(isDark))
                InfoRow("Amount", "₹${booking.amount}", valueColor = paymentColor(isDark), valueBold = true)
            }

            // ── 7b. Completion OTP (customer view only) ───────────────────────
            // Shown when purohit has initiated completion. Auto-disappears when
            // booking moves to COMPLETED and completionOtp is deleted from Firestore.
            if (!isPurohitView &&
                booking.status == BookingStatus.ACCEPTED &&
                !booking.completionOtp.isNullOrBlank()
            ) {
                CompletionOtpCard(otp = booking.completionOtp, isDark = isDark)
            }

            // ── 8. Booked On ──────────────────────────────────────────────────
            booking.createdAt?.let { BookedOnFooter(dateFmt.format(it.toDate())) }

            // ── 9. Reason ─────────────────────────────────────────────────────
            if (!booking.comments.isNullOrBlank()) {
                SectionCard(accentColor = reasonColor(isDark), isDark = isDark) {
                    SectionHeader("Reason", reasonColor(isDark))
                    InfoRow("Details", booking.comments)
                }
            }

            // ── 10. Actions ────────────────────────────────────────────────────
            if (actionFlags.hasAny) {
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                BookingActionButtons(
                    booking = booking, isDark = isDark, flags = actionFlags,
                    onCompletePayment = onCompletePayment,
                    onAccept = onAccept, onReject = onReject,
                    onComplete = onComplete, onCancel = onCancel
                )
            }
        }
    }
}

// ── Section building blocks ───────────────────────────────────────────────────

@Composable
private fun SectionCard(
    accentColor : Color,
    isDark      : Boolean,
    modifier    : Modifier = Modifier,
    content     : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = if (isDark) 0.10f else 0.05f)
        ),
        border    = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.30f else 0.20f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) { content() }
    }
}

@Composable
private fun SectionHeader(label: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(accentColor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(label.uppercase(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, color = accentColor, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun InfoRow(
    label      : String,
    value      : String,
    valueColor : Color? = null,
    valueBold  : Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(110.dp))
        Text(value, fontFamily = FontFamily.Serif,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp, color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
    }
}

// ── Specialised rows / cards ──────────────────────────────────────────────────

@Composable
private fun HeaderCard(booking: Booking, isDark: Boolean) {
    val borderColor = if (isDark) DarkBrandRed else BrandRed
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.5.dp, borderColor.copy(alpha = if (isDark) 0.5f else 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = booking.serviceName.ifBlank { "Pooja Service" },
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color      = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                )
                StatusChip(status = booking.status)
            }
        }
    }
}

@Composable
private fun ProtectedPhoneRow(phone: String, isRevealed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Contact Details", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(110.dp))

        if (isRevealed) {
            Text(phone, fontFamily = FontFamily.Serif, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
        } else {
            // Blurred state as requested
            Text(phone, fontFamily = FontFamily.Serif, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.blur(6.dp))
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.Lock, "Hidden until cancellation window closes",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text("Locked", fontFamily = FontFamily.Serif, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

/**
 * Address row with a map-pin button (LocationOn icon + "Show in Map" subtext).
 */
@Composable
private fun AddressRow(address: String, isDark: Boolean, onMapClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Address", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(110.dp))
        Text(address, fontFamily = FontFamily.Serif, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        // Pin button: tap area wraps the icon + label tightly
        Column(
            modifier            = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = ripple(bounded = false, radius = 26.dp),
                    onClick           = onMapClick
                )
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.LocationOn,
                contentDescription = "Show in Map",
                tint               = scheduleColor(isDark),
                modifier           = Modifier.size(22.dp)
            )
            Text(
                text       = "Show in Map",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 8.sp,
                color      = scheduleColor(isDark)
            )
        }
    }
}

@Composable
private fun BookedOnFooter(label: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CalendarToday, null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
            modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text("Booked on $label", fontFamily = FontFamily.Serif, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f))
    }
}

/**
 * Banner shown in the detail screen while the cancellation window is still active.
 * Ticks every second in HH:MM:SS format.
 */
@Composable
private fun GracePeriodBanner(booking: Booking, isDark: Boolean) {
    var remaining by remember { mutableStateOf(graceRemainingText(booking)) }
    LaunchedEffect(booking.bookingId) {
        while (true) {
            remaining = graceRemainingText(booking)
            if (remaining == null) break
            delay(1_000L.milliseconds)
        }
    }
    val text = remaining ?: return
    val chipColor = if (isDark) Color(0xFF80CBC4) else Color(0xFF00695C)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = chipColor.copy(alpha = if (isDark) 0.10f else 0.06f)
        ),
        border    = BorderStroke(1.dp, chipColor.copy(alpha = if (isDark) 0.30f else 0.20f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, null, tint = chipColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Free cancellation available", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp, color = chipColor)
                Text("You can cancel this booking without any reason for the next $text.",
                    fontFamily = FontFamily.Serif, fontSize = 11.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
        }
    }
}

/**
 * Banner shown when the cancellation window has closed.
 */
@Composable
private fun CancellationLockedBanner(isDark: Boolean) {
    val warnColor   = if (isDark) Color(0xFFFFCC80) else Color(0xFF6D4C41)
    val bgColor     = warnColor.copy(alpha = if (isDark) 0.10f else 0.06f)
    val borderColor = warnColor.copy(alpha = if (isDark) 0.30f else 0.20f)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = warnColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cancellation window closed", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = warnColor)
            }
            Text(
                text       = "The cancellation window for this booking has closed. " +
                        "Self-cancellation is no longer available. To request a " +
                        "cancellation, please call our support team at $SUPPORT_PHONE.",
                fontFamily = FontFamily.Serif, fontSize = 12.sp, lineHeight = 18.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )
        }
    }
}

// ── Edit booking ──────────────────────────────────────────────────────────────

/**
 * Floating edit button — shown only when:
 *  - customer view
 *  - booking is in an editable status (PAYMENT_DONE or ACCEPTED)
 *  - cancellation window is still open
 */
@Composable
internal fun EditBookingButton(isDark: Boolean, onClick: () -> Unit) {
    val accent = scheduleColor(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = accent.copy(alpha = if (isDark) 0.10f else 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = ripple(bounded = true),
                onClick           = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.Edit,
            contentDescription = "Edit booking",
            tint               = accent,
            modifier           = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Edit Address or Schedule", fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accent)
            Text("Change while cancellation window is open", fontFamily = FontFamily.Serif,
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ArrowBack, // using as chevron-right visually
            contentDescription = null,
            tint               = accent.copy(alpha = 0.5f),
            modifier           = Modifier.size(14.dp)
        )
    }
}

/**
 * Fully stateless edit dialog — all mutable state is hoisted to [BookingDetailScreen]
 * so it survives the map picker overlay without any reset.
 */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditBookingDialog(
    isDark          : Boolean,
    address         : String,
    onAddressChange : (String) -> Unit,
    selYear         : Int,
    selMonth        : Int,
    selDay          : Int,
    selHour         : Int,
    selMinute       : Int,
    onDateTimeChange: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit,
    pickedCoords    : Coordinates?,
    onOpenMapPicker : () -> Unit,
    onDismiss       : () -> Unit,
    onConfirm       : (newAddress: String, newDate: Timestamp, newCoordinates: Coordinates?) -> Unit
) {
    val accent  = scheduleColor(isDark)
    val locale  = LocalConfiguration.current.locales[0]
    val dateFmt = remember(locale) { SimpleDateFormat("dd MMM yyyy, hh:mm a", locale) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.util.Calendar.getInstance()
            .apply { set(selYear, selMonth, selDay, 0, 0, 0) }.timeInMillis,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Strip time — allow today and future only
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                return utcTimeMillis >= todayStart
            }
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selHour, initialMinute = selMinute, is24Hour = false
    )

    val displayDateTime = remember(selYear, selMonth, selDay, selHour, selMinute) {
        java.util.Calendar.getInstance()
            .apply { set(selYear, selMonth, selDay, selHour, selMinute, 0) }
            .time.let { dateFmt.format(it) }
    }

    // ── Date picker ───────────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        java.util.Calendar.getInstance().apply { timeInMillis = millis }.let { cal ->
                            onDateTimeChange(
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH),
                                selHour, selMinute
                            )
                        }
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next →", fontFamily = FontFamily.Serif, color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", fontFamily = FontFamily.Serif)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── Time picker ───────────────────────────────────────────────────────────
    if (showTimePicker) {
        // Recompute on every picker interaction
        val todayCal = remember { java.util.Calendar.getInstance() }
        val isToday  = selYear  == todayCal.get(java.util.Calendar.YEAR) &&
                selMonth == todayCal.get(java.util.Calendar.MONTH) &&
                selDay   == todayCal.get(java.util.Calendar.DAY_OF_MONTH)

        val isPastTime = isToday &&
                (timePickerState.hour < todayCal.get(java.util.Calendar.HOUR_OF_DAY) ||
                        (timePickerState.hour == todayCal.get(java.util.Calendar.HOUR_OF_DAY) &&
                                timePickerState.minute <= todayCal.get(java.util.Calendar.MINUTE)))

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text  = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) { TimePicker(state = timePickerState) }

                    // Inline warning — visible only when a past time is selected on today
                    androidx.compose.animation.AnimatedVisibility(visible = isPastTime) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(if (isDark) 0x33FF5252 else 0x1FD32F2F),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Lock,
                                contentDescription = null,
                                tint               = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F),
                                modifier           = Modifier.size(13.dp)
                            )
                            Text(
                                text       = "This time has already passed. Please pick a future time.",
                                fontFamily = FontFamily.Serif,
                                fontSize   = 11.sp,
                                lineHeight  = 15.sp,
                                color      = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDateTimeChange(selYear, selMonth, selDay,
                            timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    enabled = !isPastTime,
                    colors  = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("OK", color = Color.White, fontFamily = FontFamily.Serif) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", fontFamily = FontFamily.Serif)
                }
            }
        )
    }

    // ── Main dialog ───────────────────────────────────────────────────────────
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Booking", fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Address
                OutlinedTextField(
                    value         = address,
                    onValueChange = onAddressChange,
                    label         = { Text("Address", fontFamily = FontFamily.Serif, fontSize = 13.sp) },
                    minLines      = 2, maxLines = 4,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, focusedLabelColor = accent
                    )
                )

                // Map pin picker — preview + button
                if (pickedCoords != null) {
                    val previewLatLng = com.google.android.gms.maps.model.LatLng(
                        pickedCoords.latitude, pickedCoords.longitude
                    )
                    val cameraState = com.google.maps.android.compose.rememberCameraPositionState {
                        position = com.google.android.gms.maps.model.CameraPosition
                            .fromLatLngZoom(previewLatLng, 13f)
                    }
                    // Keep camera in sync if coords change
                    LaunchedEffect(pickedCoords) {
                        cameraState.position = com.google.android.gms.maps.model.CameraPosition
                            .fromLatLngZoom(previewLatLng, 13f)
                    }
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape     = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border    = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        com.google.maps.android.compose.GoogleMap(
                            modifier            = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState,
                            uiSettings          = com.google.maps.android.compose.MapUiSettings(
                                zoomControlsEnabled     = false,
                                scrollGesturesEnabled   = false,
                                zoomGesturesEnabled     = false,
                                tiltGesturesEnabled     = false,
                                rotationGesturesEnabled = false,
                                mapToolbarEnabled       = false,
                                myLocationButtonEnabled = false
                            )
                        ) {
                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.MarkerState(
                                    position = previewLatLng
                                ),
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                                    .defaultMarker(
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                                    )
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick  = onOpenMapPicker,
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = if (pickedCoords != null) "Tap to Change Map Location"
                        else "Pick Location on Map",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp
                    )
                }

                // Date & time
                OutlinedButton(
                    onClick  = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(displayDateTime, fontFamily = FontFamily.Serif,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // ── Near-date warning ─────────────────────────────────────────
                val selectedMs = java.util.Calendar.getInstance()
                    .apply { set(selYear, selMonth, selDay, selHour, selMinute, 0) }
                    .timeInMillis
                val within24h = selectedMs - System.currentTimeMillis() <= TimeUnit.DAYS.toMillis(1)

                if (within24h) {
                    val errColor = com.poojapurohit.dashboard.compose.theme.DeleteRed
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .background(errColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null,
                            tint     = errColor,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "This date is within 24 hours of now. After saving, the cancellation window " +
                                    "may be limited or closed immediately based on booking timing.",
                            fontFamily = FontFamily.Serif, fontSize = 11.sp, lineHeight = 15.sp,
                            color      = errColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(selYear, selMonth, selDay, selHour, selMinute, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onConfirm(address.trim(), Timestamp(cal.time), pickedCoords)
                },
                enabled = address.isNotBlank(),
                colors  = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Save Changes", color = Color.White,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = FontFamily.Serif)
            }
        }
    )
}

// ── Completion OTP section card (customer view only) ──────────────────────────

private fun otpAccentColor(dark: Boolean) =
    if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)  // Blue 200 / Blue 800

/**
 * Full-width section card displayed in BookingDetailsScreen when a completion
 * OTP is live. Mirrors the same visibility rule as CompletionOtpChip in
 * BookingCard: !isPurohitView && status == ACCEPTED && completionOtp != null.
 *
 * The card auto-disappears when the Firestore listener pushes the COMPLETED
 * status update (completionOtp deleted), triggering recomposition.
 */
@Composable
private fun CompletionOtpCard(otp: String, isDark: Boolean) {
    val accent = otpAccentColor(isDark)
    SectionCard(accentColor = accent, isDark = isDark) {
        SectionHeader("Completion Code", accent)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.VpnKey,
                contentDescription = null,
                tint               = accent,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text          = otp,
                fontFamily    = FontFamily.Serif,
                fontWeight    = FontWeight.Bold,
                fontSize      = 32.sp,
                letterSpacing = 8.sp,
                color         = accent
            )
        }
        Text(
            text       = "Share this code with the purohit to confirm completion of your booking.",
            fontFamily = FontFamily.Serif,
            fontSize   = 12.sp,
            lineHeight  = 17.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
        )
    }
}