package com.poojapurohit.dashboard

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
import com.poojapurohit.dashboard.compose.presentation.screens.EditProfileScreen
import com.poojapurohit.dashboard.compose.theme.PoojaPurohitTheme

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PoojaPurohitTheme {
                SetSystemBarsColor()
                EditProfileScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }

    @Composable
    private fun SetSystemBarsColor() {
        val isDark = isSystemInDarkTheme()
        val statusBarColor = Color(if (isDark) 0xFF5E1100 else 0xFF811C01)

        DisposableEffect(isDark) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
            windowInsetsController.isAppearanceLightStatusBars = false

            onDispose { }
        }
    }
}