package com.cleanroom.weekplate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Sage,
    onPrimaryContainer = Forest,
    secondary = Tangerine,
    onSecondary = Ink,
    secondaryContainer = Peach,
    onSecondaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0E9DE),
    onSurfaceVariant = Muted,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAED1C0),
    onPrimary = Color(0xFF07372B),
    primaryContainer = ForestSoft,
    secondary = Color(0xFFFFB86B),
    background = Color(0xFF111612),
    surface = Color(0xFF191E1A),
    onBackground = Color(0xFFE7E9E4),
    onSurface = Color(0xFFE7E9E4)
)

@Composable
fun WeekplateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
