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
import android.net.Uri
import com.poojapurohit.bookpurohit.compose.BookPurohitViewModel
import com.poojapurohit.bookpurohit.compose.presentation.screens.CheckoutScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.LocationSelectionScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.PurohitSelectionScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.ServiceSelectionScreen
import com.poojapurohit.bookpurohit.compose.presentation.screens.SubLocationSelectionScreen
import com.poojapurohit.ui.theme.PoojaPurohitTheme

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
        startDestination = "service_selection"
    ) {
        // 1. Service Selection Screen
        composable("service_selection") {
            val context = LocalContext.current

            ServiceSelectionScreen(
                viewModel = viewModel,
                onBackPressed = {
                    (context as? android.app.Activity)?.finish()
                },
                onServiceClick = { serviceSlug ->
                    navController.navigate("location_selection/$serviceSlug")
                }
            )
        }

        // 2. Location Selection Screen
        composable(
            route = "location_selection/{serviceSlug}",
            arguments = listOf(
                navArgument("serviceSlug") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceSlug = backStackEntry.arguments?.getString("serviceSlug") ?: return@composable

            LocationSelectionScreen(
                viewModel = viewModel,
                serviceSlug = serviceSlug,
                onBackPressed = {
                    viewModel.resetToServices()
                    navController.popBackStack()
                },
                onLocationClick = { locationId ->
                    navController.navigate("sublocation_selection/$serviceSlug/$locationId")
                }
            )
        }

        // 3. Sub-Location Selection Screen
        composable(
            route = "sublocation_selection/{serviceSlug}/{locationId}",
            arguments = listOf(
                navArgument("serviceSlug") { type = NavType.StringType },
                navArgument("locationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceSlug = backStackEntry.arguments?.getString("serviceSlug") ?: return@composable
            val locationId = backStackEntry.arguments?.getString("locationId") ?: return@composable

            SubLocationSelectionScreen(
                viewModel = viewModel,
                locationId = locationId,
                onBackPressed = {
                    navController.popBackStack()
                },
                onSubLocationClick = { locId, subLocId ->
                    navController.navigate("purohit_selection/$serviceSlug/$locId/$subLocId")
                }
            )
        }

        // 4. Purohit Selection Screen
        composable(
            route = "purohit_selection/{serviceSlug}/{locationId}/{subLocationId}",
            arguments = listOf(
                navArgument("serviceSlug") { type = NavType.StringType },
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
                    val serviceName = Uri.encode(viewModel.uiState.value.selectedServiceName)
                    val servicePrice = viewModel.uiState.value.selectedServicePrice ?: 0
                    navController.navigate("booking/${purohit.id}/$serviceName/$servicePrice")
                }
            )
        }

        // 5. Booking Screen (Checkout & Razorpay Stub)
        composable(
            route = "booking/{purohitId}/{serviceName}/{servicePrice}",
            arguments = listOf(
                navArgument("purohitId") { type = NavType.StringType },
                navArgument("serviceName") { type = NavType.StringType },
                navArgument("servicePrice") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val purohitId = backStackEntry.arguments?.getString("purohitId") ?: return@composable

            CheckoutScreen(
                purohitId = purohitId,
                onBackPressed = {
                    navController.popBackStack()
                },
                onBookingSuccess = {
                    navController.popBackStack("service_selection", inclusive = false)
                }
            )
        }
    }
}