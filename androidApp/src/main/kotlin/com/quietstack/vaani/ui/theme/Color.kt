package com.quietstack.vaani.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens from the approved screen design (Vaani App.dc.html).
// Names match the design's CSS custom properties 1:1 so the two stay easy to
// cross-reference — --bg -> Bg, --pri -> Pri, etc.

object VaaniDark {
    val Bg = Color(0xFF0B0A10)
    val Surf = Color(0xFF141220)
    val Surf2 = Color(0xFF1D1A2E)
    val Surf3 = Color(0xFF26223C)
    val Line = Color(0x1AFFFFFF) // rgba(255,255,255,.10)
    val Ink = Color(0xFFF4F1FF)
    val Ink2 = Color(0xFFA29CC0)
    val Ink3 = Color(0xFF6F6890)
    val Pri = Color(0xFFA79BFF)
    val Pri2 = Color(0xFF7EC4FF)
    val PriDim = Color(0xFF3A2F78)
    val PriOn = Color(0xFF1B0F6B)
    val Acc = Color(0xFFFF8A66)
    val Ok = Color(0xFF7FE0B0)
}

object VaaniLight {
    val Bg = Color(0xFFF6F4FF)
    val Surf = Color(0xFFFFFFFF)
    val Surf2 = Color(0xFFEEEAFF)
    val Surf3 = Color(0xFFE2DCFF)
    val Line = Color(0x1A14102D) // rgba(20,16,45,.10)
    val Ink = Color(0xFF151129)
    val Ink2 = Color(0xFF5D5780)
    val Ink3 = Color(0xFF8A84A8)
    val Pri = Color(0xFF5B4BFF)
    val Pri2 = Color(0xFF2C7BE5)
    val PriDim = Color(0xFFD9D2FF)
    val PriOn = Color(0xFFFFFFFF)
    val Acc = Color(0xFFD9502A)
    val Ok = Color(0xFF1E9E6A)
}
