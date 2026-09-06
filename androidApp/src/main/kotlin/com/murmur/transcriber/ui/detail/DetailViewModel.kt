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
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val SPEED_CYCLE = listOf(1f, 1.5f, 2f)

data class DetailState(
    val recording: Recording? = null,
    val segments: List<Segment> = emptyList(),
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
) {
    val activeSegmentIndex: Int
        get() = segments.indexOfLast { positionMs >= it.startMs }
}

class DetailViewModel(
    private val recordingId: String,
    private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
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

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
