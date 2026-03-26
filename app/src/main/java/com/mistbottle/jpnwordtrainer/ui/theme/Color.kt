package com.mistbottle.jpnwordtrainer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.mistbottle.jpnwordtrainer.data.model.ThemePreset

@Stable
data class AppThemePalette(
    val preset: ThemePreset,
    val isDark: Boolean,
    val paper: Color,
    val paperElevated: Color,
    val surfaceSoft: Color,
    val surfaceContainer: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMuted: Color,
    val accentPrimary: Color,
    val accentPrimarySoft: Color,
    val accentSecondary: Color,
    val accentSecondarySoft: Color,
    val examAction: Color,
    val examActionSoft: Color,
    val border: Color,
    val swatches: List<Color>,
)

private val DefaultLightPalette = AppThemePalette(
    preset = ThemePreset.DEFAULT_LIGHT,
    isDark = false,
    paper = oklch(0.97, 0.01, 95.0),
    paperElevated = oklch(0.985, 0.008, 95.0),
    surfaceSoft = oklch(0.955, 0.01, 250.0),
    surfaceContainer = oklch(0.945, 0.012, 250.0),
    ink = oklch(0.24, 0.02, 255.0),
    inkSoft = oklch(0.48, 0.025, 255.0),
    inkMuted = oklch(0.60, 0.02, 255.0),
    accentPrimary = oklch(0.59, 0.085, 250.0),
    accentPrimarySoft = oklch(0.86, 0.028, 250.0),
    accentSecondary = oklch(0.66, 0.11, 32.0),
    accentSecondarySoft = oklch(0.88, 0.03, 32.0),
    examAction = oklch(0.58, 0.09, 145.0),
    examActionSoft = oklch(0.88, 0.035, 145.0),
    border = oklch(0.76, 0.018, 250.0),
    swatches = listOf(
        oklch(0.97, 0.01, 95.0),
        oklch(0.985, 0.008, 95.0),
        oklch(0.59, 0.085, 250.0),
        oklch(0.66, 0.11, 32.0),
        oklch(0.58, 0.09, 145.0),
        oklch(0.24, 0.02, 255.0),
    ),
)

private val VscodeDarkModernPalette = AppThemePalette(
    preset = ThemePreset.VSCODE_DARK_MODERN,
    isDark = true,
    paper = hexColor(0x1F1F1F),
    paperElevated = hexColor(0x181818),
    surfaceSoft = hexColor(0x313131),
    surfaceContainer = hexColor(0x252526),
    ink = hexColor(0xCCCCCC),
    inkSoft = hexColor(0x9D9D9D),
    inkMuted = hexColor(0x868686),
    accentPrimary = hexColor(0x0078D4),
    accentPrimarySoft = hexColor(0x264778),
    accentSecondary = hexColor(0x4DAAFC),
    accentSecondarySoft = hexColor(0x2B2B2B),
    examAction = hexColor(0x2EA043),
    examActionSoft = hexColor(0x173A24),
    border = hexColor(0x2B2B2B),
    swatches = listOf(
        hexColor(0x1F1F1F),
        hexColor(0x181818),
        hexColor(0x0078D4),
        hexColor(0x4DAAFC),
        hexColor(0x2EA043),
        hexColor(0xCCCCCC),
    ),
)

private val VscodeHighContrastPalette = AppThemePalette(
    preset = ThemePreset.VSCODE_HIGH_CONTRAST,
    isDark = true,
    paper = Color.Black,
    paperElevated = hexColor(0x0F0F0F),
    surfaceSoft = hexColor(0x151515),
    surfaceContainer = hexColor(0x1F1F1F),
    ink = Color.White,
    inkSoft = hexColor(0xEAEAEA),
    inkMuted = hexColor(0xBDBDBD),
    accentPrimary = hexColor(0x00A6FF),
    accentPrimarySoft = hexColor(0x003B57),
    accentSecondary = hexColor(0xFFD400),
    accentSecondarySoft = hexColor(0x3D3300),
    examAction = hexColor(0x00C853),
    examActionSoft = hexColor(0x003D18),
    border = Color.White,
    swatches = listOf(
        Color.Black,
        Color.White,
        hexColor(0x00A6FF),
        hexColor(0xFFD400),
        hexColor(0x00C853),
        hexColor(0x1F1F1F),
    ),
)

private val OneDarkProPalette = AppThemePalette(
    preset = ThemePreset.ONE_DARK_PRO,
    isDark = true,
    paper = hexColor(0x282C34),
    paperElevated = hexColor(0x21252B),
    surfaceSoft = hexColor(0x1D1F23),
    surfaceContainer = hexColor(0x2F343D),
    ink = hexColor(0xABB2BF),
    inkSoft = hexColor(0x8F96A3),
    inkMuted = hexColor(0x6B7280),
    accentPrimary = hexColor(0x61AFEF),
    accentPrimarySoft = hexColor(0x2C425B),
    accentSecondary = hexColor(0xC678DD),
    accentSecondarySoft = hexColor(0x48334F),
    examAction = hexColor(0x98C379),
    examActionSoft = hexColor(0x233222),
    border = hexColor(0x3E4452),
    swatches = listOf(
        hexColor(0x282C34),
        hexColor(0x21252B),
        hexColor(0x61AFEF),
        hexColor(0xC678DD),
        hexColor(0x98C379),
        hexColor(0xABB2BF),
    ),
)

private fun paletteFor(preset: ThemePreset): AppThemePalette = when (preset) {
    ThemePreset.DEFAULT_LIGHT -> DefaultLightPalette
    ThemePreset.VSCODE_DARK_MODERN -> VscodeDarkModernPalette
    ThemePreset.VSCODE_HIGH_CONTRAST -> VscodeHighContrastPalette
    ThemePreset.ONE_DARK_PRO -> OneDarkProPalette
}

internal val LocalWordbookThemeTokens = staticCompositionLocalOf { DefaultLightPalette }

internal var activeThemePalette by mutableStateOf(DefaultLightPalette)

object WordbookTheme {
    val tokens: AppThemePalette
        @Composable get() = LocalWordbookThemeTokens.current
}

internal fun rememberPaletteForPreset(preset: ThemePreset): AppThemePalette = paletteFor(preset)

internal fun themePaletteForPreset(preset: ThemePreset): AppThemePalette = paletteFor(preset)

private fun hexColor(hex: Long): Color = Color(0xFF000000 or hex)

val Paper: Color
    get() = activeThemePalette.paper

val PaperElevated: Color
    get() = activeThemePalette.paperElevated

val SurfaceSoft: Color
    get() = activeThemePalette.surfaceSoft

val SurfaceContainer: Color
    get() = activeThemePalette.surfaceContainer

val Ink: Color
    get() = activeThemePalette.ink

val InkSoft: Color
    get() = activeThemePalette.inkSoft

val InkMuted: Color
    get() = activeThemePalette.inkMuted

val PrimaryBlue: Color
    get() = activeThemePalette.accentPrimary

val PrimaryBlueSoft: Color
    get() = activeThemePalette.accentPrimarySoft

val PrimaryBlueContainer: Color
    get() = activeThemePalette.accentPrimarySoft

val SecondaryCoral: Color
    get() = activeThemePalette.accentSecondary

val SecondaryCoralSoft: Color
    get() = activeThemePalette.accentSecondarySoft

val SecondaryCoralContainer: Color
    get() = activeThemePalette.accentSecondarySoft

val ExamGreen: Color
    get() = activeThemePalette.examAction

val ExamGreenSoft: Color
    get() = activeThemePalette.examActionSoft

val DividerSoft: Color
    get() = activeThemePalette.border.copy(alpha = if (activeThemePalette.isDark) 0.8f else 0.7f)

val CardBorderStrong: Color
    get() = activeThemePalette.border
