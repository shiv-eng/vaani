package com.quietstack.vaani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.quietstack.vaani.ui.theme.VaaniTheme
import com.quietstack.vaani.ui.theme.SoraFamily

/**
 * The three-bar mark (three distinct theme colors) that also backs the
 * launcher icon (ic_launcher_foreground.xml) — the app's one visual
 * identity, reused across the icon, the splash screen and the in-app
 * wordmark rather than three different marks. Animates via `mwave`
 * (Motion.kt) for as long as it's composed, so on its own it doubles as a
 * loading indicator; the launcher icon shows it frozen mid-wave instead
 * since a static icon can't animate.
 */
@Composable
fun WordmarkBars(barWidth: Dp = 3.dp, maxHeight: Dp = 16.dp, gap: Dp = 2.dp) {
    val colors = VaaniTheme.colors
    val barColors = listOf(colors.pri, colors.pri2, colors.acc)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
        val heights = listOf(0.6f, 1f, 0.75f)
        val delays = listOf(0, 180, 360)
        heights.forEachIndexed { i, baseHeight ->
            val scale = rememberWaveScale(delayMillis = delays[i])
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * baseHeight * (0.6f + 0.4f * scale))
                    .background(barColors[i], RoundedCornerShape(barWidth / 2)),
            )
        }
    }
}

/** [WordmarkBars] plus the "VAANI" text, the lockup used on Onboarding, Home and the splash screen. */
@Composable
fun WordmarkLockup(
    barWidth: Dp = 3.dp,
    barMaxHeight: Dp = 16.dp,
    fontSize: TextUnit = 15.sp,
    letterSpacing: TextUnit = 2.1.sp,
) {
    val colors = VaaniTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WordmarkBars(barWidth = barWidth, maxHeight = barMaxHeight)
        Spacer(Modifier.width(4.dp))
        Text(
            "VAANI",
            color = colors.ink,
            fontFamily = SoraFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
        )
    }
}
