package com.murmur.transcriber.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.MurmurTheme

enum class EdgeCaseTone { INFO, WARNING }

/**
 * The shared modal anatomy for every failure/edge-case dialog (SCREENS-2-EDGE-CASES.md
 * "Dialog anatomy"): glyph tile, title, body, an optional mono detail block, and
 * right-aligned actions — one composable so each edge case can't drift visually.
 */
@Composable
fun EdgeCaseDialog(
    glyph: String,
    title: String,
    body: String,
    tone: EdgeCaseTone,
    onDismiss: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    detail: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryIsDestructive: Boolean = false,
) {
    val colors = MurmurTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .background(colors.surf, RoundedCornerShape(30.dp))
                .padding(26.dp),
        ) {
            val tileBg = if (tone == EdgeCaseTone.INFO) colors.priDim else colors.surf3
            val glyphColor = if (tone == EdgeCaseTone.INFO) colors.pri else colors.acc
            Box(
                modifier = Modifier.size(48.dp).background(tileBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = glyphColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Text(title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(10.dp))
            Text(body, color = colors.ink2, fontSize = 13.5.sp, lineHeight = 21.6.sp)

            if (detail != null) {
                Spacer(Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().background(colors.bg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(detail, color = colors.ink3, fontFamily = JetBrainsMonoFamily, fontSize = 11.5.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (secondaryLabel != null && onSecondary != null) {
                    Box(
                        modifier = Modifier.height(44.dp).clickable(onClick = onSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(secondaryLabel, color = colors.ink2, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                val primaryBg = if (primaryIsDestructive) colors.acc else colors.pri
                val primaryFg = if (primaryIsDestructive) colors.bg else colors.priOn
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .background(primaryBg, RoundedCornerShape(14.dp))
                        .clickable(onClick = onPrimary)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryLabel, color = primaryFg, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}
