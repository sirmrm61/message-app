package com.sirmrm.messageapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import com.sirmrm.messageapp.core.R
import com.sirmrm.messageapp.domain.model.AppLanguage
import com.sirmrm.messageapp.domain.model.ThemeMode

val VazirmatnFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.Bold),
)

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

private fun typography() = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = VazirmatnFamily),
        displayMedium = displayMedium.copy(fontFamily = VazirmatnFamily),
        displaySmall = displaySmall.copy(fontFamily = VazirmatnFamily),
        headlineLarge = headlineLarge.copy(fontFamily = VazirmatnFamily),
        headlineMedium = headlineMedium.copy(fontFamily = VazirmatnFamily),
        headlineSmall = headlineSmall.copy(fontFamily = VazirmatnFamily),
        titleLarge = titleLarge.copy(fontFamily = VazirmatnFamily),
        titleMedium = titleMedium.copy(fontFamily = VazirmatnFamily),
        titleSmall = titleSmall.copy(fontFamily = VazirmatnFamily),
        bodyLarge = bodyLarge.copy(fontFamily = VazirmatnFamily, fontSize = 16.sp),
        bodyMedium = bodyMedium.copy(fontFamily = VazirmatnFamily, fontSize = 14.sp),
        bodySmall = bodySmall.copy(fontFamily = VazirmatnFamily, fontSize = 12.sp),
        labelLarge = labelLarge.copy(fontFamily = VazirmatnFamily),
        labelMedium = labelMedium.copy(fontFamily = VazirmatnFamily),
        labelSmall = labelSmall.copy(fontFamily = VazirmatnFamily),
    )
}

/**
 * App-wide theme: Material 3 with dynamic color, Vazirmatn for every text style,
 * and the right layout direction for the active app language.
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
    val context = LocalContext.current
    val colors = when {
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    val direction = if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography(),
            content = content,
        )
    }
}
