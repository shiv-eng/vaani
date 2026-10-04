package com.quietstack.vaani.ui.detail

import android.content.ClipData
import android.content.Intent
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.quietstack.vaani.export.TranscriptExporter
import com.quietstack.vaani.shared.data.ModelCatalogue
import com.quietstack.vaani.shared.domain.model.Bookmark
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.model.TranscriptionState
import com.quietstack.vaani.ui.components.DeleteRecordingDialog
import com.quietstack.vaani.ui.components.EdgeCaseDialog
import com.quietstack.vaani.ui.components.EdgeCaseTone
import com.quietstack.vaani.ui.components.ExportSheetContent
import com.quietstack.vaani.ui.components.LanguagePickerContent
import com.quietstack.vaani.ui.components.RecordingActionsSheetContent
import com.quietstack.vaani.ui.components.labelForLanguageCode
import com.quietstack.vaani.ui.components.RenameDialog
import com.quietstack.vaani.ui.components.SpinnerRing
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.components.rememberAnimateOnce
import com.quietstack.vaani.ui.components.rememberElapsedSeconds
import com.quietstack.vaani.shared.domain.usecase.LOADING_MODEL_SENTINEL
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.format.formatDurationShort
import com.quietstack.vaani.ui.format.formatMmSs
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private val PressSpring = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordingId: String,
    onBack: () -> Unit,
    onEditTranscript: (String) -> Unit,
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(recordingId) }),
) {
    val state by viewModel.state.collectAsState()
    val colors = VaaniTheme.colors
    val recording = state.recording
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val animatedRows = remember { mutableSetOf<Any>() }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    // Leaving this screen (back press, switching tabs) without an explicit
    // pause used to leave the recording playing in the background with no
    // visible transport controls anywhere else in the app — see
    // DetailViewModel.pauseForNavigation.
    DisposableEffect(Unit) {
        onDispose { viewModel.pauseForNavigation() }
    }

    val createTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(TranscriptExporter.plainText(state.segments).toByteArray())
            }
        }
    }
    val createAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/*")) { uri ->
        if (uri != null && recording != null) {
            val source = File(context.filesDir, "recordings/${recording.fileName}")
            context.contentResolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            }
        }
    }
    val createSrtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(TranscriptExporter.srt(state.segments).toByteArray())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).mrise()) {
        DetailHeader(
            recording = recording,
            onBack = onBack,
            onOverflowClick = { showActions = true },
            onEditClick = { recording?.let { onEditTranscript(it.id) } },
            onExportClick = { showExport = true },
        )

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
                    onBookmarkClick = viewModel::addBookmark,
                )

                if (state.bookmarks.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    BookmarksRow(
                        bookmarks = state.bookmarks,
                        onSeek = viewModel::seekTo,
                        onRemove = viewModel::deleteBookmark,
                    )
                }

                Spacer(Modifier.height(30.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "TRANSCRIPT",
                        color = colors.ink3,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp,
                    )
                    if (state.segments.isNotEmpty()) {
                        TranscriptViewToggle(
                            continuous = state.continuousTranscriptView,
                            onSelect = viewModel::setContinuousTranscriptView,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val job = state.transcriptionJob
            when {
                job != null && (job.status == TranscriptionState.QUEUED || job.status == TranscriptionState.RUNNING) -> {
                    item {
                        TranscribingCard(
                            progress = state.liveProgress ?: job.progress,
                            modifier = Modifier.mrise(),
                        )
                    }
                }
                state.segments.isEmpty() -> {
                    item {
                        EmptyTranscriptCard(
                            hasModel = state.models.any { it.id == ModelCatalogue.DEFAULT.id },
                            language = state.language,
                            onLanguageClick = { showLanguagePicker = true },
                            onTranscribeClick = viewModel::startTranscription,
                            modifier = Modifier.mrise(),
                        )
                    }
                }
                state.continuousTranscriptView -> {
                    item {
                        // No timestamps, no per-segment seek targets — just the
                        // words, for reading straight through or copying as prose.
                        // SelectionContainer gives the standard long-press ->
                        // select/copy/select-all behavior for free.
                        SelectionContainer(modifier = Modifier.mrise()) {
                            Text(
                                TranscriptExporter.plainText(state.segments),
                                color = colors.ink2,
                                fontSize = 14.sp,
                                lineHeight = 22.68.sp,
                            )
                        }
                    }
                }
                else -> {
                    itemsIndexed(state.segments, key = { _, s -> s.id }) { index, segment ->
                        val animate = rememberAnimateOnce(segment.id, animatedRows)
                        SegmentRow(
                            segment = segment,
                            active = index == state.activeSegmentIndex,
                            onClick = { viewModel.seekTo(segment.startMs) },
                            modifier = Modifier.mslide(index * 40, animate = animate),
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
            body = "Transcribing this needs about ${formatBytes(needed)}, and your phone has ${formatBytes(available)} " +
                "free right now. ${if (block is PreflightBlock.Storage) "Free up some space" else "Close a few apps"}, then try again.",
            detail = "checked before starting, so it won't fail partway through",
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
                onEditTranscript = { showActions = false; onEditTranscript(recording.id) },
                onExport = { showActions = false; showExport = true },
                onDelete = { showActions = false; deleting = true },
                onClose = { showActions = false },
            )
        }
    }

    if (recording != null && showExport) {
        val exportSheetState = rememberModalBottomSheetState()
        val plainText = TranscriptExporter.plainText(state.segments)
        val srtText = TranscriptExporter.srt(state.segments)
        ModalBottomSheet(onDismissRequest = { showExport = false }, sheetState = exportSheetState) {
            ExportSheetContent(
                durationMs = recording.durationMs,
                plainTextSizeBytes = plainText.toByteArray().size.toLong(),
                srtSizeBytes = srtText.toByteArray().size.toLong(),
                audioSizeBytes = recording.sizeBytes,
                onExportTxt = {
                    showExport = false
                    createTxtLauncher.launch("${recording.displayName}.txt")
                },
                onExportSrt = {
                    showExport = false
                    createSrtLauncher.launch("${recording.displayName}.srt")
                },
                onExportAudio = {
                    showExport = false
                    val extension = recording.fileName.substringAfterLast('.', missingDelimiterValue = "m4a")
                    createAudioLauncher.launch("${recording.displayName}.$extension")
                },
                onCopy = {
                    showExport = false
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", TranscriptExporter.withTimestamps(state.segments)))
                },
                onShare = {
                    showExport = false
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, plainText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share transcript"))
                },
                onClose = { showExport = false },
            )
        }
    }

    if (showLanguagePicker) {
        val languageSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showLanguagePicker = false }, sheetState = languageSheetState) {
            LanguagePickerContent(
                selected = state.language,
                onSelect = { code ->
                    viewModel.setLanguage(code)
                    showLanguagePicker = false
                },
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
private fun DetailHeader(
    recording: Recording?,
    onBack: () -> Unit,
    onOverflowClick: () -> Unit,
    onEditClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val colors = VaaniTheme.colors
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
                    HeaderChip("Edit", onClick = onEditClick)
                    HeaderChip("Export", onClick = onExportClick)
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
    val colors = VaaniTheme.colors
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
    val colors = VaaniTheme.colors
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
private fun TransportRow(
    speed: Float,
    isPlaying: Boolean,
    onSpeedClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val colors = VaaniTheme.colors
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

        val bookmarkInteraction = remember { MutableInteractionSource() }
        val bookmarkPressed by bookmarkInteraction.collectIsPressedAsState()
        val bookmarkScale by animateFloatAsState(if (bookmarkPressed) 0.9f else 1f, PressSpring, label = "bookmarkPress")
        Box(
            modifier = Modifier
                .size(46.dp)
                .scale(bookmarkScale)
                .background(colors.surf2, RoundedCornerShape(16.dp))
                .clickable(interactionSource = bookmarkInteraction, indication = null, onClick = onBookmarkClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Bookmark this moment", tint = colors.ink2, modifier = Modifier.size(18.dp))
        }
    }
}

/** Bookmarked moments in this recording — tap to jump there, tap the × to remove. */
@Composable
private fun BookmarksRow(bookmarks: List<Bookmark>, onSeek: (Long) -> Unit, onRemove: (String) -> Unit) {
    val colors = VaaniTheme.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bookmarks, key = { it.id }) { bookmark ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(colors.surf2, RoundedCornerShape(999.dp))
                    .clickable { onSeek(bookmark.positionMs) }
                    .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            ) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, tint = colors.pri, modifier = Modifier.size(13.dp))
                Text(formatMmSs(bookmark.positionMs), color = colors.ink2, fontFamily = JetBrainsMonoFamily, fontSize = 11.5.sp)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onRemove(bookmark.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = colors.ink3, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyTranscriptCard(
    hasModel: Boolean,
    language: String?,
    onLanguageClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaaniTheme.colors
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
        Row(
            modifier = Modifier
                .background(colors.surf3, RoundedCornerShape(999.dp))
                .clickable(onClick = onLanguageClick)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Language: ${labelForLanguageCode(language)}", color = colors.ink2, fontSize = 12.sp)
            Text("▾", color = colors.pri, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
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
private fun TranscribingCard(progress: Float, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    // A real, unambiguous signal from StartTranscriptionUseCase — checked
    // via TranscriptionEngine.isModelLoaded up front, not inferred from
    // progress sitting at 0%. That guess used to be wrong for every job
    // after the first one in a session (model already cached, so genuine
    // decode progress legitimately starts at 0 too) — a real, reported bug.
    val isLoadingModel = progress == LOADING_MODEL_SENTINEL

    // whisper.cpp exposes no sub-progress for the load itself — it's one
    // opaque native call — so there's no real percentage to show here, only
    // a spinner with no forward motion. Real testing showed that reading as
    // stuck outright. An honestly-ticking elapsed-time counter isn't fake
    // progress — it's the one true, always-increasing number available —
    // and gives a visible sign of life a static or looping spinner can't.
    val loadingElapsedSeconds = rememberElapsedSeconds(isLoadingModel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf2, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpinnerRing(colors.pri)
            Spacer(Modifier.width(8.dp))
            Text(
                if (isLoadingModel) "LOADING MODEL" else "TRANSCRIBING",
                color = colors.pri,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (isLoadingModel) "${loadingElapsedSeconds}s" else "${(progress * 100).toInt()}%",
                color = colors.ink2,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        // A large offline model takes real time to decode — measured several
        // times longer than the recording itself on the reference device. A
        // spinner with no explanation for that long reads as broken; this
        // sets the expectation up front instead of leaving it to be inferred.
        Text(
            if (isLoadingModel) {
                "loading the model — usually just a couple of seconds, and only once per app session"
            } else {
                "transcribing on-device — this can take a few minutes, longer recordings take longer"
            },
            color = colors.ink3,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(colors.surf3, RoundedCornerShape(4.dp))) {
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

/** Timestamped/seekable segments vs one continuous block of prose — see AppSettings.transcriptContinuousView. */
@Composable
private fun TranscriptViewToggle(continuous: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = VaaniTheme.colors
    Row(
        modifier = Modifier.background(colors.surf2, RoundedCornerShape(999.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleOption("Timeline", selected = !continuous, onClick = { onSelect(false) })
        ToggleOption("Text", selected = continuous, onClick = { onSelect(true) })
    }
}

@Composable
private fun ToggleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = VaaniTheme.colors
    val bg by animateColorAsState(if (selected) colors.surf3 else colors.surf2, tween(200), label = "toggleBg")
    val fg by animateColorAsState(if (selected) colors.ink else colors.ink3, tween(200), label = "toggleFg")
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun SegmentRow(segment: Segment, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
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
        SelectionContainer {
            Text(segment.displayText, color = fg, fontSize = 14.sp, lineHeight = 22.68.sp)
        }
    }
}
