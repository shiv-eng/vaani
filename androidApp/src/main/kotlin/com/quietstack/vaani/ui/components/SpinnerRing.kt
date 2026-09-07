package com.quietstack.vaani.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** `mspin` (README "Motion"): a ring with a transparent gap, spinning 360°/900ms linear. */
@Composable
fun SpinnerRing(color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp, strokeWidth: Dp = 2.dp) {
    val degrees = rememberSpinDegrees()
    Canvas(modifier = modifier.size(size)) {
        rotate(degrees) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(this.size.width, this.size.height),
            )
        }
    }
}
