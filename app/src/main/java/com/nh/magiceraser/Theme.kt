package com.nh.magiceraser

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object NH {
    // True AMOLED Pitch Black & Obsidian Surfaces
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF090608)
    val Surface2 = Color(0xFF13090D)
    val Surface3 = Color(0xFF1D0E14)

    // Flagship Crimson & Neon Red Palette
    val Red = Color(0xFFFF1E38)
    val Scarlet = Color(0xFFFF4D6D)
    val DarkRed = Color(0xFF8F0B1D)
    val Ember = Color(0xFFFF6B35)
    val RoseGold = Color(0xFFFFB3C1)

    // Semantic aliases kept for compatibility
    val Violet = Red
    val Cyan = Scarlet
    val Pink = Ember
    val Amber = Ember
    val Green = Color(0xFF10B981)

    // High-contrast AMOLED Typography
    val TextHi = Color(0xFFFFFFFF)
    val TextMid = Color(0xFFE2D4D8)
    val TextLo = Color(0xFF8E7A80)

    // Signature Red & Black Gradients
    val Accent: Brush get() = Brush.horizontalGradient(listOf(Color(0xFFFF1E38), Color(0xFFC80D26), Color(0xFFFF4D6D)))
    val Cool: Brush get() = Brush.horizontalGradient(listOf(Color(0xFFFF4D6D), Color(0xFFFF1E38), Color(0xFF990F24)))
    val CrimsonVertical: Brush get() = Brush.verticalGradient(listOf(Color(0xFFFF2A44), Color(0xFF9E0B20)))
    val ObsidianCard: Brush get() = Brush.linearGradient(listOf(Color(0xFF180A0E), Color(0xFF080506)))
}

@Composable
fun NHTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = NH.Red,
        onPrimary = Color.White,
        secondary = NH.Scarlet,
        tertiary = NH.Ember,
        background = NH.Bg,
        surface = NH.Surface,
        onSurface = NH.TextHi,
        onBackground = NH.TextHi
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides NH.TextHi) { content() }
    }
}
