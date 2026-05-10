package com.poojapurohit.booking.compose.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingStatus
import com.poojapurohit.booking.model.displayLabel
import com.poojapurohit.dashboard.compose.theme.BrandGold
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.CallGreen
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkSurface
import com.poojapurohit.dashboard.compose.theme.DeleteRed
import com.poojapurohit.dashboard.compose.theme.LightSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val HIGHLIGHT_DURATION_MS    = 4_000L
private const val BORDER_PULSE_DURATION_MS = 2_500
private const val COLOR_FADE_DURATION_MS   = 1_500

/**
 * Summary card shown in the bookings list.
 *
 * @param booking           Data to display.
 * @param isHighlighted     Slow breathing border + warm background wash for [HIGHLIGHT_DURATION_MS] ms.
 * @param isPurohitView     True when the signed-in user is the purohit on this booking.
 * @param onCardClick       Tapping the card body (outside buttons) opens the detail screen.
 * @param onCompletePayment User-only, PENDING_PAYMENT state.
 * @param onAccept          Purohit-only, PAYMENT_DONE state.
 * @param onReject          Purohit-only, PAYMENT_DONE state.
 * @param onComplete        Purohit-only, ACCEPTED state.
 * @param onCancel          User cancel — no remarks required.
 * @param onPurohitCancel   Purohit cancel — triggers remarks dialog upstream.
 * @param onHighlightClear  Fired when the highlight window expires.
 */
@Composable
fun BookingCard(
    booking          : Booking,
    modifier         : Modifier = Modifier,
    isHighlighted    : Boolean = false,
    isPurohitView    : Boolean = false,
    onCardClick      : ((Booking) -> Unit)? = null,
    onCompletePayment: ((Booking) -> Unit)? = null,
    onAccept         : ((Booking) -> Unit)? = null,
    onReject         : ((Booking) -> Unit)? = null,
    onComplete       : ((Booking) -> Unit)? = null,
    onCancel         : ((Booking) -> Unit)? = null,
    onPurohitCancel  : ((Booking) -> Unit)? = null,
    onHighlightClear : (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    // Reading LocalConfiguration makes locale changes observable to the Compose runtime.
    val locale: Locale =
        LocalConfiguration.current.locales[0]
    val dateFmt = remember(locale) { SimpleDateFormat("dd MMM yyyy, hh:mm a", locale) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(HIGHLIGHT_DURATION_MS)
            onHighlightClear?.invoke()
        }
    }

    // ── Background — slow ease-in wash ────────────────────────────────────────
    val cardContainerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            if (isDark) Color(0xFF3A2A10) else Color(0xFFFFF3D0)
        } else {
            if (isDark) DarkSurface else LightSurface
        },
        animationSpec = tween(durationMillis = COLOR_FADE_DURATION_MS, easing = FastOutSlowInEasing),
        label = "card_bg_color"
    )

    // ── Border — slow breathing glow ──────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = BORDER_PULSE_DURATION_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1800f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = BORDER_PULSE_DURATION_MS * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_sweep"
    )

    val highlightBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f  to (if (isDark) DarkBrandOrange else BrandOrange).copy(alpha = borderAlpha),
            0.4f  to BrandGold.copy(alpha = borderAlpha),
            0.65f to Color.Transparent,
            1.0f  to (if (isDark) DarkBrandOrange else BrandOrange).copy(alpha = borderAlpha * 0.5f)
        ),
        start = Offset(0f, gradientOffset),
        end   = Offset(gradientOffset + 700f, gradientOffset + 700f)
    )

    val cardBorder = if (isHighlighted) BorderStroke(2.dp, highlightBrush) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = ripple(bounded = true),
                        onClick           = { onCardClick(booking) }
                    )
                } else Modifier
            ),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 10.dp else 3.dp),
        colors    = CardDefaults.cardColors(containerColor = cardContainerColor),
        border    = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = booking.serviceName.ifBlank { "Pooja Service" },
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.weight(1f)
                )
                StatusChip(status = booking.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            LabelValueRow("Purohit", booking.purohitName.ifBlank { "—" })

            booking.scheduledDate?.let {
                LabelValueRow("Scheduled", dateFmt.format(it.toDate()))
            }
            booking.createdAt?.let {
                LabelValueRow("Booked On", dateFmt.format(it.toDate()))
            }

            if (booking.address.isNotBlank())     LabelValueRow("Address", booking.address)
            if (!booking.remarks.isNullOrBlank()) LabelValueRow("Reason",  booking.remarks)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text       = "₹${booking.amount}",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = if (isDark) DarkBrandOrange else BrandOrange
                )
            }

            val flags = bookingActionFlags(
                booking, isPurohitView,
                onAccept, onReject, onComplete, onCompletePayment, onCancel, onPurohitCancel
            )

            if (flags.hasAny) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                BookingActionButtons(
                    booking           = booking,
                    isDark            = isDark,
                    flags             = flags,
                    onCompletePayment = onCompletePayment,
                    onAccept          = onAccept,
                    onReject          = onReject,
                    onComplete        = onComplete,
                    onCancel          = onCancel,
                    onPurohitCancel   = onPurohitCancel
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action flags — pure function, no Compose dependency
// ─────────────────────────────────────────────────────────────────────────────

data class BookingActionFlags(
    val showPurohitActions  : Boolean,
    val showCompleteAction  : Boolean,
    val showPurohitCancel   : Boolean,
    val showCompletePayment : Boolean,
    val showCancelOnly      : Boolean
) {
    val hasAny: Boolean get() =
        showPurohitActions || showCompleteAction || showCompletePayment ||
                showCancelOnly || showPurohitCancel
}

fun bookingActionFlags(
    booking          : Booking,
    isPurohitView    : Boolean,
    onAccept         : ((Booking) -> Unit)?,
    onReject         : ((Booking) -> Unit)?,
    onComplete       : ((Booking) -> Unit)?,
    onCompletePayment: ((Booking) -> Unit)?,
    onCancel         : ((Booking) -> Unit)?,
    onPurohitCancel  : ((Booking) -> Unit)?
): BookingActionFlags {
    val terminalStatuses = setOf(
        BookingStatus.CANCELLED,
        BookingStatus.REJECTED,
        BookingStatus.REFUNDED,
        BookingStatus.AUTO_CANCELLED,
        BookingStatus.COMPLETED
    )
    return BookingActionFlags(
        showPurohitActions = isPurohitView &&
                booking.status == BookingStatus.PAYMENT_DONE &&
                (onAccept != null || onReject != null),

        showCompleteAction = isPurohitView &&
                booking.status == BookingStatus.ACCEPTED &&
                onComplete != null,

        showPurohitCancel = isPurohitView &&
                booking.status !in terminalStatuses &&
                onPurohitCancel != null,

        showCompletePayment = !isPurohitView &&
                booking.status == BookingStatus.PENDING_PAYMENT &&
                onCompletePayment != null,

        showCancelOnly = !isPurohitView &&
                booking.status in setOf(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAYMENT_DONE,
            BookingStatus.ACCEPTED
        ) && onCancel != null
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared action buttons — used by BookingCard AND BookingDetailScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingActionButtons(
    booking          : Booking,
    isDark           : Boolean,
    flags            : BookingActionFlags,
    onCompletePayment: ((Booking) -> Unit)?,
    onAccept         : ((Booking) -> Unit)?,
    onReject         : ((Booking) -> Unit)?,
    onComplete       : ((Booking) -> Unit)?,
    onCancel         : ((Booking) -> Unit)?,
    onPurohitCancel  : ((Booking) -> Unit)?
) {
    when {
        flags.showPurohitActions -> {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = { onReject?.invoke(booking) },
                    modifier = Modifier.weight(1f),
                    border   = BorderStroke(1.dp, DeleteRed),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                ) {
                    Text("Reject", fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick  = { onAccept?.invoke(booking) },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = CallGreen)
                ) {
                    Text("Accept", fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
            if (flags.showPurohitCancel) {
                Spacer(Modifier.height(6.dp))
                PurohitCancelButton(booking, onPurohitCancel)
            }
        }

        flags.showCompleteAction -> {
            Button(
                onClick  = { onComplete?.invoke(booking) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) DarkBrandOrange else BrandOrange
                )
            ) {
                Text("Mark as Completed", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
            if (flags.showPurohitCancel) {
                Spacer(Modifier.height(6.dp))
                PurohitCancelButton(booking, onPurohitCancel)
            }
        }

        flags.showCompletePayment -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick  = { onCompletePayment?.invoke(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkBrandOrange else BrandOrange
                    )
                ) {
                    Text("Complete Payment", fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
                if (onCancel != null) {
                    OutlinedButton(
                        onClick  = { onCancel(booking) },
                        modifier = Modifier.fillMaxWidth(),
                        border   = BorderStroke(1.dp, DeleteRed),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                    ) {
                        Text("Cancel Booking", fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        flags.showPurohitCancel -> {
            PurohitCancelButton(booking, onPurohitCancel)
        }

        flags.showCancelOnly -> {
            OutlinedButton(
                onClick  = { onCancel?.invoke(booking) },
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, DeleteRed),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
            ) {
                Text("Cancel Booking", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Package-level sub-composables (imported by BookingDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PurohitCancelButton(
    booking        : Booking,
    onPurohitCancel: ((Booking) -> Unit)?
) {
    OutlinedButton(
        onClick  = { onPurohitCancel?.invoke(booking) },
        modifier = Modifier.fillMaxWidth(),
        border   = BorderStroke(1.dp, DeleteRed),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
    ) {
        Text("Cancel Booking", fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun LabelValueRow(label: String, value: String, valueFontSize: Int = 12) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text       = "$label: ",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize   = valueFontSize.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text       = value,
            fontFamily = FontFamily.Serif,
            fontSize   = valueFontSize.sp,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusChip(status: BookingStatus) {
    val (bgColor, textColor) = statusChipColors(status)
    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = status.displayLabel,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize   = 10.sp,
            color      = textColor
        )
    }
}

@Composable
fun statusChipColors(status: BookingStatus): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return when (status) {
        BookingStatus.PENDING_PAYMENT ->  Color(0xFFFFF3CD) to Color(0xFF856404)
        BookingStatus.PAYMENT_DONE    ->  Color(0xFFD1ECF1) to Color(0xFF0C5460)
        BookingStatus.ACCEPTED        ->
            if (isDark) Color(0xFF1B4332) to CallGreen else Color(0xFFD4EDDA) to Color(0xFF155724)
        BookingStatus.COMPLETED       ->
            if (isDark) Color(0xFF1B3A4B) to BrandGold  else Color(0xFFCCE5FF) to Color(0xFF004085)
        BookingStatus.REJECTED,
        BookingStatus.CANCELLED,
        BookingStatus.AUTO_CANCELLED  ->
            if (isDark) Color(0xFF3B1A1A) to DeleteRed  else Color(0xFFF8D7DA) to BrandRed
        BookingStatus.REFUNDED        ->  Color(0xFFE8D5F5) to Color(0xFF6F42C1)
    }
}