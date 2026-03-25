package com.example.wordbookapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryCoral,
    background = Paper,
    surface = PaperElevated,
    surfaceVariant = SurfaceSoft,
    primaryContainer = PrimaryBlueSoft,
    secondaryContainer = SecondaryCoralSoft,
    onPrimary = Paper,
    onSecondary = Ink,
    onPrimaryContainer = Ink,
    onSecondaryContainer = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = InkSoft,
    outlineVariant = DividerSoft,
)

@Composable
fun WordbookAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
