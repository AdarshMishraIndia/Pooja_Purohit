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
import com.poojapurohit.ui.theme.PoojaPurohitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        renderScreen(intent)
    }

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
                BookingsScreen(highlightBookingId = bookingId)
            }
        }
    }

    private fun extractBookingId(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        if (uri.scheme != "poojapurohit" || uri.host != "bookings") return null
        return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    @Composable
    private fun SetSystemBarsColor() {
        val isDark = isSystemInDarkTheme()
        DisposableEffect(isDark) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            // Transparent — lets BookingsScreen header gradient bleed behind status bar
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            controller.isAppearanceLightStatusBars = false
            onDispose { }
        }
    }
}