package com.poojapurohit.dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.poojapurohit.booking.compose.presentation.screens.BookingsScreen
import com.poojapurohit.dashboard.compose.presentation.screens.DashboardScreen
import com.poojapurohit.dashboard.compose.theme.PoojaPurohitTheme
import kotlinx.coroutines.launch

class DashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern non-deprecated Edge-to-Edge setup
        enableEdgeToEdge()

        setContent {
            PoojaPurohitTheme {
                SetSystemBarsStyle()
                MainNavigationScreen(onExitApp = { finish() })
            }
        }
    }

    @Composable
    private fun SetSystemBarsStyle() {
        val isDark = isSystemInDarkTheme()
        val context = LocalContext.current
        val window = (context as? android.app.Activity)?.window ?: return

        SideEffect {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = !isDark
        }
    }
}

@Composable
fun MainNavigationScreen(onExitApp: () -> Unit) {
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val items = listOf(
        NavigationItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    )

    val pagerState = rememberPagerState(initialPage = 0) { items.size }

    // Double-tap back to exit logic
    BackHandler {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            onExitApp()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(context, "Press back again to exit app", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    val selected = pagerState.currentPage == index
                    val activeColor = if (isSystemInDarkTheme()) Color(0xFFFFAB91) else Color(0xFF811C01)

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            selectedTextColor = activeColor,
                            indicatorColor = activeColor.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> DashboardScreen()
                    1 -> BookingsScreen()
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)