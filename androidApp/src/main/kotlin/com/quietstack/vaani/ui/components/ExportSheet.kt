package com.quietstack.vaani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.format.formatDurationShort
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme

/**
 * The shared "Export" sheet (SCREENS.md #12) — same rows from Detail or
 * Library. Offers the voice recording itself alongside the transcript, not
 * just the transcript: on-device transcription accuracy has real limits
 * (see WhisperLanguages.kt), so a user who doesn't trust one recording's
 * text should still be able to get the actual audio out, not be stuck with
 * an unusable transcript and no fallback.
 */
@Composable
fun ExportSheetContent(
    durationMs: Long,
    plainTextSizeBytes: Long,
    srtSizeBytes: Long,
    audioSizeBytes: Long,
    onExportTxt: () -> Unit,
    onExportSrt: () -> Unit,
    onExportAudio: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = VaaniTheme.colors
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Export", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(10.dp))

        ExportRow(
            glyph = "♪",
            title = "Voice recording",
            meta = "${formatDurationShort(durationMs)} · ${formatBytes(audioSizeBytes)}",
            onClick = onExportAudio,
        )
        ExportRow(
            glyph = "txt",
            title = "Text file",
            meta = "${formatDurationShort(durationMs)} · about ${formatBytes(plainTextSizeBytes)}",
            onClick = onExportTxt,
        )
        ExportRow(
            glyph = "srt",
            title = "Subtitle file",
            meta = "For video editors · about ${formatBytes(srtSizeBytes)}",
            onClick = onExportSrt,
        )
        ExportRow(glyph = "⧉", title = "Copy transcript", meta = "Whole transcript, with timestamps", onClick = onCopy)
        ExportRow(glyph = "↗", title = "Share transcript", meta = "Hand off to any app", onClick = onShare)

        Spacer(Modifier.height(4.dp))
        Text(
            "Uses your edited version where you've changed something, and the original everywhere else.",
            color = colors.ink3,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close", color = colors.ink2) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExportRow(glyph: String, title: String, meta: String, onClick: () -> Unit) {
    val colors = VaaniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(colors.surf2, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = colors.pri, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Column {
            Text(title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(meta, color = colors.ink3, fontSize = 11.5.sp)
        }
    }
}
