package com.poojapurohit.dashboard.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = TextWhite,
    secondary = BrandRed,
    onSecondary = TextWhite,
    tertiary = BrandGold,
    background = LightBackground,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandOrange,
    onPrimary = TextWhite,
    secondary = DarkBrandRed,
    onSecondary = TextWhite,
    tertiary = BrandGold,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun PoojaPurohitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PoojaPurohitTypography,
        content = content
    )
}