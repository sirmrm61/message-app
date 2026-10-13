package com.sirmrm.messageapp.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Minimal iOS-inspired palette. Flat surfaces, hairline separators, and color
 * only where it carries meaning: one accent blue for actions, green for
 * deposits, red for withdrawals/danger, amber for warnings.
 */
object AppPalette {
    // accent + semantic
    val Blue = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val Red = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val Orange = Color(0xFFFF9500)
    val OrangeDark = Color(0xFFFF9F0A)

    // light surfaces (systemGroupedBackground family)
    val GroupedLight = Color(0xFFF2F2F7)
    val CardLight = Color(0xFFFFFFFF)
    val FillLight = Color(0xFFE5E5EA)      // search field / segmented track
    val SeparatorLight = Color(0x1F000000) // hairline, ~12% black
    val LabelLight = Color(0xFF000000)
    val Label2Light = Color(0x993C3C43)    // secondary label
    val Label3Light = Color(0x4D3C3C43)    // tertiary label

    // dark surfaces
    val GroupedDark = Color(0xFF000000)
    val CardDark = Color(0xFF1C1C1E)
    val FillDark = Color(0xFF2C2C2E)
    val SeparatorDark = Color(0x26FFFFFF)  // ~15% white
    val LabelDark = Color(0xFFFFFFFF)
    val Label2Dark = Color(0x99EBEBF5)
    val Label3Dark = Color(0x4DEBEBF5)
}

/** Semantic extras the Material color scheme has no slot for. */
class AppExtendedColors(
    val groupedBackground: Color,
    val card: Color,
    val fill: Color,
    val separator: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val deposit: Color,
    val withdrawal: Color,
    val warning: Color,
)

val LocalAppColors = staticCompositionLocalOf {
    AppExtendedColors(
        groupedBackground = AppPalette.GroupedLight,
        card = AppPalette.CardLight,
        fill = AppPalette.FillLight,
        separator = AppPalette.SeparatorLight,
        secondaryLabel = AppPalette.Label2Light,
        tertiaryLabel = AppPalette.Label3Light,
        deposit = AppPalette.Green,
        withdrawal = AppPalette.Red,
        warning = AppPalette.Orange,
    )
}

fun appLightColorScheme() = lightColorScheme(
    primary = AppPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = AppPalette.Blue.copy(alpha = 0.12f),
    onPrimaryContainer = AppPalette.Blue,
    secondary = AppPalette.Label2Light,
    onSecondary = Color.White,
    secondaryContainer = AppPalette.FillLight,
    onSecondaryContainer = AppPalette.LabelLight,
    tertiary = AppPalette.Orange,
    background = AppPalette.GroupedLight,
    onBackground = AppPalette.LabelLight,
    surface = AppPalette.CardLight,
    onSurface = AppPalette.LabelLight,
    surfaceVariant = AppPalette.FillLight,
    onSurfaceVariant = AppPalette.Label2Light,
    surfaceContainer = AppPalette.CardLight,
    surfaceContainerLow = AppPalette.GroupedLight,
    surfaceContainerHighest = AppPalette.FillLight,
    outline = AppPalette.SeparatorLight,
    outlineVariant = AppPalette.SeparatorLight,
    error = AppPalette.Red,
    onError = Color.White,
    errorContainer = AppPalette.Red.copy(alpha = 0.12f),
    onErrorContainer = AppPalette.Red,
)

fun appDarkColorScheme() = darkColorScheme(
    primary = AppPalette.BlueDark,
    onPrimary = Color.White,
    primaryContainer = AppPalette.BlueDark.copy(alpha = 0.22f),
    onPrimaryContainer = AppPalette.BlueDark,
    secondary = AppPalette.Label2Dark,
    onSecondary = Color.Black,
    secondaryContainer = AppPalette.FillDark,
    onSecondaryContainer = AppPalette.LabelDark,
    tertiary = AppPalette.OrangeDark,
    background = AppPalette.GroupedDark,
    onBackground = AppPalette.LabelDark,
    surface = AppPalette.CardDark,
    onSurface = AppPalette.LabelDark,
    surfaceVariant = AppPalette.FillDark,
    onSurfaceVariant = AppPalette.Label2Dark,
    surfaceContainer = AppPalette.CardDark,
    surfaceContainerLow = AppPalette.GroupedDark,
    surfaceContainerHighest = AppPalette.FillDark,
    outline = AppPalette.SeparatorDark,
    outlineVariant = AppPalette.SeparatorDark,
    error = AppPalette.RedDark,
    onError = Color.Black,
    errorContainer = AppPalette.RedDark.copy(alpha = 0.22f),
    onErrorContainer = AppPalette.RedDark,
)

fun appExtendedColors(dark: Boolean) = if (dark) AppExtendedColors(
    groupedBackground = AppPalette.GroupedDark,
    card = AppPalette.CardDark,
    fill = AppPalette.FillDark,
    separator = AppPalette.SeparatorDark,
    secondaryLabel = AppPalette.Label2Dark,
    tertiaryLabel = AppPalette.Label3Dark,
    deposit = AppPalette.GreenDark,
    withdrawal = AppPalette.RedDark,
    warning = AppPalette.OrangeDark,
) else AppExtendedColors(
    groupedBackground = AppPalette.GroupedLight,
    card = AppPalette.CardLight,
    fill = AppPalette.FillLight,
    separator = AppPalette.SeparatorLight,
    secondaryLabel = AppPalette.Label2Light,
    tertiaryLabel = AppPalette.Label3Light,
    deposit = AppPalette.Green,
    withdrawal = AppPalette.Red,
    warning = AppPalette.Orange,
)
