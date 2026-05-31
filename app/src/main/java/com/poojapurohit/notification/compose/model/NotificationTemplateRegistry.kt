package com.poojapurohit.notification.compose.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Display configuration for a single notification type.
 *
 * @param icon          Icon shown on the NotificationCard.
 * @param accentLight   Icon tint / accent color in light theme.
 * @param accentDark    Icon tint / accent color in dark theme.
 * @param category      Human-readable group label (used for future filtering UI).
 * @param isActionable  True if a deep link is expected. Controls tap affordance in the card.
 */
data class NotificationTemplate(
    val icon: ImageVector,
    val accentLight: Color,
    val accentDark: Color,
    val category: NotificationCategory,
    val isActionable: Boolean = false
)

enum class NotificationCategory(val label: String) {
    BOOKING("Booking"),
    PAYMENT("Payment"),
    REMINDER("Reminder"),
    PROMOTIONAL("Offers"),
    SYSTEM("System")
}

/**
 * Registry mapping every NotificationType to its NotificationTemplate.
 *
 * ── How to add a new notification type ───────────────────────────────────────
 * 1. Add the enum value to NotificationType.
 * 2. Add one entry here in the registry map.
 * 3. Write the Cloud Function that sends the notification with that type string.
 * Nothing else in the app needs to change.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object NotificationTemplateRegistry {

    private val registry: Map<NotificationType, NotificationTemplate> = mapOf(

        // ── Booking lifecycle ─────────────────────────────────────────────────

        NotificationType.BOOKING_CONFIRMED to NotificationTemplate(
            icon = Icons.Default.CheckCircle,
            accentLight = Color(0xFF2E7D32),  // Green 800
            accentDark = Color(0xFF81C784),   // Green 300
            category = NotificationCategory.BOOKING,
            isActionable = true
        ),

        NotificationType.BOOKING_CANCELLED to NotificationTemplate(
            icon = Icons.Default.Cancel,
            accentLight = Color(0xFFC62828),  // Red 800
            accentDark = Color(0xFFEF9A9A),   // Red 200
            category = NotificationCategory.BOOKING,
            isActionable = true
        ),

        NotificationType.BOOKING_REJECTED to NotificationTemplate(
            icon = Icons.Default.Cancel,
            accentLight = Color(0xFFE64A19),  // Deep Orange 700
            accentDark = Color(0xFFFFAB91),   // Deep Orange 200
            category = NotificationCategory.BOOKING,
            isActionable = true
        ),

        NotificationType.PUROHIT_ACCEPTED to NotificationTemplate(
            icon = Icons.Default.HowToReg,
            accentLight = Color(0xFF1565C0),  // Blue 800
            accentDark = Color(0xFF90CAF9),   // Blue 200
            category = NotificationCategory.BOOKING,
            isActionable = true
        ),

        // ── Payment lifecycle ─────────────────────────────────────────────────

        NotificationType.PAYMENT_DONE to NotificationTemplate(
            icon = Icons.Default.CurrencyRupee,
            accentLight = Color(0xFF2E7D32),
            accentDark = Color(0xFF81C784),
            category = NotificationCategory.PAYMENT,
            isActionable = true
        ),

        NotificationType.PAYMENT_REFUNDED to NotificationTemplate(
            icon = Icons.Default.MoneyOff,
            accentLight = Color(0xFF6A1B9A),  // Purple 800
            accentDark = Color(0xFFCE93D8),   // Purple 200
            category = NotificationCategory.PAYMENT,
            isActionable = true
        ),

        NotificationType.PAYMENT_FAILED to NotificationTemplate(
            icon = Icons.Default.Error,
            accentLight = Color(0xFFB71C1C),  // Red 900
            accentDark = Color(0xFFEF9A9A),
            category = NotificationCategory.PAYMENT,
            isActionable = true
        ),

        // ── Future: Reminders ─────────────────────────────────────────────────

        NotificationType.UPCOMING_RITUAL to NotificationTemplate(
            icon = Icons.Default.EventAvailable,
            accentLight = Color(0xFFE65100),  // Orange 900
            accentDark = Color(0xFFFFCC80),   // Orange 200
            category = NotificationCategory.REMINDER,
            isActionable = true
        ),

        NotificationType.HOLY_DAY_REMINDER to NotificationTemplate(
            icon = Icons.Default.CalendarMonth,
            accentLight = Color(0xFF4527A0),  // Deep Purple 800
            accentDark = Color(0xFFB39DDB),   // Deep Purple 200
            category = NotificationCategory.REMINDER,
            isActionable = false
        ),

        NotificationType.BOOKING_REVIEW_PROMPT to NotificationTemplate(
            icon = Icons.Default.Star,
            accentLight = Color(0xFFF57F17),  // Amber 900
            accentDark = Color(0xFFFFF176),   // Yellow 300
            category = NotificationCategory.REMINDER,
            isActionable = true
        ),

        // ── Future: Promotional ───────────────────────────────────────────────

        NotificationType.PROMO to NotificationTemplate(
            icon = Icons.Default.Campaign,
            accentLight = Color(0xFF00695C),  // Teal 800
            accentDark = Color(0xFF80CBC4),   // Teal 200
            category = NotificationCategory.PROMOTIONAL,
            isActionable = false
        ),

        // ── System ────────────────────────────────────────────────────────────

        NotificationType.SYSTEM_ALERT to NotificationTemplate(
            icon = Icons.Default.Info,
            accentLight = Color(0xFF37474F),  // Blue Grey 800
            accentDark = Color(0xFFB0BEC5),   // Blue Grey 200
            category = NotificationCategory.SYSTEM,
            isActionable = false
        ),

        // ── Fallback ──────────────────────────────────────────────────────────

        NotificationType.GENERAL to NotificationTemplate(
            icon = Icons.Default.Notifications,
            accentLight = Color(0xFF811C01),  // BrandRed
            accentDark = Color(0xFFE07B5A),
            category = NotificationCategory.SYSTEM,
            isActionable = false
        ),

        NotificationType.AUTO_CANCELLED to NotificationTemplate(
            icon = Icons.Default.Autorenew,
            accentLight = Color(0xFF37474F),
            accentDark = Color(0xFFB0BEC5),
            category = NotificationCategory.BOOKING,
            isActionable = true
        ),

        NotificationType.COMPLETION_OTP to NotificationTemplate(
            icon = Icons.Default.VpnKey,
            accentLight = Color(0xFF1565C0),  // Blue 800 — distinct, attention-grabbing
            accentDark  = Color(0xFF90CAF9),  // Blue 200
            category    = NotificationCategory.BOOKING,
            isActionable = true               // taps deep-link to booking detail
        )
    )

    /**
     * Returns the template for the given type.
     * Always returns a valid template — falls back to GENERAL if somehow unmapped.
     */
    fun get(type: NotificationType): NotificationTemplate =
        registry[type] ?: registry[NotificationType.GENERAL]!!
}