package com.sirmrm.messageapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.sirmrm.messageapp.core.R
import com.sirmrm.messageapp.core.designsystem.LocalAppColors
import com.sirmrm.messageapp.core.designsystem.appDarkColorScheme
import com.sirmrm.messageapp.core.designsystem.appExtendedColors
import com.sirmrm.messageapp.core.designsystem.appLightColorScheme
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.ThemeMode

val VazirmatnFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Light),
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.Bold),
)

/**
 * iOS-like type scale on Vazirmatn: hierarchy through size and weight.
 * Persian line heights ~1.5.
 */
private fun appTypography() = Typography(
    displayLarge = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp),
    displayMedium = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    displaySmall = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineLarge = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = VazirmatnFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

/**
 * App-wide theme: fixed minimal iOS-like palette (no dynamic color), Vazirmatn
 * for every text style, and layout direction from the app language.
 */
@Composable
fun MessageAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.FA,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) appDarkColorScheme() else appLightColorScheme()
    val direction = if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalAppColors provides appExtendedColors(dark),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(),
            content = content,
        )
    }
}
