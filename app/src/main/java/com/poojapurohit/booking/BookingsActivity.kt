package com.poojapurohit.booking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.poojapurohit.booking.compose.presentation.screens.BookingsScreen
import com.poojapurohit.dashboard.compose.theme.PoojaPurohitTheme

/**
 * Host Activity for the Bookings screen.
 *
 * ── Entry points ─────────────────────────────────────────────────────────────
 *
 * 1. Normal launch (dashboard bottom nav / tab):
 *      startActivity(Intent(context, BookingsActivity::class.java))
 *    Opens the Active tab, no highlight.
 *
 * 2. Deep link from a notification tap:
 *      Intent(ACTION_VIEW, "poojapurohit://bookings/{bookingId}".toUri())
 *    Extracts the bookingId, passes it to BookingsScreen which jumps to the
 *    correct tab and highlights the card for 2 s.
 *
 * ── AndroidManifest.xml ──────────────────────────────────────────────────────
 * Add this inside <application>:
 *
 *   <activity
 *       android:name=".booking.BookingsActivity"
 *       android:launchMode="singleTop"
 *       android:exported="true">
 *
 *       <!-- Normal launch -->
 *       <intent-filter>
 *           <action android:name="android.intent.action.MAIN" />
 *       </intent-filter>
 *
 *       <!-- Deep link: poojapurohit://bookings/{bookingId} -->
 *       <intent-filter>
 *           <action android:name="android.intent.action.VIEW" />
 *           <category android:name="android.intent.category.DEFAULT" />
 *           <category android:name="android.intent.category.BROWSABLE" />
 *           <data
 *               android:scheme="poojapurohit"
 *               android:host="bookings" />
 *       </intent-filter>
 *
 *   </activity>
 */
class BookingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        renderScreen(intent)
    }

    /**
     * Called when the activity is already running (launchMode="singleTop") and a new
     * deep link arrives — e.g. the user taps a second notification while the screen
     * is already open.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderScreen(intent)
    }

    private fun renderScreen(intent: Intent?) {
        val bookingId = extractBookingId(intent)
        setContent {
            PoojaPurohitTheme {
                SetSystemBarsColor()
                BookingsScreen(
                    highlightBookingId = bookingId,
                    initialTabIndex = 0
                )
            }
        }
    }

    /**
     * Extracts the bookingId from a deep link URI.
     *
     * URI format:  poojapurohit://bookings/{bookingId}
     * pathSegments: ["bookingId"]   ← first segment after the host
     *
     * Returns null for normal (non-deep-link) launches.
     */
    private fun extractBookingId(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        if (uri.scheme != "poojapurohit" || uri.host != "bookings") return null
        return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    @Composable
    private fun SetSystemBarsColor() {
        val isDark = isSystemInDarkTheme()
        val statusBarColor = Color(if (isDark) 0xFF5E1100 else 0xFF811C01)
        DisposableEffect(isDark) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
            controller.isAppearanceLightStatusBars = false
            onDispose { }
        }
    }
}
