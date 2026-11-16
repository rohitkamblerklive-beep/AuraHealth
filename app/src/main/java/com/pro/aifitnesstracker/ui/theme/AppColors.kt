package com.pro.aifitnesstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dynamic Global App Color Palette — automatically adapts to Light Mode & Dark Mode.
 */
object AppColors {
    val Background: Color @Composable get() = MaterialTheme.colorScheme.background
    val Card: Color @Composable get() = MaterialTheme.colorScheme.surface
    val CardSecondary: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
    val AccentAlt: Color @Composable get() = MaterialTheme.colorScheme.primary
    val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val Divider: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val IconBg: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val DeleteRed: Color = Color(0x99FF1744)
    val TopBar: Color @Composable get() = MaterialTheme.colorScheme.background
    val BottomBar: Color @Composable get() = MaterialTheme.colorScheme.surface
}
