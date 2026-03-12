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
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poojapurohit.bookpurohit.compose.BookPurohitViewModel
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
        composable("location_selection") {
            LocationSelectionScreen(
                viewModel = viewModel,
                onBackPressed = {
                    navController.popBackStack()
                },
                onLocationClick = { locationId ->
                    navController.navigate("sublocation_selection/$locationId")
                }
            )
        }

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
    }
}