package com.mistbottle.jpnwordtrainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.mistbottle.jpnwordtrainer.data.model.ThemePreset

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun WordbookAppTheme(
    themePreset: ThemePreset,
    content: @Composable () -> Unit,
) {
    val palette = rememberPaletteForPreset(themePreset)
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accentPrimary,
            secondary = palette.accentSecondary,
            tertiary = palette.accentSecondary,
            background = palette.paper,
            surface = palette.paperElevated,
            surfaceVariant = palette.surfaceSoft,
            primaryContainer = palette.accentPrimarySoft,
            secondaryContainer = palette.accentSecondarySoft,
            onPrimary = palette.paperElevated,
            onSecondary = palette.paperElevated,
            onPrimaryContainer = palette.ink,
            onSecondaryContainer = palette.ink,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onSurfaceVariant = palette.inkSoft,
            outline = palette.border,
            outlineVariant = palette.border.copy(alpha = 0.7f),
        )
    } else {
        lightColorScheme(
            primary = palette.accentPrimary,
            secondary = palette.accentSecondary,
            tertiary = palette.accentSecondary,
            background = palette.paper,
            surface = palette.paperElevated,
            surfaceVariant = palette.surfaceSoft,
            primaryContainer = palette.accentPrimarySoft,
            secondaryContainer = palette.accentSecondarySoft,
            onPrimary = palette.paper,
            onSecondary = palette.ink,
            onPrimaryContainer = palette.ink,
            onSecondaryContainer = palette.ink,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onSurfaceVariant = palette.inkSoft,
            outline = palette.border,
            outlineVariant = palette.border.copy(alpha = 0.7f),
        )
    }

    SideEffect {
        activeThemePalette = palette
    }

    CompositionLocalProvider(LocalWordbookThemeTokens provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
