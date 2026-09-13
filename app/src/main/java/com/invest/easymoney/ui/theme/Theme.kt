package com.invest.easymoney.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand / accent (Option 1: Pro Trading)
private val SlateNavy = Color(0xFF0F172A)
private val VividBlue = Color(0xFF2563EB)
private val CyanAccent = Color(0xFF06B6D4)

// Semantic trading colors
private val WarningAmber = Color(0xFFF59E0B)

// Neutral surfaces (light)
private val LightBackground = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF1F5F9)
private val LightTextPrimary = Color(0xFF0F172A)
private val LightTextSecondary = Color(0xFF475569)
private val LightOutline = Color(0xFFCBD5E1)
private val LightOutlineVariant = Color(0xFFE2E8F0)

// Neutral surfaces (dark)
private val DarkBackground = Color(0xFF020617)
private val DarkSurface = Color(0xFF111827)
private val DarkSurfaceVariant = Color(0xFF1E293B)
private val DarkTextPrimary = Color(0xFFF8FAFC)
private val DarkTextSecondary = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF334155)
private val DarkOutlineVariant = Color(0xFF1E293B)

// Error colors
private val ErrorLight = Color(0xFFD92D20)
private val ErrorDark = Color(0xFFFF6B6B)

private val EasyMoneyLightColorScheme = lightColorScheme(
    primary = SlateNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF020617),
    secondary = VividBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = CyanAccent,
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = VividBlue,
    surfaceTint = SlateNavy,
    scrim = Color(0x66000000)
)

private val EasyMoneyDarkColorScheme = darkColorScheme(
    primary = VividBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CyanAccent,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = SlateNavy,
    onTertiary = Color(0xFFF8FAFC),
    tertiaryContainer = Color(0xFF1E293B),
    onTertiaryContainer = Color(0xFFE2E8F0),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorDark,
    onError = Color(0xFF410002),
    errorContainer = Color(0xFF5F1A18),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = VividBlue,
    surfaceTint = VividBlue,
    scrim = Color(0x99000000)
)

@Immutable
data class TradingExtraColors(
    val gain: Color,
    val loss: Color,
    val neutral: Color,
    val bullishGlow: Color,
    val bearishGlow: Color,
    val warning: Color
)

private val LightTradingExtraColors = TradingExtraColors(
    gain = GainGreen,
    loss = LossRed,
    neutral = VividBlue,
    bullishGlow = GainGreen.copy(alpha = 0.12f),
    bearishGlow = LossRed.copy(alpha = 0.12f),
    warning = WarningAmber
)

private val DarkTradingExtraColors = TradingExtraColors(
    gain = GainGreen,
    loss = LossRed,
    neutral = VividBlue,
    bullishGlow = GainGreen.copy(alpha = 0.18f),
    bearishGlow = LossRed.copy(alpha = 0.18f),
    warning = WarningAmber
)

val LocalTradingExtraColors = staticCompositionLocalOf { LightTradingExtraColors }

val EasyMoneyTradingColors: TradingExtraColors
    @Composable
    get() = LocalTradingExtraColors.current

@Composable
fun EasyMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EasyMoneyDarkColorScheme
        else -> EasyMoneyLightColorScheme
    }

    val extras = if (darkTheme) DarkTradingExtraColors else LightTradingExtraColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalTradingExtraColors provides extras
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = EasyMoneyShapes,
            content = content
        )
    }
}
