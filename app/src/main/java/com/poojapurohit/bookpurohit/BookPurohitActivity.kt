package com.poojapurohit.bookpurohit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poojapurohit.bookpurohit.compose.BookPurohitViewModel
import com.poojapurohit.bookpurohit.compose.presentation.screens.BookingScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.LocationSelectionScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.PurohitSelectionScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.SubLocationSelectionScreen
import com.poojapurohit.dashboard.compose.theme.PoojaPurohitTheme

class BookPurohitActivity : ComponentActivity() {
    private val viewModel: BookPurohitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PoojaPurohitTheme {
                SetSystemBarsColor()
                BookPurohitNavigation(viewModel = viewModel)
            }
        }
    }

    @Composable
    private fun SetSystemBarsColor() {
        val isDark = isSystemInDarkTheme()
        // Using the brand colors from your theme setup
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

@Composable
fun BookPurohitNavigation(viewModel: BookPurohitViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "location_selection"
    ) {
        // 1. Location Selection Screen
        composable("location_selection") {
            // Get the context here to finish the activity
            val context = LocalContext.current

            LocationSelectionScreen(
                viewModel = viewModel,
                onBackPressed = {
                    // This will close BookPurohitActivity and take you back to DashActivity
                    (context as? android.app.Activity)?.finish()
                },
                onLocationClick = { locationId ->
                    navController.navigate("sublocation_selection/$locationId")
                }
            )
        }

        // 2. Sub-Location Selection Screen
        composable(
            route = "sublocation_selection/{locationId}",
            arguments = listOf(
                navArgument("locationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId") ?: return@composable

            SubLocationSelectionScreen(
                viewModel = viewModel,
                locationId = locationId,
                onBackPressed = {
                    navController.popBackStack()
                },
                onSubLocationClick = { locId, subLocId ->
                    navController.navigate("purohit_selection/$locId/$subLocId")
                }
            )
        }

        // 3. Purohit Selection Screen
        composable(
            route = "purohit_selection/{locationId}/{subLocationId}",
            arguments = listOf(
                navArgument("locationId") { type = NavType.StringType },
                navArgument("subLocationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId") ?: return@composable
            val subLocationId = backStackEntry.arguments?.getString("subLocationId") ?: return@composable

            PurohitSelectionScreen(
                viewModel = viewModel,
                locationId = locationId,
                subLocationId = subLocationId,
                onBackPressed = {
                    navController.popBackStack()
                },
                onBookClick = { purohit ->
                    navController.navigate("booking/${purohit.id}")
                }
            )
        }

        // 4. Booking Screen (Checkout & Razorpay Stub)
        composable(
            route = "booking/{purohitId}",
            arguments = listOf(
                navArgument("purohitId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val purohitId = backStackEntry.arguments?.getString("purohitId") ?: return@composable

            BookingScreen(
                purohitId = purohitId,
                onBackPressed = {
                    navController.popBackStack()
                },
                onBookingSuccess = {
                    // On success, pop all the way back to the start of the flow
                    navController.popBackStack("location_selection", inclusive = false)
                }
            )
        }
    }
}