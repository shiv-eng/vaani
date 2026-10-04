package com.quietstack.vaani.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.ui.components.KindBadge
import com.quietstack.vaani.ui.components.MiniWaveform
import com.quietstack.vaani.ui.components.SpinnerRing
import com.quietstack.vaani.ui.components.kind
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.components.rememberAnimateOnce
import com.quietstack.vaani.ui.components.rememberBlinkAlpha
import com.quietstack.vaani.ui.components.rememberElapsedSeconds
import com.quietstack.vaani.ui.components.WordmarkBars
import com.quietstack.vaani.shared.domain.usecase.LOADING_MODEL_SENTINEL
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.format.formatBytesParts
import com.quietstack.vaani.ui.format.formatDurationShort
import com.quietstack.vaani.ui.format.formatHoursCompact
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onOpenRecording: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = VaaniTheme.colors
    val animatedRows = remember { mutableSetOf<Any>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 20.dp).mrise(),
        contentPadding = PaddingValues(top = 22.dp, bottom = 128.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WordmarkBars(barWidth = 2.5.dp, maxHeight = 13.dp, gap = 1.5.dp)
                    Text("Vaani", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                }
                OfflineReadyPill()
            }
            Spacer(Modifier.height(18.dp))

            val (hours, minutes) = formatHoursCompact(state.totalDurationMs)
            val (sizeValue, sizeUnit) = formatBytesParts(state.totalSizeBytes)
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                StatCard(statValue(state.recordingsCount.toString()), "recordings", Modifier.weight(1f).fillMaxHeight().mslide(40))
                StatCard(statValue(hours, suffix = "h ", trailing = minutes), "captured", Modifier.weight(1f).fillMaxHeight().mslide(100))
                StatCard(statValue(sizeValue, suffix = sizeUnit), "on device", Modifier.weight(1f).fillMaxHeight().mslide(160))
            }

            state.activeJob?.let { job ->
                Spacer(Modifier.height(14.dp))
                ActiveJobCard(
                    recording = job,
                    progress = state.activeJobProgress,
                    onClick = { onOpenRecording(job.id) },
                    modifier = Modifier.mslide(200),
                )
            }

            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Recent", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                Text(
                    "Library",
                    color = colors.pri,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable(onClick = onOpenLibrary),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.recent.isEmpty()) {
            item { EmptyRecentCard(modifier = Modifier.mrise()) }
        } else {
            itemsIndexed(state.recent, key = { _, item -> item.id }) { index, item ->
                val animate = rememberAnimateOnce(item.id, animatedRows)
                RecentRow(item, onClick = { onOpenRecording(item.id) }, modifier = Modifier.mslide(index * 60, animate = animate))
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

/** Shown the first time there's nothing recorded yet — an empty list with no explanation reads as broken, not "start here." */
@Composable
private fun EmptyRecentCard(modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf2, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("Nothing recorded yet", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap the record button below to capture your first note — it'll show up right here, transcribed on-device.",
            color = colors.ink2,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun OfflineReadyPill() {
    val colors = VaaniTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .background(colors.surf, RoundedCornerShape(999.dp))
            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        val blinkAlpha = rememberBlinkAlpha()
        Box(Modifier.size(7.dp).background(colors.ok.copy(alpha = blinkAlpha), CircleShape))
        Text("Offline ready", color = colors.ink2, style = MaterialTheme.typography.labelMedium)
    }
}

/** The mono value with its unit rendered smaller and dimmer (SCREENS.md #03: "unit suffixes drop to 13px ink3"). */
@Composable
private fun statValue(main: String, suffix: String? = null, trailing: String? = null): AnnotatedString {
    val colors = VaaniTheme.colors
    return buildAnnotatedString {
        append(main)
        if (suffix != null) {
            withStyle(SpanStyle(fontSize = 13.sp, color = colors.ink3)) { append(suffix) }
        }
        trailing?.let { append(it) }
    }
}

@Composable
private fun StatCard(value: AnnotatedString, label: String, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Column(
        modifier = modifier
            .background(colors.surf, RoundedCornerShape(19.dp))
            .border(1.dp, colors.line, RoundedCornerShape(19.dp))
            .padding(horizontal = 13.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            value,
            color = colors.ink,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = colors.ink3, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ActiveJobCard(recording: Recording, progress: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    // Same real signal as DetailScreen's TranscribingCard (see there for
    // why inferring this from progress==0 was a real, reported bug) — and
    // whisper.cpp still gives no real sub-progress for the loading phase
    // itself, so an honestly-ticking elapsed counter beats a bar frozen at 0.
    val isLoadingModel = progress == LOADING_MODEL_SENTINEL
    val loadingElapsedSeconds = rememberElapsedSeconds(isLoadingModel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(listOf(colors.priDim, colors.surf)),
                RoundedCornerShape(24.dp),
            )
            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpinnerRing(colors.pri)
                Text(
                    if (isLoadingModel) "LOADING MODEL" else "TRANSCRIBING",
                    color = colors.pri,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                if (isLoadingModel) "${loadingElapsedSeconds}s" else "${(progress * 100).toInt()}%",
                color = colors.ink2,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(recording.displayName, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatDurationShort(recording.durationMs)} · running in the background, can take a few minutes",
            color = colors.ink2,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.surf3, RoundedCornerShape(4.dp)),
        ) {
            if (!isLoadingModel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(colors.pri, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun RecentRow(recording: Recording, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(colors.surf, RoundedCornerShape(20.dp))
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        MiniWaveform(seed = recording.id.hashCode())
        Column(modifier = Modifier.weight(1f)) {
            Text(
                recording.displayName,
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${formatDurationShort(recording.durationMs)} · ${formatBytes(recording.sizeBytes)}",
                color = colors.ink3,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
            )
        }
        KindBadge(recording.kind())
    }
}
