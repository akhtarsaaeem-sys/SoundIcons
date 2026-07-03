package com.soundicons.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Predefined icon color palette shown in the editor color picker.
 */
object IconColors {

    val palette = listOf(
        Color(0xFF6200EE), // Purple
        Color(0xFF03DAC6), // Teal
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Cyan
        Color(0xFF45B7D1), // Sky Blue
        Color(0xFF96CEB4), // Sage Green
        Color(0xFFFFEAA7), // Soft Yellow
        Color(0xFFDDA0DD), // Plum
        Color(0xFF98D8C8), // Mint
        Color(0xFFFF9A9E), // Blush
        Color(0xFFA8E6CF), // Light Green
        Color(0xFFFFD93D), // Warm Yellow
        Color(0xFF6C5CE7), // Indigo
        Color(0xFFFD79A8), // Pink
        Color(0xFF00B894), // Emerald
        Color(0xFF0984E3), // Blue
        Color(0xFFE17055), // Coral
        Color(0xFF636E72), // Slate
        Color(0xFF2D3436), // Dark
        Color(0xFFB2BEC3), // Light Grey
    )

    val default: Int get() = palette.first().toArgb()

    /** Returns a color from the palette cycling by index */
    fun colorForIndex(index: Int): Color = palette[index % palette.size]
}
