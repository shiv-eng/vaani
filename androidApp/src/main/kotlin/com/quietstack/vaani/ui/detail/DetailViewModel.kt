package com.quietstack.vaani.ui.detail

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.quietstack.vaani.playback.PlaybackService
import com.quietstack.vaani.recording.DeleteRecordingUseCase
import com.quietstack.vaani.service.TranscriptionForegroundService
import com.quietstack.vaani.shared.domain.model.Bookmark
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.model.TranscriptionJob
import com.quietstack.vaani.shared.domain.model.TranscriptionModel
import com.quietstack.vaani.shared.data.AppSettings
import com.quietstack.vaani.shared.data.ModelCatalogue
import com.quietstack.vaani.shared.domain.pipeline.TranscriptionProgressTracker
import com.quietstack.vaani.shared.domain.repository.BookmarkRepository
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import com.quietstack.vaani.transcription.PreflightCheck
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val SPEED_CYCLE = listOf(1f, 1.5f, 2f)

sealed interface PreflightBlock {
    data class Storage(val neededBytes: Long, val availableBytes: Long) : PreflightBlock
    data class Memory(val neededBytes: Long, val availableBytes: Long) : PreflightBlock
}

data class DetailState(
    val recording: Recording? = null,
    val segments: List<Segment> = emptyList(),
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val deleted: Boolean = false,
    val models: List<TranscriptionModel> = emptyList(),
    val transcriptionJob: TranscriptionJob? = null,
    val preflightBlock: PreflightBlock? = null,
    val language: String? = null,
    val liveProgress: Float? = null,
    val continuousTranscriptView: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
) {
    val activeSegmentIndex: Int
        get() = segments.indexOfLast { positionMs >= it.startMs }
}

class DetailViewModel(
    private val recordingId: String,
    private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val modelRepository: ModelRepository,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val appSettings: AppSettings,
    private val progressTracker: TranscriptionProgressTracker,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var isTicking = false

    init {
        _state.update {
            it.copy(
                language = appSettings.transcriptionLanguage,
                continuousTranscriptView = appSettings.transcriptContinuousView,
            )
        }
        viewModelScope.launch {
            val recording = recordingRepository.getById(recordingId)
            _state.update { it.copy(recording = recording, durationMs = recording?.durationMs ?: 0) }
            if (recording != null) connectPlayer(recording)
        }
        viewModelScope.launch {
            transcriptionRepository.observeSegments(recordingId).collect { segments ->
                _state.update { it.copy(segments = segments) }
            }
        }
        viewModelScope.launch {
            transcriptionRepository.observeJob(recordingId).collect { job ->
                _state.update { it.copy(transcriptionJob = job) }
            }
        }
        viewModelScope.launch {
            progressTracker.progress.collect { entry ->
                _state.update { it.copy(liveProgress = entry?.takeIf { (id, _) -> id == recordingId }?.second) }
            }
        }
        viewModelScope.launch {
            modelRepository.observeDownloaded().collect { models ->
                _state.update { it.copy(models = models) }
            }
        }
        viewModelScope.launch {
            bookmarkRepository.observeForRecording(recordingId).collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    private fun connectPlayer(recording: Recording) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val mediaController = future.get()
            controller = mediaController

            val file = File(context.filesDir, "recordings/${recording.fileName}")
            mediaController.setMediaItem(MediaItem.fromUri(file.toUri()))
            mediaController.prepare()

            mediaController.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _state.update { it.copy(isPlaying = playing) }
                    if (playing) startTicking()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _state.update {
                            it.copy(isReady = true, durationMs = mediaController.duration.coerceAtLeast(0))
                        }
                    }
                }
            })
            _state.update { it.copy(isReady = true) }
        }, MoreExecutors.directExecutor())
    }

    private fun startTicking() {
        if (isTicking) return
        isTicking = true
        viewModelScope.launch {
            while (controller?.isPlaying == true) {
                _state.update { it.copy(positionMs = controller?.currentPosition ?: 0) }
                delay(200)
            }
            isTicking = false
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    /**
     * Called when Detail leaves composition (back press, tapping a bottom
     * tab, closing the app). Playback runs through a MediaSessionService
     * specifically so it survives the screen turning off — but that's
     * "leaving this app screen" happening on purpose, e.g. a locked phone or
     * a genuine background-listening session. Navigating to another in-app
     * page is different: nothing else in the app plays that audio back or
     * shows transport controls for it, so leaving it running is just a
     * recording nobody's looking at that keeps making noise until it ends.
     */
    fun pauseForNavigation() {
        controller?.pause()
    }

    fun cycleSpeed() {
        val c = controller ?: return
        val next = SPEED_CYCLE[(SPEED_CYCLE.indexOf(_state.value.speed) + 1) % SPEED_CYCLE.size]
        c.playbackParameters = PlaybackParameters(next)
        _state.update { it.copy(speed = next) }
    }

    fun seekToFraction(fraction: Float) {
        val duration = _state.value.durationMs
        if (duration <= 0) return
        val target = (duration * fraction.coerceIn(0f, 1f)).toLong()
        controller?.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            recordingRepository.rename(recordingId, newName)
            _state.update { s -> s.copy(recording = s.recording?.copy(displayName = newName)) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteRecordingUseCase(recordingId)
            _state.update { it.copy(deleted = true) }
        }
    }

    /**
     * Auto-detect repeatedly misidentified real Hindi speech as a
     * related-but-wrong language (Urdu, then Marathi) in testing, but this
     * app has international users who don't speak Hindi — hardcoding one
     * language for everyone is wrong in the other direction. So this is a
     * user choice (persisted in AppSettings), not a hardcoded default.
     */
    fun setLanguage(code: String?) {
        appSettings.transcriptionLanguage = code
        _state.update { it.copy(language = code) }
    }

    fun startTranscription() {
        // See RecordingForegroundService's identical fix: "first downloaded
        // model" now also matches IndicConformer's Hindi model/tokens files,
        // which aren't a selectable transcription model at all.
        val model = _state.value.models.firstOrNull { it.id == ModelCatalogue.DEFAULT.id } ?: return
        when (val result = PreflightCheck.run(context, model.sizeBytes)) {
            is PreflightCheck.Result.Ok ->
                TranscriptionForegroundService.start(context, recordingId, model.id, language = _state.value.language)
            is PreflightCheck.Result.InsufficientStorage ->
                _state.update { it.copy(preflightBlock = PreflightBlock.Storage(result.neededBytes, result.availableBytes)) }
            is PreflightCheck.Result.LowMemory ->
                _state.update { it.copy(preflightBlock = PreflightBlock.Memory(result.neededBytes, result.availableBytes)) }
        }
    }

    fun dismissPreflightBlock() {
        _state.update { it.copy(preflightBlock = null) }
    }

    fun setContinuousTranscriptView(continuous: Boolean) {
        appSettings.transcriptContinuousView = continuous
        _state.update { it.copy(continuousTranscriptView = continuous) }
    }

    /** Marks the current playback position — jumping back to it later is just tapping it in the bookmarks row. */
    fun addBookmark() {
        viewModelScope.launch {
            bookmarkRepository.add(recordingId, _state.value.positionMs)
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            bookmarkRepository.delete(id)
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
