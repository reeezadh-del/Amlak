package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PersianTeal,
    onPrimary = Color.White,
    primaryContainer = PersianTealLight,
    onPrimaryContainer = PersianTealDark,
    secondary = WarmTerracotta,
    onSecondary = Color.White,
    secondaryContainer = WarmTerracottaLight,
    onSecondaryContainer = Color(0xFF5D210B),
    tertiary = SageGreen,
    onTertiary = Color.White,
    tertiaryContainer = SageGreenLight,
    onTertiaryContainer = Color(0xFF103824),
    background = WarmIvory,
    onBackground = DeepCharcoal,
    surface = WarmSurface,
    onSurface = DeepCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = StoneBorder,
    outlineVariant = Color(0xFFECE5DC),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedLight,
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = Color(0xFF00252E),
    primaryContainer = PersianTealDark,
    onPrimaryContainer = PersianTealLight,
    secondary = DarkTerracotta,
    onSecondary = Color(0xFF471605),
    secondaryContainer = Color(0xFF6B2A14),
    onSecondaryContainer = WarmTerracottaLight,
    tertiary = Color(0xFF74C69D),
    onTertiary = Color(0xFF0B311F),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = Color(0xFF334348),
    outlineVariant = Color(0xFF263337),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BiVaseteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
