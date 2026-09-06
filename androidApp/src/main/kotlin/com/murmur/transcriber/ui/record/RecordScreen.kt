package com.murmur.transcriber.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmur.transcriber.ui.components.mrise
import com.murmur.transcriber.ui.components.rememberBlinkAlpha
import com.murmur.transcriber.ui.components.rememberBreatheScale
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.MurmurTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onFinished: (recordingId: String) -> Unit,
    viewModel: RecordViewModel = koinViewModel(),
) {
    val state by viewModel.recorderState.collectAsState()
    val savedId by viewModel.savedRecordingId.collectAsState()
    val colors = MurmurTheme.colors

    LaunchedEffect(savedId) { savedId?.let(onFinished) }

    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.start()
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val already = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        hasPermission = already
        if (already) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        // "520px radial glow bleeding off the bottom" (SCREENS.md #04) — sized
        // and offset explicitly rather than centered on the whole screen.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 140.dp)
                .size(520.dp, 420.dp)
                .background(
                    Brush.radialGradient(listOf(colors.priDim.copy(alpha = 0.55f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp).mrise()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surf, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.ink2, modifier = Modifier.size(16.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(colors.surf, RoundedCornerShape(999.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Text("Hindi + English", color = colors.ink2, fontSize = 11.5.sp)
                    Text("▾", color = colors.pri, fontSize = 11.5.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val blink = rememberBlinkAlpha(periodMillis = 1100)
                val statusColor = if (state.isPaused) colors.ink3 else colors.acc
                val statusAlpha = if (state.isPaused) 1f else blink
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(statusColor.copy(alpha = statusAlpha), CircleShape))
                    Text(
                        if (state.isPaused) "PAUSED" else "RECORDING",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    formatTimer(state.elapsedMs),
                    color = colors.ink,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 52.sp,
                    letterSpacing = (-1.5).sp,
                )
                Text(
                    "writing to disk continuously · ${formatMb(state.fileSizeBytes)}",
                    color = colors.ink3,
                    fontSize = 11.5.sp,
                )
            }

            LevelMeter(amplitude = state.amplitude, paused = state.isPaused)
            Spacer(Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(colors.surf, RoundedCornerShape(22.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                        .clickable(onClick = viewModel::togglePause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (state.isPaused) "Resume" else "Pause",
                        tint = colors.ink,
                    )
                }

                val breathe = rememberBreatheScale()
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(breathe)
                        .background(colors.acc, RoundedCornerShape(32.dp))
                        .clickable(onClick = viewModel::stop),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(Color.White, RoundedCornerShape(6.dp)),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(colors.surf, RoundedCornerShape(22.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("44.1", color = colors.ink, fontFamily = JetBrainsMonoFamily, fontSize = 13.sp)
                        Text("KHZ", color = colors.ink3, fontSize = 9.sp, letterSpacing = 0.7.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelMeter(amplitude: Int, paused: Boolean) {
    val colors = MurmurTheme.colors
    val heights = remember { mutableStateListOf<Float>().apply { repeat(42) { add(0.2f) } } }

    LaunchedEffect(amplitude, paused) {
        if (!paused) {
            val normalized = (amplitude / 32767f).coerceIn(0f, 1f)
            heights.removeAt(0)
            heights.add(0.16f + 0.84f * normalized)
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        val barWidth = 3.5.dp.toPx()
        val gap = 2.5.dp.toPx()
        val totalWidth = heights.size * (barWidth + gap) - gap
        val startX = (size.width - totalWidth) / 2f
        heights.forEachIndexed { i, h ->
            val x = startX + i * (barWidth + gap)
            val barHeight = size.height * h
            val color = if (i in 17..24) colors.acc else colors.pri
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatMb(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)
