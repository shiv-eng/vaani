package com.murmur.transcriber.ui.importing

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmur.transcriber.importing.ImportedFileInfo
import com.murmur.transcriber.ui.format.formatBytes
import com.murmur.transcriber.ui.format.formatDurationShort
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.MurmurTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

@Composable
fun ImportScreen(
    uri: Uri,
    onBack: () -> Unit,
    onFinished: (recordingId: String) -> Unit,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MurmurTheme.colors

    LaunchedEffect(uri) { viewModel.stage(uri) }
    LaunchedEffect(state.savedRecordingId) { state.savedRecordingId?.let(onFinished) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 20.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.surf, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.ink2, modifier = Modifier.size(16.dp))
            }
            Text(
                "SHARED FROM FILES",
                color = colors.ink3,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("Add this to the library?", style = MaterialTheme.typography.headlineSmall, color = colors.ink)

        when {
            state.error != null -> {
                Spacer(Modifier.height(20.dp))
                Text(state.error.orEmpty(), color = colors.acc, style = MaterialTheme.typography.bodyMedium)
            }
            state.isLoading || state.info == null -> {
                Spacer(Modifier.height(20.dp))
                Text("Reading file…", color = colors.ink3, style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                val info = state.info!!
                Spacer(Modifier.height(20.dp))
                FileCard(info)

                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier
                        .background(colors.surf2, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).background(colors.pri, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↓", color = colors.priOn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "The file is copied into app-private storage, so it keeps working if the folder permission is later revoked.",
                        color = colors.ink2,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                    )
                }

                Spacer(Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(colors.pri, RoundedCornerShape(18.dp))
                            .clickable(onClick = viewModel::commit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Import and transcribe", color = colors.priOn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(onClick = viewModel::commit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Import, transcribe later", color = colors.ink2, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(info: ImportedFileInfo) {
    val colors = MurmurTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(46.dp).background(colors.priDim, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(info.extension, color = colors.pri, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            }
            Column {
                Text(info.displayFileName, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text("read from the system picker", color = colors.ink3, fontSize = 11.5.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        ImportWaveform()

        Spacer(Modifier.height(20.dp))
        val estimateMs = (info.durationMs * 0.46).toLong()
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCell("DURATION", formatDurationShort(info.durationMs), Modifier.weight(1f))
            InfoCell("SIZE", formatBytes(info.sizeBytes), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCell("FORMAT", "${info.mimeType.substringAfter('/')} · ${info.sampleRate / 1000}kHz", Modifier.weight(1f))
            InfoCell("ESTIMATE", "~${(estimateMs / 60_000).coerceAtLeast(1)} min", Modifier.weight(1f))
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(modifier = modifier) {
        Text(label, color = colors.ink3, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.9.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, color = colors.ink, fontFamily = JetBrainsMonoFamily, fontSize = 15.sp)
    }
}

@Composable
private fun ImportWaveform() {
    val colors = MurmurTheme.colors
    // Placeholder bars — real per-file peaks land with playback (Milestone 4);
    // waveforms must be computed from audio and cached, never derived at render time.
    val heights = remember { List(50) { 0.2f + Random.nextFloat() * 0.7f } }
    Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { i, h ->
            val x = i * (barWidth + gap) + barWidth / 2
            drawLine(
                color = colors.pri.copy(alpha = 0.7f),
                start = Offset(x, size.height * (1 - h)),
                end = Offset(x, size.height),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
