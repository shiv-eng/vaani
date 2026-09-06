package com.murmur.transcriber.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design's token set is richer than Material3's ColorScheme slots (three
 * surface tiers, three ink tiers, a distinct "ok" accent) — exposed directly
 * via CompositionLocal rather than force-fit into ColorScheme's fixed names.
 */
data class MurmurColors(
    val bg: Color,
    val surf: Color,
    val surf2: Color,
    val surf3: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val pri: Color,
    val priDim: Color,
    val priOn: Color,
    val acc: Color,
    val ok: Color,
)

private val DarkMurmurColors = MurmurColors(
    bg = MurmurDark.Bg, surf = MurmurDark.Surf, surf2 = MurmurDark.Surf2, surf3 = MurmurDark.Surf3,
    line = MurmurDark.Line, ink = MurmurDark.Ink, ink2 = MurmurDark.Ink2, ink3 = MurmurDark.Ink3,
    pri = MurmurDark.Pri, priDim = MurmurDark.PriDim, priOn = MurmurDark.PriOn,
    acc = MurmurDark.Acc, ok = MurmurDark.Ok,
)

private val LightMurmurColors = MurmurColors(
    bg = MurmurLight.Bg, surf = MurmurLight.Surf, surf2 = MurmurLight.Surf2, surf3 = MurmurLight.Surf3,
    line = MurmurLight.Line, ink = MurmurLight.Ink, ink2 = MurmurLight.Ink2, ink3 = MurmurLight.Ink3,
    pri = MurmurLight.Pri, priDim = MurmurLight.PriDim, priOn = MurmurLight.PriOn,
    acc = MurmurLight.Acc, ok = MurmurLight.Ok,
)

private val LocalMurmurColors = staticCompositionLocalOf { DarkMurmurColors }

/** Access as `MurmurTheme.colors.pri` etc. inside any `@Composable`. */
object MurmurTheme {
    val colors: MurmurColors
        @Composable get() = LocalMurmurColors.current
}

@Composable
fun MurmurTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkMurmurColors else LightMurmurColors

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            background = colors.bg, surface = colors.surf, primary = colors.pri,
            onPrimary = colors.priOn, onBackground = colors.ink, onSurface = colors.ink,
            secondaryContainer = colors.surf2, error = colors.acc,
        )
    } else {
        lightColorScheme(
            background = colors.bg, surface = colors.surf, primary = colors.pri,
            onPrimary = colors.priOn, onBackground = colors.ink, onSurface = colors.ink,
            secondaryContainer = colors.surf2, error = colors.acc,
        )
    }

    CompositionLocalProvider(LocalMurmurColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MurmurTypography,
            content = content,
        )
    }
}
