package com.poojapurohit.booking.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * @param booking             Booking data to display.
 * @param isHighlighted       True when navigated to via a deep link — renders with
 *                            an accent border and elevated shadow for 2 s.
 * @param isPurohitView       True when the signed-in user is the purohit on this booking.
 *                            Shows Accept / Reject (on PAYMENT_DONE) or Mark as Completed
 *                            (on ACCEPTED) instead of Cancel.
 * @param onCompletePayment   Invoked on PENDING_PAYMENT bookings (user view only).
 * @param onAccept            Invoked by purohit on PAYMENT_DONE bookings.
 * @param onReject            Invoked by purohit on PAYMENT_DONE bookings.
 * @param onComplete          Invoked by purohit on ACCEPTED bookings to mark the pooja done.
 * @param onCancel            Invoked by user on PENDING_PAYMENT / PAYMENT_DONE / ACCEPTED.
 */
@Composable
fun BookingCard(
    booking: Booking,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isPurohitView: Boolean = false,
    onCompletePayment: ((Booking) -> Unit)? = null,
    onAccept: ((Booking) -> Unit)? = null,
    onReject: ((Booking) -> Unit)? = null,
    onComplete: ((Booking) -> Unit)? = null,
    onCancel: ((Booking) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    val cardContainerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            if (isDark) Color(0xFF3A2A10) else Color(0xFFFFF3D0)
        } else {
            if (isDark) DarkSurface else LightSurface
        },
        animationSpec = tween(400),
        label = "card_highlight_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = if (isHighlighted) {
            BorderStroke(1.5.dp, if (isDark) DarkBrandOrange else BrandOrange)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Service name + status chip ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.serviceName.ifBlank { "Pooja Service" },
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LabelValueRow(label = "Purohit", value = booking.purohitName.ifBlank { "—" })

            booking.scheduledDate?.let { ts ->
                val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(ts.toDate())
                LabelValueRow(label = "Scheduled", value = formatted)
            }

            if (booking.address.isNotBlank()) {
                LabelValueRow(label = "Address", value = booking.address)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Amount ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "₹${booking.amount}",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) DarkBrandOrange else BrandOrange
                )
            }

            // ── Action button visibility flags ────────────────────────────────

            // Purohit: PAYMENT_DONE → Accept / Reject
            val showPurohitActions = isPurohitView &&
                    booking.status == BookingStatus.PAYMENT_DONE &&
                    (onAccept != null || onReject != null)

            // Purohit: ACCEPTED → Mark as Completed
            val showCompleteAction = isPurohitView &&
                    booking.status == BookingStatus.ACCEPTED &&
                    onComplete != null

            // User: PENDING_PAYMENT → Complete Payment (+ optional Cancel below it)
            val showCompletePayment = !isPurohitView &&
                    booking.status == BookingStatus.PENDING_PAYMENT &&
                    onCompletePayment != null

            // User: PENDING_PAYMENT / PAYMENT_DONE / ACCEPTED → Cancel
            val showCancelOnly = !isPurohitView &&
                    booking.status in listOf(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PAYMENT_DONE,
                BookingStatus.ACCEPTED
            ) && onCancel != null

            if (showPurohitActions || showCompleteAction || showCompletePayment || showCancelOnly) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    // ── Purohit: Accept / Reject ──────────────────────────────
                    showPurohitActions -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onReject?.invoke(booking) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, DeleteRed),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DeleteRed
                                )
                            ) {
                                Text(
                                    text = "Reject",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = { onAccept?.invoke(booking) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CallGreen
                                )
                            ) {
                                Text(
                                    text = "Accept",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ── Purohit: Mark as Completed ────────────────────────────
                    showCompleteAction -> {
                        Button(
                            onClick = { onComplete.invoke(booking) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) DarkBrandOrange else BrandOrange
                            )
                        ) {
                            Text(
                                text = "Mark as Completed",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    // ── User: PENDING_PAYMENT — Complete Payment + Cancel ─────
                    showCompletePayment -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onCompletePayment.invoke(booking) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) DarkBrandOrange else BrandOrange
                                )
                            ) {
                                Text(
                                    text = "Complete Payment",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            if (onCancel != null) {
                                OutlinedButton(
                                    onClick = { onCancel.invoke(booking) },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, DeleteRed),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = DeleteRed
                                    )
                                ) {
                                    Text(
                                        text = "Cancel Booking",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── User: PAYMENT_DONE or ACCEPTED — Cancel only ──────────
                    else -> {
                        OutlinedButton(
                            onClick = { onCancel?.invoke(booking) },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, DeleteRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DeleteRed
                            )
                        ) {
                            Text(
                                text = "Cancel Booking",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusChip(status: BookingStatus) {
    val (bgColor, textColor) = statusChipColors(status)
    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayLabel,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
private fun statusChipColors(status: BookingStatus): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return when (status) {
        BookingStatus.PENDING_PAYMENT -> Color(0xFFFFF3CD) to Color(0xFF856404)
        BookingStatus.PAYMENT_DONE -> Color(0xFFD1ECF1) to Color(0xFF0C5460)
        BookingStatus.ACCEPTED ->
            if (isDark) Color(0xFF1B4332) to CallGreen
            else Color(0xFFD4EDDA) to Color(0xFF155724)
        BookingStatus.COMPLETED ->
            if (isDark) Color(0xFF1B3A4B) to BrandGold
            else Color(0xFFCCE5FF) to Color(0xFF004085)
        BookingStatus.REJECTED,
        BookingStatus.CANCELLED,
        BookingStatus.AUTO_CANCELLED ->
            if (isDark) Color(0xFF3B1A1A) to DeleteRed
            else Color(0xFFF8D7DA) to BrandRed
        BookingStatus.REFUNDED -> Color(0xFFE8D5F5) to Color(0xFF6F42C1)
    }
}