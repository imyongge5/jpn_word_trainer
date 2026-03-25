package com.example.wordbookapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Coral,
    background = Sand,
    surface = Mist,
    onPrimary = Sand,
    onSecondary = Ink,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Coral,
    secondary = Teal,
    background = Ink,
    surface = ColorTokens.DarkSurface,
    onPrimary = Ink,
    onSecondary = Ink,
    onBackground = Sand,
    onSurface = Sand,
)

@Composable
fun WordbookAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}

private object ColorTokens {
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF2A3441)
}
