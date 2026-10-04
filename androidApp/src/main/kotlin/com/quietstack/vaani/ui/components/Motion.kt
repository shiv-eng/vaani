package com.quietstack.vaani.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The design's shared easing — "fast-out, settle-in" (README "Motion"). */
val VaaniEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

/**
 * `mrise` (screen enter, README "Motion"): opacity 0→1, translateY 14–16dp→0,
 * scale .985→1, 400–500ms. Apply once to a screen's root content.
 */
fun Modifier.mrise(durationMillis: Int = 450, translateY: Dp = 15.dp): Modifier = composed {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = VaaniEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = with(density) { translateY.toPx() } * (1f - progress.value)
        val scale = 0.985f + 0.015f * progress.value
        scaleX = scale
        scaleY = scale
    }
}

/**
 * `mslide` (list/card stagger, README "Motion"): opacity 0→1, translateY
 * 22dp→0, 400–550ms, with a per-item stagger delay.
 *
 * [animate] must be false for a row that has already played this once —
 * LazyColumn disposes and recomposes item slots that scroll far enough out
 * of view, which re-runs `remember { Animatable(0f) }` and `LaunchedEffect`
 * from scratch. Left at its old always-true default, every row silently
 * replayed its fade/slide-in each time it scrolled back into view, which is
 * exactly what "janky scrolling" looks like — see rememberAnimateOnce, which
 * callers use to compute this per item key.
 */
fun Modifier.mslide(delayMillis: Int, durationMillis: Int = 480, translateY: Dp = 22.dp, animate: Boolean = true): Modifier = composed {
    val density = LocalDensity.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) {
            delay(delayMillis.toLong())
            progress.animateTo(1f, tween(durationMillis, easing = VaaniEasing))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = with(density) { translateY.toPx() } * (1f - progress.value)
    }
}

/**
 * Returns true the first time it's called for a given [key] during this
 * screen's lifetime, false every time after — including after the caller's
 * own composition was disposed and recreated (a LazyColumn row scrolled out
 * then back). [seen] must be hoisted above the list (e.g. the screen's
 * top-level `remember`), not created inside the item lambda, or it gets
 * disposed along with the row and defeats the whole point.
 */
@Composable
fun rememberAnimateOnce(key: Any, seen: MutableSet<Any>): Boolean = remember(key) { seen.add(key) }

/**
 * A plain ticking second-count while [active] is true, reset to 0 each time
 * it becomes active. Used wherever there's a real duration to show but no
 * real percentage (a model load — see TranscribingCard and ActiveJobCard,
 * which both had their own copy of this exact loop before being extracted
 * here).
 */
@Composable
fun rememberElapsedSeconds(active: Boolean): Int {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(active) {
        if (active) {
            seconds = 0
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }
    return seconds
}

/** `mblink` (status dot / caret, README "Motion"): opacity 1→.15, period given in ms. */
@Composable
fun rememberBlinkAlpha(periodMillis: Int = 2400): Float {
    val transition = rememberInfiniteTransition(label = "blink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(periodMillis / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )
    return alpha
}

/** `mspin` (job spinner, README "Motion"): 900ms linear, 360 degrees, infinite. */
@Composable
fun rememberSpinDegrees(): Float {
    val transition = rememberInfiniteTransition(label = "spin")
    val degrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinDegrees",
    )
    return degrees
}

/** `mwave` (wordmark / live bars, README "Motion"): a bar's height scale, staggered by [delayMillis]. */
@Composable
fun rememberWaveScale(delayMillis: Int = 0, periodMillis: Int = 900): Float {
    val transition = rememberInfiniteTransition(label = "wave")
    val scale by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(periodMillis, delayMillis = delayMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveScale",
    )
    return scale
}

/** `mbreathe` (stop button idle, README "Motion"): scale 1→1.05, 3s ease-in-out, infinite. */
@Composable
fun rememberBreatheScale(): Float {
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )
    return scale
}
