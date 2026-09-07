package com.quietstack.vaani.ui.theme

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
data class VaaniColors(
    val bg: Color,
    val surf: Color,
    val surf2: Color,
    val surf3: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val pri: Color,
    val pri2: Color,
    val priDim: Color,
    val priOn: Color,
    val acc: Color,
    val ok: Color,
)

private val DarkVaaniColors = VaaniColors(
    bg = VaaniDark.Bg, surf = VaaniDark.Surf, surf2 = VaaniDark.Surf2, surf3 = VaaniDark.Surf3,
    line = VaaniDark.Line, ink = VaaniDark.Ink, ink2 = VaaniDark.Ink2, ink3 = VaaniDark.Ink3,
    pri = VaaniDark.Pri, pri2 = VaaniDark.Pri2, priDim = VaaniDark.PriDim, priOn = VaaniDark.PriOn,
    acc = VaaniDark.Acc, ok = VaaniDark.Ok,
)

private val LightVaaniColors = VaaniColors(
    bg = VaaniLight.Bg, surf = VaaniLight.Surf, surf2 = VaaniLight.Surf2, surf3 = VaaniLight.Surf3,
    line = VaaniLight.Line, ink = VaaniLight.Ink, ink2 = VaaniLight.Ink2, ink3 = VaaniLight.Ink3,
    pri = VaaniLight.Pri, pri2 = VaaniLight.Pri2, priDim = VaaniLight.PriDim, priOn = VaaniLight.PriOn,
    acc = VaaniLight.Acc, ok = VaaniLight.Ok,
)

private val LocalVaaniColors = staticCompositionLocalOf { DarkVaaniColors }

/** Access as `VaaniTheme.colors.pri` etc. inside any `@Composable`. */
object VaaniTheme {
    val colors: VaaniColors
        @Composable get() = LocalVaaniColors.current
}

@Composable
fun VaaniTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkVaaniColors else LightVaaniColors

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

    CompositionLocalProvider(LocalVaaniColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = VaaniTypography,
            content = content,
        )
    }
}
