package com.poojapurohit.booking.compose.components

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.firebase.Timestamp
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
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private const val HIGHLIGHT_DURATION_MS    = 4_000L
private const val BORDER_PULSE_DURATION_MS = 2_500
private const val COLOR_FADE_DURATION_MS   = 1_500

// ─────────────────────────────────────────────────────────────────────────────
// Cancellation window helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Returns true when now is within 24 h of the scheduled event (or past it). */
fun isWithin1DayOfEvent(scheduledDate: Timestamp?): Boolean {
    scheduledDate ?: return false
    val cutoffMs = scheduledDate.toDate().time - TimeUnit.DAYS.toMillis(1)
    return System.currentTimeMillis() >= cutoffMs
}

/**
 * Computes the epoch-ms at which the cancellation window closes for [booking].
 *
 * Rule:
 *  - diff = scheduledDate - createdAt
 *  - diff > 27 h  → window closes at scheduledDate − 24 h
 *  - diff ≤ 27 h  → window closes at createdAt + 6 h
 *
 * Returns null when either timestamp is missing.
 */
fun cancellationWindowEndMs(booking: Booking): Long? {
    val createdMs = booking.createdAt?.toDate()?.time ?: return null
    val scheduledMs = booking.scheduledDate?.toDate()?.time ?: return null
    val diffMs      = scheduledMs - createdMs
    return when {
        diffMs > TimeUnit.HOURS.toMillis(27) ->
            scheduledMs - TimeUnit.HOURS.toMillis(24)   // window closes at schedule - 24 h
        diffMs > TimeUnit.HOURS.toMillis(6)  ->
            createdMs + TimeUnit.HOURS.toMillis(3)      // window = 3 h from creation
        else                                  ->
            createdMs                                   // <= 6 h gap -> no window
    }
}

/**
 * Returns true when the cancellation window is still open.
 * Replaces the old 3-hour-only grace period.
 */
fun isWithinGracePeriod(booking: Booking): Boolean {
    val endMs = cancellationWindowEndMs(booking) ?: return false
    return System.currentTimeMillis() < endMs
}

/** Overload accepting only createdAt — kept for call-sites that don't have the full Booking. */
fun isWithinGracePeriod(createdAt: Timestamp?): Boolean {
    // Legacy single-arg version: cannot determine correct window without scheduledDate.
    // Callers that have the full Booking MUST use isWithinGracePeriod(Booking).
    createdAt ?: return false
    val graceEndsMs = createdAt.toDate().time + TimeUnit.HOURS.toMillis(3)
    return System.currentTimeMillis() < graceEndsMs
}

/**
 * Returns true if the cancellation window is fully closed.
 * Returns false (window open / blurred) when timestamps are missing — safe default.
 */
fun isCancellationWindowClosed(booking: Booking): Boolean {
    val endMs = cancellationWindowEndMs(booking) ?: return false
    return System.currentTimeMillis() >= endMs
}

/**
 * Returns the remaining time in the cancellation window as "HH:MM:SS".
 * Returns null when the window has closed.
 */
fun graceRemainingText(booking: Booking): String? {
    val endMs       = cancellationWindowEndMs(booking) ?: return null
    val remainingMs = endMs - System.currentTimeMillis()
    if (remainingMs <= 0L) return null
    val h  = TimeUnit.MILLISECONDS.toHours(remainingMs)
    val m  = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
    val s  = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Legacy overload — kept for call-sites still passing only createdAt. */
fun graceRemainingText(createdAt: Timestamp?): String? {
    createdAt ?: return null
    val graceEndsMs = createdAt.toDate().time + TimeUnit.HOURS.toMillis(3)
    val remainingMs = graceEndsMs - System.currentTimeMillis()
    if (remainingMs <= 0L) return null
    val h  = TimeUnit.MILLISECONDS.toHours(remainingMs)
    val m  = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
    val s  = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Phone number of the marketing executive shown when cancellation is blocked. */
const val SUPPORT_PHONE = "+919999999999" // TODO: replace with actual number

// ─────────────────────────────────────────────────────────────────────────────
// BookingCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingCard(
    booking          : Booking,
    modifier         : Modifier = Modifier,
    isHighlighted    : Boolean = false,
    isPurohitView    : Boolean,
    onCardClick      : ((Booking) -> Unit)? = null,
    onCompletePayment: ((Booking) -> Unit)? = null,
    onAccept         : ((Booking) -> Unit)? = null,
    onReject         : ((Booking) -> Unit)? = null,
    onComplete       : ((Booking) -> Unit)? = null,
    onCancel         : ((Booking) -> Unit)? = null,
    onHighlightClear : (() -> Unit)? = null
) {
    val isDark  = isSystemInDarkTheme()
    val locale  = LocalConfiguration.current.locales[0]
    val dateFmt = remember(locale) { SimpleDateFormat("dd MMM yyyy, hh:mm a", locale) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(HIGHLIGHT_DURATION_MS.milliseconds)
            onHighlightClear?.invoke()
        }
    }

    val cardContainerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            if (isDark) Color(0xFF3A2A10) else Color(0xFFFFF3D0)
        } else {
            if (isDark) DarkSurface else LightSurface
        },
        animationSpec = tween(durationMillis = COLOR_FADE_DURATION_MS, easing = FastOutSlowInEasing),
        label = "card_bg_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = BORDER_PULSE_DURATION_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "border_alpha"
    )
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = BORDER_PULSE_DURATION_MS * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "gradient_sweep"
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

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .then(
                if (onCardClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = ripple(bounded = true),
                    onClick           = { onCardClick(booking) }
                ) else Modifier
            ),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 10.dp else 3.dp),
        colors    = CardDefaults.cardColors(containerColor = cardContainerColor),
        border    = if (isHighlighted) BorderStroke(2.dp, highlightBrush) else null
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

            Spacer(Modifier.height(6.dp))
            val windowClosed = isCancellationWindowClosed(booking)
            val phoneRevealed = windowClosed && booking.status == BookingStatus.ACCEPTED
            if (isPurohitView) {
                LabelValueRow("Customer", booking.userName.ifBlank { "—" })
                if (booking.userPhone.isNotBlank())
                    CardPhoneRow(label = "Contact", phone = booking.userPhone, revealed = phoneRevealed)
            } else {
                LabelValueRow("Purohit", booking.purohitName.ifBlank { "—" })
                if (booking.purohitPhone.isNotBlank())
                    CardPhoneRow(label = "Contact", phone = booking.purohitPhone, revealed = phoneRevealed)
            }
            booking.scheduledDate?.let { LabelValueRow("Scheduled", dateFmt.format(it.toDate())) }
            booking.createdAt?.let    { LabelValueRow("Booked On", dateFmt.format(it.toDate())) }
            if (booking.address.isNotBlank())     LabelValueRow("Address", booking.address)
            if (!booking.comments.isNullOrBlank()) LabelValueRow("Reason",  booking.comments)

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text       = "₹${booking.amount}",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = if (isDark) DarkBrandOrange else BrandOrange
                )
            }

            // ── Completion OTP chip (customer view only) ──────────────────────
            // Shown when the purohit has initiated completion and the OTP is live.
            // Disappears automatically once the booking moves to COMPLETED
            // because completionOtp is deleted from the document at that point.
            if (!isPurohitView &&
                booking.status == BookingStatus.ACCEPTED &&
                !booking.completionOtp.isNullOrBlank()
            ) {
                Spacer(Modifier.height(8.dp))
                CompletionOtpChip(otp = booking.completionOtp, isDark = isDark)
            }

            val flags = bookingActionFlags(booking, isPurohitView,
                onAccept, onReject, onComplete, onCompletePayment, onCancel)

            if (flags.hasAny) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                BookingActionButtons(
                    booking = booking, isDark = isDark, flags = flags,
                    onCompletePayment = onCompletePayment,
                    onAccept = onAccept, onReject = onReject,
                    onComplete = onComplete, onCancel = onCancel
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action flags
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cancellation rules:
 *  1. Grace period (createdAt within 3 h) → customer can always cancel.
 *  2. After grace period + within 1 day of event → cancel blocked; show call-support.
 *  3. After grace period + more than 1 day away → customer can cancel normally.
 */
data class BookingActionFlags(
    val showPurohitActions  : Boolean,
    val showCompleteAction  : Boolean,
    val showCompletePayment : Boolean,
    val showCancelOnly      : Boolean,   // free cancellation window open
    val showCallSupport     : Boolean    // window closed, call support instead
) {
    val hasAny: Boolean get() =
        showPurohitActions || showCompleteAction || showCompletePayment ||
                showCancelOnly || showCallSupport
}

fun bookingActionFlags(
    booking          : Booking,
    isPurohitView    : Boolean,
    onAccept         : ((Booking) -> Unit)?,
    onReject         : ((Booking) -> Unit)?,
    onComplete       : ((Booking) -> Unit)?,
    onCompletePayment: ((Booking) -> Unit)?,
    onCancel         : ((Booking) -> Unit)?
): BookingActionFlags {
    val withinGrace = isWithinGracePeriod(booking)
    val customerActive = !isPurohitView && booking.status in setOf(
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.PAYMENT_DONE,
        BookingStatus.ACCEPTED
    )
    // Non-payment statuses have dedicated cancel/call-support rows
    val nonPayment = booking.status != BookingStatus.PENDING_PAYMENT

    return BookingActionFlags(
        showPurohitActions  = isPurohitView &&
                booking.status == BookingStatus.PAYMENT_DONE &&
                (onAccept != null || onReject != null),

        showCompleteAction  = isPurohitView &&
                booking.status == BookingStatus.ACCEPTED &&
                onComplete != null,

        showCompletePayment = !isPurohitView &&
                booking.status == BookingStatus.PENDING_PAYMENT &&
                onCompletePayment != null,

        showCancelOnly      = customerActive && withinGrace && onCancel != null && nonPayment,
        showCallSupport     = customerActive && !withinGrace && nonPayment
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared action buttons
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
    onCancel         : ((Booking) -> Unit)?
) {
    val context     = LocalContext.current
    val withinGrace = isWithinGracePeriod(booking)

    when {
        flags.showPurohitActions -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { onReject?.invoke(booking) },
                    modifier = Modifier.weight(1f),
                    border   = BorderStroke(1.dp, DeleteRed),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                ) { Text("Reject", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Button(
                    onClick  = { onAccept?.invoke(booking) },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = CallGreen)
                ) { Text("Accept", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
            }
        }

        flags.showCompleteAction -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick  = { onComplete?.invoke(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = if (isDark) DarkBrandOrange else BrandOrange)
                ) { Text("Mark as Completed", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
                OutlinedButton(
                    onClick  = { onReject?.invoke(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, DeleteRed),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                ) { Text("Reject Booking", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // PENDING_PAYMENT: Restart Payment + cancel or call-support depending on window
        flags.showCompletePayment -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Grace period chip — shown only while grace is still active
                if (withinGrace) {
                    GracePeriodChip(booking = booking, isDark = isDark)
                }
                Button(
                    onClick  = { onCompletePayment?.invoke(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = if (isDark) DarkBrandOrange else BrandOrange)
                ) { Text("Restart Payment", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
                if (withinGrace && onCancel != null) {
                    OutlinedButton(
                        onClick  = { onCancel(booking) },
                        modifier = Modifier.fillMaxWidth(),
                        border   = BorderStroke(1.dp, DeleteRed),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                    ) { Text("Cancel Booking", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                } else if (!withinGrace) {
                    CallSupportButton(isDark = isDark, context = context)
                }
            }
        }

        // Free cancellation window open
        flags.showCancelOnly -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (withinGrace) {
                    GracePeriodChip(booking = booking, isDark = isDark)
                }
                OutlinedButton(
                    onClick  = { onCancel?.invoke(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, DeleteRed),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed)
                ) { Text("Cancel Booking", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Cancellation window closed — no grace period left
        flags.showCallSupport -> {
            CallSupportButton(isDark = isDark, context = context)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Small info chip shown while the cancellation window is active.
 * Ticks every second in HH:MM:SS format.
 */
@Composable
fun GracePeriodChip(booking: Booking, isDark: Boolean) {
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
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(chipColor.copy(alpha = if (isDark) 0.12f else 0.07f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.Timer,
            contentDescription = null,
            tint               = chipColor,
            modifier           = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = "Free cancellation available · $text remaining",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 11.sp,
            color      = chipColor
        )
    }
}

/**
 * Shown when the 1-day window is active and grace period has expired.
 * Fires a dial intent to the marketing executive.
 */
@Composable
fun CallSupportButton(isDark: Boolean, context: android.content.Context) {
    val supportColor = if (isDark) Color(0xFFFFCC80) else Color(0xFF6D4C41)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = supportColor.copy(alpha = if (isDark) 0.12f else 0.07f)
        ),
        border    = BorderStroke(1.dp, supportColor.copy(alpha = if (isDark) 0.35f else 0.25f))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = supportColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cancellation locked", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = supportColor)
            }
            Text(
                text       = "You're within 24 hours of your event. To cancel, please contact our support team.",
                fontFamily = FontFamily.Serif, fontSize = 12.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Button(
                onClick  = { context.startActivity(Intent(Intent.ACTION_DIAL,
                    "tel:$SUPPORT_PHONE".toUri())) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = supportColor)
            ) {
                Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Call Support", fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Package-level sub-composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Phone row shown on BookingCard for the counterparty.
 * Number is blurred while the cancellation window is open; revealed once it closes.
 */
@Composable
fun CardPhoneRow(label: String, phone: String, revealed: Boolean) {
    Row(
        modifier          = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label: ",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 12.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (revealed) {
            Text(
                phone,
                fontFamily = FontFamily.Serif,
                fontSize   = 12.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                phone,
                fontFamily = FontFamily.Serif,
                fontSize   = 12.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.blur(6.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun LabelValueRow(label: String, value: String, valueFontSize: Int = 12) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text("$label: ", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
            fontSize = valueFontSize.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontFamily = FontFamily.Serif, fontSize = valueFontSize.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Animated status chip.
 *
 * - Colours fade smoothly via [animateColorAsState] when status changes in real-time
 *   (Firestore listener pushes the new status → recomposition → transition plays).
 * - A brief scale pop via [Animatable] makes the change visually noticeable without
 *   requiring any explicit "status changed" signal from the caller.
 */
@Composable
fun StatusChip(status: BookingStatus) {
    val (bgColor, textColor) = statusChipColors(status)

    // Smooth colour transition on real-time status update
    val animBg   by animateColorAsState(bgColor,   animationSpec = tween(450), label = "chip_bg")
    val animText by animateColorAsState(textColor, animationSpec = tween(450), label = "chip_text")

    // Brief scale pop whenever status changes
    val scale = remember { Animatable(1f) }
    LaunchedEffect(status) {
        scale.animateTo(1.12f, tween(100))
        scale.animateTo(1f,    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(color = animBg, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.displayLabel, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, color = animText)
    }
}

@Composable
fun statusChipColors(status: BookingStatus): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return when (status) {
        BookingStatus.PENDING_PAYMENT -> Color(0xFFFFF3CD) to Color(0xFF856404)
        BookingStatus.PAYMENT_DONE    -> Color(0xFFD1ECF1) to Color(0xFF0C5460)
        BookingStatus.ACCEPTED        ->
            if (isDark) Color(0xFF1B4332) to CallGreen else Color(0xFFD4EDDA) to Color(0xFF155724)
        BookingStatus.COMPLETED       ->
            if (isDark) Color(0xFF1B3A4B) to BrandGold  else Color(0xFFCCE5FF) to Color(0xFF004085)
        BookingStatus.REJECTED,
        BookingStatus.CANCELLED_BY_USER,
        BookingStatus.CANCELLED_BY_PUROHIT,
        BookingStatus.AUTO_CANCELLED  ->
            if (isDark) Color(0xFF3B1A1A) to DeleteRed  else Color(0xFFF8D7DA) to BrandRed
        BookingStatus.REFUNDED        -> Color(0xFFE8D5F5) to Color(0xFF6F42C1)
    }
}

/**
 * Distinct card shown on the customer's BookingCard when a completion OTP is live.
 *
 * Visibility rule: !isPurohitView && status == ACCEPTED && completionOtp != null
 * Auto-disappears when booking moves to COMPLETED (completionOtp deleted from Firestore,
 * Firestore listener pushes the update, recomposition clears this card).
 */
@Composable
fun CompletionOtpChip(otp: String, isDark: Boolean) {
    val accentColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)  // Blue 200 / Blue 800
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = if (isDark) 0.12f else 0.07f)
        ),
        border    = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.40f else 0.30f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = "Your Completion Code",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp,
                    color      = accentColor
                )
            }
            Text(
                text       = otp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize   = 28.sp,
                letterSpacing = 8.sp,
                color      = accentColor
            )
            Text(
                text       = "Share this code with the purohit to complete your booking.",
                fontFamily = FontFamily.Serif,
                fontSize   = 11.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )
        }
    }
}