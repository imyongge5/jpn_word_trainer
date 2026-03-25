package com.example.wordbookapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryCoral,
    tertiary = SecondaryCoral,
    background = Paper,
    surface = PaperElevated,
    surfaceVariant = SurfaceSoft,
    primaryContainer = PrimaryBlueContainer,
    secondaryContainer = SecondaryCoralContainer,
    onPrimary = Paper,
    onSecondary = Ink,
    onPrimaryContainer = Ink,
    onSecondaryContainer = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = InkSoft,
    outline = CardBorderStrong,
    outlineVariant = DividerSoft,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun WordbookAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
