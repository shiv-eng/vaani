package com.murmur.transcriber.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.model.TranscriptionJob
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.ui.components.DeleteRecordingDialog
import com.murmur.transcriber.ui.components.EdgeCaseDialog
import com.murmur.transcriber.ui.components.EdgeCaseTone
import com.murmur.transcriber.ui.components.RecordingActionsSheetContent
import com.murmur.transcriber.ui.components.RenameDialog
import com.murmur.transcriber.ui.components.SpinnerRing
import com.murmur.transcriber.ui.components.mrise
import com.murmur.transcriber.ui.components.mslide
import com.murmur.transcriber.ui.format.formatBytes
import com.murmur.transcriber.ui.format.formatDurationShort
import com.murmur.transcriber.ui.format.formatMmSs
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.MurmurTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private val PressSpring = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordingId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(recordingId) }),
) {
    val state by viewModel.state.collectAsState()
    val colors = MurmurTheme.colors
    val recording = state.recording
    var showActions by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).mrise()) {
        DetailHeader(recording = recording, onBack = onBack, onOverflowClick = { showActions = true })

        if (recording == null) return@Column

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
        ) {
            item {
                Text(
                    recording.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatDurationShort(recording.durationMs)} · ${formatBytes(recording.sizeBytes)} · " +
                        "${recording.fileName.substringAfterLast('.', "audio")} · " +
                        if (recording.source == RecordingSource.RECORDED) "recorded" else "imported",
                    color = colors.ink3,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.5.sp,
                )
                Spacer(Modifier.height(22.dp))

                Scrubber(
                    seed = recording.id.hashCode(),
                    playedFraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                    onSeek = viewModel::seekToFraction,
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMmSs(state.positionMs), color = colors.ink3, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                    Text(formatMmSs(state.durationMs), color = colors.ink3, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                }

                Spacer(Modifier.height(22.dp))
                TransportRow(
                    speed = state.speed,
                    isPlaying = state.isPlaying,
                    onSpeedClick = viewModel::cycleSpeed,
                    onPlayPauseClick = viewModel::togglePlayPause,
                )

                Spacer(Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "TRANSCRIPT",
                        color = colors.ink3,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            val job = state.transcriptionJob
            when {
                job != null && (job.status == TranscriptionState.QUEUED || job.status == TranscriptionState.RUNNING) -> {
                    item { TranscribingCard(job = job, modifier = Modifier.mrise()) }
                }
                state.segments.isEmpty() -> {
                    item {
                        EmptyTranscriptCard(
                            hasModel = state.models.isNotEmpty(),
                            onTranscribeClick = viewModel::startTranscription,
                            modifier = Modifier.mrise(),
                        )
                    }
                }
                else -> {
                    itemsIndexed(state.segments, key = { _, s -> s.id }) { index, segment ->
                        SegmentRow(
                            segment = segment,
                            active = index == state.activeSegmentIndex,
                            onClick = { viewModel.seekTo(segment.startMs) },
                            modifier = Modifier.mslide(index * 40),
                        )
                    }
                }
            }
        }
    }

    state.preflightBlock?.let { block ->
        val (title, needed, available) = when (block) {
            is PreflightBlock.Storage -> Triple("Not enough free space", block.neededBytes, block.availableBytes)
            is PreflightBlock.Memory -> Triple("Not enough free memory", block.neededBytes, block.availableBytes)
        }
        EdgeCaseDialog(
            glyph = "!",
            title = title,
            body = "This job needs about ${formatBytes(needed)} and this device has ${formatBytes(available)} " +
                "available right now. ${if (block is PreflightBlock.Storage) "Free up some space" else "Close a few apps"}, then try again.",
            detail = "checked before the job starts, never partway through",
            tone = EdgeCaseTone.WARNING,
            onDismiss = viewModel::dismissPreflightBlock,
            primaryLabel = "OK",
            onPrimary = viewModel::dismissPreflightBlock,
        )
    }

    if (recording != null && showActions) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showActions = false }, sheetState = sheetState) {
            RecordingActionsSheetContent(
                recording = recording,
                onRename = { showActions = false; renaming = true },
                onEditTranscript = { showActions = false /* TODO: transcript editor lands with Milestone 8 */ },
                onExport = { showActions = false /* TODO: export sheet lands with Milestone 8 */ },
                onDelete = { showActions = false; deleting = true },
                onClose = { showActions = false },
            )
        }
    }

    if (recording != null && renaming) {
        RenameDialog(
            recording = recording,
            onDismiss = { renaming = false },
            onConfirm = { name -> viewModel.rename(name); renaming = false },
        )
    }

    if (recording != null && deleting) {
        DeleteRecordingDialog(
            recording = recording,
            onDismiss = { deleting = false },
            onConfirm = { viewModel.delete(); deleting = false },
        )
    }
}

@Composable
private fun DetailHeader(recording: Recording?, onBack: () -> Unit, onOverflowClick: () -> Unit) {
    val colors = MurmurTheme.colors
    Column(modifier = Modifier.fillMaxWidth().background(colors.surf)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.surf2, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.ink2, modifier = Modifier.size(18.dp))
            }

            if (recording != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderChip("Edit") { /* TODO: transcript editor lands with Milestone 8 */ }
                    HeaderChip("Export") { /* TODO: export sheet lands with Milestone 8 */ }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.surf2, RoundedCornerShape(12.dp))
                            .clickable(onClick = onOverflowClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More actions", tint = colors.ink2, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    }
}

@Composable
private fun HeaderChip(label: String, onClick: () -> Unit) {
    val colors = MurmurTheme.colors
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(colors.surf2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.ink2, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

@Composable
private fun Scrubber(seed: Int, playedFraction: Float, onSeek: (Float) -> Unit) {
    val colors = MurmurTheme.colors
    val barCount = 54
    val heights = remember(seed) {
        val r = Random(seed)
        List(barCount) { 0.2f + r.nextFloat() * 0.8f }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { i, h ->
            val x = i * (barWidth + gap) + barWidth / 2
            val played = (i.toFloat() / heights.size) <= playedFraction
            drawLine(
                color = if (played) colors.pri else colors.surf3,
                start = Offset(x, size.height * (1 - h)),
                end = Offset(x, size.height),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TransportRow(speed: Float, isPlaying: Boolean, onSpeedClick: () -> Unit, onPlayPauseClick: () -> Unit) {
    val colors = MurmurTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(colors.surf2, RoundedCornerShape(16.dp))
                .clickable(onClick = onSpeedClick),
            contentAlignment = Alignment.Center,
        ) {
            val label = if (speed == speed.toLong().toFloat()) "${speed.toLong()}×" else "${speed}×"
            Text(label, color = colors.ink2, fontFamily = JetBrainsMonoFamily, fontSize = 12.5.sp)
        }

        val playInteraction = remember { MutableInteractionSource() }
        val playPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(if (playPressed) 0.92f else 1f, PressSpring, label = "playPress")
        Box(
            modifier = Modifier
                .zIndex(1f)
                .size(64.dp)
                .scale(playScale)
                .background(colors.pri, RoundedCornerShape(24.dp))
                .clickable(interactionSource = playInteraction, indication = null, onClick = onPlayPauseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = colors.priOn,
                modifier = Modifier.size(28.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(colors.surf2, RoundedCornerShape(16.dp))
                .clickable { /* TODO: bookmarks are a v1.1 idea, not in the FR-01..25 scope yet */ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Add bookmark", tint = colors.ink2, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyTranscriptCard(hasModel: Boolean, onTranscribeClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf2, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("Not transcribed yet", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasModel) {
                "Run it through the on-device model to get a searchable, editable transcript."
            } else {
                "No model downloaded yet. Once one is, this button runs it on-device."
            },
            color = colors.ink2,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .background(if (hasModel) colors.pri else colors.surf3, RoundedCornerShape(14.dp))
                .clickable(enabled = hasModel, onClick = onTranscribeClick)
                .padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Text(
                "Transcribe",
                color = if (hasModel) colors.priOn else colors.ink3,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            )
        }
    }
}

@Composable
private fun TranscribingCard(job: TranscriptionJob, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf2, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpinnerRing(colors.pri)
            Spacer(Modifier.width(8.dp))
            Text("TRANSCRIBING", color = colors.pri, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${job.windowsDone}/${job.windowsTotal} windows",
                color = colors.ink2,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(colors.surf3, RoundedCornerShape(4.dp))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(job.progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(colors.pri, RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun SegmentRow(segment: Segment, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    val bg by animateColorAsState(if (active) colors.surf2 else colors.bg, tween(300), label = "segBg")
    val fg by animateColorAsState(if (active) colors.ink else colors.ink2, tween(300), label = "segFg")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            formatMmSs(segment.startMs),
            color = colors.pri,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp),
        )
        Text(segment.displayText, color = fg, fontSize = 14.sp, lineHeight = 22.68.sp)
    }
}
