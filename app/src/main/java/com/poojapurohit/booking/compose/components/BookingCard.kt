package com.poojapurohit.booking.compose.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun BookingCard(
    booking: Booking,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else LightSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: service name + status chip
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

            // Purohit name
            LabelValueRow(
                label = "Purohit",
                value = booking.purohitName.ifBlank { "—" }
            )

            // Scheduled date
            booking.scheduledDate?.let { ts ->
                val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(ts.toDate())
                LabelValueRow(label = "Scheduled", value = formatted)
            }

            // Address
            if (booking.address.isNotBlank()) {
                LabelValueRow(label = "Address", value = booking.address)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount row
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
        BookingStatus.PENDING_PAYMENT ->
            Color(0xFFFFF3CD) to Color(0xFF856404)

        BookingStatus.PAYMENT_DONE ->
            Color(0xFFD1ECF1) to Color(0xFF0C5460)

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

        BookingStatus.REFUNDED ->
            Color(0xFFE8D5F5) to Color(0xFF6F42C1)
    }
}
