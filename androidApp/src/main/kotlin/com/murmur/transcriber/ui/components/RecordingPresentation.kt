package com.murmur.transcriber.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.ui.theme.MurmurTheme
import kotlin.random.Random

/**
 * The design's four badge states (SCREENS.md #03/#06) — shared by Home and
 * Library so the two screens never drift into inconsistent labelling.
 */
enum class ItemKind { DONE, RUNNING, IMPORTED_RAW, RAW }

fun Recording.kind(): ItemKind = when {
    transcriptionState == TranscriptionState.COMPLETE -> ItemKind.DONE
    transcriptionState == TranscriptionState.RUNNING || transcriptionState == TranscriptionState.QUEUED -> ItemKind.RUNNING
    source == RecordingSource.IMPORTED -> ItemKind.IMPORTED_RAW
    else -> ItemKind.RAW
}

@Composable
fun KindBadge(kind: ItemKind, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    val (label, fg, bg) = when (kind) {
        ItemKind.DONE -> Triple("TEXT", colors.priOn, colors.pri)
        ItemKind.RUNNING -> Triple("RUNNING", colors.bg, colors.acc)
        ItemKind.IMPORTED_RAW -> Triple("IMPORTED", colors.ink2, colors.surf3)
        ItemKind.RAW -> Triple("RAW", colors.ink3, Color.Transparent)
    }
    Box(
        modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.6.sp)
    }
}

/**
 * A seeded placeholder waveform — real per-file peaks land with playback
 * (Milestone 4); they must be computed from audio and cached, never derived
 * at render time. [seed] should be stable per recording (e.g. its id hash)
 * so a row doesn't visually reshuffle on recomposition.
 */
@Composable
fun Waveform(seed: Int, barCount: Int, modifier: Modifier = Modifier, alpha: Float = 0.6f, minHeight: Float = 0.15f) {
    val colors = MurmurTheme.colors
    val heights = remember(seed, barCount) {
        val r = Random(seed)
        List(barCount) { minHeight + r.nextFloat() * (1f - minHeight) }
    }
    // Callers size this fully (fillMaxWidth+height, or a fixed size like
    // MiniWaveform) — no size opinions imposed here.
    Canvas(modifier = modifier) {
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { i, h ->
            val x = i * (barWidth + gap) + barWidth / 2
            drawLine(
                color = colors.pri.copy(alpha = alpha),
                start = Offset(x, size.height * (1 - h)),
                end = Offset(x, size.height),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The 34x26dp mini thumbnail used in Home's Recent rows. */
@Composable
fun MiniWaveform(seed: Int, modifier: Modifier = Modifier, width: Dp = 34.dp, height: Dp = 26.dp) {
    Waveform(seed = seed, barCount = 9, modifier = modifier.size(width, height), alpha = 0.85f, minHeight = 0.25f)
}
