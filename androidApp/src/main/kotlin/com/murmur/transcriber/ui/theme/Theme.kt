package com.murmur.transcriber.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFA3560C),
    background = Color(0xFFF6F6F8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3A260),
    background = Color(0xFF0F1116),
)

@Composable
fun MurmurTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
