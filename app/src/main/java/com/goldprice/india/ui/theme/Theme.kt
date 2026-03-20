package com.goldprice.india.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = DarkBg,
    primaryContainer = DarkSurface,
    onPrimaryContainer = GoldLight,
    secondary = GoldMid,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    outline = DarkBorder,
)

@Composable
fun GoldPriceIndiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
