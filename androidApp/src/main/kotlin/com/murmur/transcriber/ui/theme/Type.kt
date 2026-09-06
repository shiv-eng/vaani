package com.murmur.transcriber.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.murmur.transcriber.R

// Single variable-font files, one weight instance per FontWeight used in the
// design (300/400/600/700 for Sora, 400/500/600/700 for Manrope, 400/500 for
// JetBrains Mono) — avoids bundling a static file per weight.

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, weights: List<FontWeight>) = FontFamily(
    weights.map { w ->
        Font(resId, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)

val SoraFamily = variableFamily(
    R.font.sora,
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold),
)

val ManropeFamily = variableFamily(
    R.font.manrope,
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
)

val JetBrainsMonoFamily = variableFamily(
    R.font.jetbrains_mono,
    listOf(FontWeight.Normal, FontWeight.Medium),
)

// Sora for display/headings (the design's "Sora, sans-serif" weight-600/300
// treatments), Manrope for body/labels (the design's default body font).
val MurmurTypography = Typography(
    headlineSmall = TextStyle(fontFamily = SoraFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = SoraFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = SoraFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = SoraFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 14.5.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.6.sp),
)
