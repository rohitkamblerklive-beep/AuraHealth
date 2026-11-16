package com.pro.aifitnesstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightCleanScheme = lightColorScheme(
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    primary = LightPrimary,
    secondary = LightPrimary,
    tertiary = LightPrimary,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFFCBD5E1)
)

val SleepOptimizerScheme = darkColorScheme(
    background = SleepBg,
    surface = SleepSurface,
    surfaceVariant = SleepSurfaceVariant,
    primary = SleepPrimary,
    secondary = SleepPrimary,
    tertiary = SleepPrimary,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onTertiary = Color(0xFF0F172A),
    onSurfaceVariant = SleepOnSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = SleepSurfaceVariant
)

val CyberpunkNeonScheme = darkColorScheme(
    background = CyberBg,
    surface = CyberSurface,
    surfaceVariant = CyberSurfaceVariant,
    primary = CyberPrimary,
    secondary = CyberPrimary,
    tertiary = CyberPrimary,
    onPrimary = Color(0xFF0A0514),
    onSecondary = Color(0xFF0A0514),
    onTertiary = Color(0xFF0A0514),
    onSurfaceVariant = CyberOnSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = CyberSurfaceVariant
)

val GoldEliteScheme = darkColorScheme(
    background = GoldBg,
    surface = GoldSurface,
    surfaceVariant = GoldSurfaceVariant,
    primary = GoldPrimary,
    secondary = GoldPrimary,
    tertiary = GoldPrimary,
    onPrimary = Color(0xFF0F0F0F),
    onSecondary = Color(0xFF0F0F0F),
    onTertiary = Color(0xFF0F0F0F),
    onSurfaceVariant = GoldOnSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = GoldSurfaceVariant
)

val MidnightDarkScheme = darkColorScheme(
    background = MidnightBg,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    primary = MidnightPrimary,
    secondary = MidnightPrimary,
    tertiary = MidnightPrimary,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = MidnightSurfaceVariant
)

@Composable
fun AIFitnessTrackerTheme(
    themeName: String = "SleepOptimizer",
    fontFamilyName: String = "Default",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "LightClean", "LightMode", "Light" -> LightCleanScheme
        "CyberpunkNeon" -> CyberpunkNeonScheme
        "GoldElite" -> GoldEliteScheme
        "MidnightDark" -> MidnightDarkScheme
        else -> SleepOptimizerScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamilyName),
        content = content
    )
}