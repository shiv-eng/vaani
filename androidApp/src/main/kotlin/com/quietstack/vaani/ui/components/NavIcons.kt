package com.quietstack.vaani.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The design's one icon with brand character (per the handoff's asset note:
 * "keep the custom library glyph — five vertical bars of varying height — as
 * a vector; substitute Material Symbols for the rest"). Redrawn from its
 * inline SVG (`M4 6v12M8.5 9v6M13 4.5v15M17.5 8v8M21 10.5v3`, 24x24 viewBox).
 */
@Composable
fun LibraryBarsIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeWidth = 1.9f * s
        val bars = listOf(4f to (6f to 18f), 8.5f to (9f to 15f), 13f to (4.5f to 19.5f), 17.5f to (8f to 16f), 21f to (10.5f to 13.5f))
        for ((x, range) in bars) {
            drawLine(tint, Offset(x * s, range.first * s), Offset(x * s, range.second * s), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
    }
}
