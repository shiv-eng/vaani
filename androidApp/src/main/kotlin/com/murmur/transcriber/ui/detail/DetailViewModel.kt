package com.murmur.transcriber.ui.detail

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
import com.murmur.transcriber.playback.PlaybackService
import com.murmur.transcriber.recording.DeleteRecordingUseCase
import com.murmur.transcriber.service.TranscriptionForegroundService
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.model.TranscriptionJob
import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import com.murmur.transcriber.transcription.PreflightCheck
import com.murmur.transcriber.transcription.StuckJobRecovery
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
    val stuckJobDismissed: Boolean = false,
) {
    val activeSegmentIndex: Int
        get() = segments.indexOfLast { positionMs >= it.startMs }

    /** A job an earlier, now-dead process left behind — see [StuckJobRecovery]. */
    val stuckJob: TranscriptionJob?
        get() = transcriptionJob?.takeIf {
            !stuckJobDismissed && it.status == TranscriptionState.FAILED && it.error == StuckJobRecovery.STUCK_JOB_ERROR
        }
}

class DetailViewModel(
    private val recordingId: String,
    private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val modelRepository: ModelRepository,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var isTicking = false

    init {
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
            modelRepository.observeDownloaded().collect { models ->
                _state.update { it.copy(models = models) }
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

    fun startTranscription() {
        val model = _state.value.models.firstOrNull() ?: return
        startTranscription(model, language = null)
    }

    /**
     * Re-invokes the same use case as [startTranscription]; it always starts from window 0,
     * so this restarts the job rather than resuming from windowsDone (see resumeStuckJob's
     * dialog copy, which is honest about that instead of claiming a true resume).
     */
    fun resumeStuckJob() {
        val job = _state.value.transcriptionJob ?: return
        _state.update { it.copy(stuckJobDismissed = true) }
        val model = _state.value.models.firstOrNull { it.id == job.modelId } ?: _state.value.models.firstOrNull() ?: return
        startTranscription(model, job.language)
    }

    fun dismissStuckJob() {
        _state.update { it.copy(stuckJobDismissed = true) }
    }

    private fun startTranscription(model: TranscriptionModel, language: String?) {
        when (val result = PreflightCheck.run(context, model.sizeBytes)) {
            is PreflightCheck.Result.Ok ->
                TranscriptionForegroundService.start(context, recordingId, model.id, language)
            is PreflightCheck.Result.InsufficientStorage ->
                _state.update { it.copy(preflightBlock = PreflightBlock.Storage(result.neededBytes, result.availableBytes)) }
            is PreflightCheck.Result.LowMemory ->
                _state.update { it.copy(preflightBlock = PreflightBlock.Memory(result.neededBytes, result.availableBytes)) }
        }
    }

    fun dismissPreflightBlock() {
        _state.update { it.copy(preflightBlock = null) }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
