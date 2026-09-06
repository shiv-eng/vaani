package com.murmur.transcriber.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class RecorderUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    /** 0..32767, from [MediaRecorder.getMaxAmplitude] — real mic input, not simulated. */
    val amplitude: Int = 0,
    val fileSizeBytes: Long = 0L,
)

data class FinishedRecording(
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val sampleRate: Int,
)

private const val SAMPLE_RATE = 44_100

/**
 * Wraps [MediaRecorder]: writes continuously to its final app-private path from
 * the first byte (FR-01 — a crash loses at most the last moments, and there is
 * no separate temp-file-then-rename step to lose). Polls amplitude and elapsed
 * time on a coroutine rather than a Handler, to stay consistent with the rest
 * of the app's coroutine-based state.
 */
class AudioRecorderController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RecorderUiState())
    val state: StateFlow<RecorderUiState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtElapsedMs = 0L
    private var pausedAccumulatedMs = 0L
    private var tickerJob: Job? = null

    fun start() {
        check(recorder == null) { "Already recording" }

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")

        val mediaRecorder = createMediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        outputFile = file
        startedAtElapsedMs = System.currentTimeMillis()
        pausedAccumulatedMs = 0L
        _state.value = RecorderUiState(isRecording = true)
        startTicker()
    }

    fun pause() {
        val mediaRecorder = recorder ?: return
        if (_state.value.isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) mediaRecorder.pause()
        pausedAccumulatedMs = System.currentTimeMillis()
        _state.update { it.copy(isPaused = true) }
    }

    fun resume() {
        val mediaRecorder = recorder ?: return
        if (!_state.value.isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) mediaRecorder.resume()
        startedAtElapsedMs += System.currentTimeMillis() - pausedAccumulatedMs
        _state.update { it.copy(isPaused = false) }
    }

    fun stop(): FinishedRecording {
        val mediaRecorder = requireNotNull(recorder) { "Not recording" }
        val file = requireNotNull(outputFile)
        val elapsed = _state.value.elapsedMs

        tickerJob?.cancel()
        tickerJob = null
        runCatching { mediaRecorder.stop() }
        mediaRecorder.release()
        recorder = null
        outputFile = null
        _state.value = RecorderUiState()

        return FinishedRecording(
            filePath = file.absolutePath,
            durationMs = elapsed,
            sizeBytes = file.length(),
            mimeType = "audio/mp4",
            sampleRate = SAMPLE_RATE,
        )
    }

    private fun startTicker() {
        tickerJob = scope.launch {
            while (isActive && recorder != null) {
                val current = recorder
                if (current != null && !_state.value.isPaused) {
                    val elapsed = System.currentTimeMillis() - startedAtElapsedMs
                    val amplitude = runCatching { current.maxAmplitude }.getOrDefault(0)
                    _state.update {
                        it.copy(elapsedMs = elapsed, amplitude = amplitude, fileSizeBytes = outputFile?.length() ?: 0L)
                    }
                }
                kotlinx.coroutines.delay(150)
            }
        }
    }
}

private fun createMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
