package com.poojapurohit.bookpurohit.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.ui.theme.BrandOrange
import com.poojapurohit.ui.theme.BrandRed
import com.poojapurohit.ui.theme.DarkBrandOrange
import com.poojapurohit.ui.theme.DarkBrandRed
import com.poojapurohit.ui.theme.DarkWelcomeBannerEnd
import com.poojapurohit.ui.theme.DarkWelcomeBannerStart
import com.poojapurohit.ui.theme.WelcomeBannerEnd
import com.poojapurohit.ui.theme.WelcomeBannerStart

private const val APP_TITLE = "POOJA PUROHIT (ପୂଜା ପୁରୋହିତ)"

/**
 * Shared top bar for all BookPurohit flow screens.
 *
 * Replaces the three duplicated composables:
 *   - BookPurohitTopBar  (LocationSelectionScreen)
 *   - SubLocationTopBar  (SubLocationSelectionScreen)
 *   - PurohitTopBar      (PurohitSelectionScreen)
 *
 * @param bannerTitle Text shown in the gradient banner below the app bar,
 *                    e.g. "Select Location", "Select Area", "Select Purohit".
 * @param onBackPressed Called when the back arrow is tapped.
 * @param isDark        Whether the system is in dark theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPurohitTopBar(
    bannerTitle: String,
    onBackPressed: () -> Unit,
    isDark: Boolean
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = APP_TITLE,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(DarkBrandOrange, DarkBrandRed)
                    else listOf(BrandOrange, BrandRed),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(DarkWelcomeBannerStart, DarkWelcomeBannerEnd)
                        else listOf(WelcomeBannerStart, WelcomeBannerEnd)
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bannerTitle,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange,
                textAlign = TextAlign.Center
            )
        }
    }
}