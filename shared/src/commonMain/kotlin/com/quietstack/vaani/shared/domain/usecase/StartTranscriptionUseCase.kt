package com.quietstack.vaani.shared.domain.usecase

import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.pipeline.AudioDecoder
import com.quietstack.vaani.shared.domain.pipeline.WindowPlanner
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import com.quietstack.vaani.whisper.TranscriptionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * A negative progress value is never a real decode progress (always [0f,
 * 1f]), so it's an unambiguous sentinel a UI can check for real "the model
 * is genuinely loading right now" instead of guessing from progress==0f.
 */
const val LOADING_MODEL_SENTINEL = -1f

/**
 * Runs one transcription job window by window: decode, transcribe, persist,
 * report progress. Each window's segments are written immediately (FR-12), so
 * cancellation or process death loses at most the window in flight.
 *
 * Resuming an existing job from its last completed window (rather than always
 * starting fresh, as this does) is a separate use case, not yet written — it
 * needs the foreground service's restart path to exist first.
 */
class StartTranscriptionUseCase(
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val audioDecoder: AudioDecoder,
    private val transcriptionEngine: TranscriptionEngine,
    /** :shared holds no Context, so the platform layer supplies the recordings-dir join. */
    private val resolveAudioPath: (fileName: String) -> String,
    private val windowPlanner: WindowPlanner = WindowPlanner(),
) {
    /**
     * Emits progress in [0f, 1f] once real decoding has started, both between
     * windows and — via the engine's own progress callback — within the
     * window currently decoding. Most recordings are a single window
     * (WindowPlanner defaults to 30s), so without the latter this only ever
     * emitted 0 then 1, reading as "stuck, then a sudden jump" for anything
     * under ~30s (a real-device finding).
     *
     * Emits [LOADING_MODEL_SENTINEL] first instead, and only if a real
     * native model load is actually about to happen — checked up front via
     * [TranscriptionEngine.isModelLoaded] rather than left for a caller to
     * guess from progress sitting at 0%, which is indistinguishable from
     * genuine just-started decode progress. A caller distinguishing "loading"
     * from "transcribing" by treating exactly 0f as "still loading" was
     * wrong for every job after the first one in a session (session's model
     * already cached, real decode progress legitimately starts at 0 too) —
     * a real, reported bug this replaces with an unambiguous signal.
     *
     * channelFlow (not flow{}) because transcribeWindow's callback fires
     * synchronously from native code on this same thread, not as a suspend
     * call — trySend is the non-suspending way to forward it.
     */
    operator fun invoke(
        recordingId: String,
        modelPath: String,
        modelId: String,
        language: String?,
    ): Flow<Float> = channelFlow {
        val recording = requireNotNull(recordingRepository.getById(recordingId)) {
            "Unknown recording $recordingId"
        }
        val windows = windowPlanner.plan(recording.durationMs)
        val job = transcriptionRepository.createJob(recordingId, modelId, language, windows.size)

        if (!transcriptionEngine.isModelLoaded(modelPath)) {
            trySend(LOADING_MODEL_SENTINEL)
        }
        check(transcriptionEngine.loadModel(modelPath)) { "Failed to load model $modelPath" }
        try {
            windows.forEachIndexed { done, window ->
                val pcm = audioDecoder.decodeWindow(resolveAudioPath(recording.fileName), window)
                transcriptionEngine.transcribeWindow(
                    pcm,
                    sampleRate = 16_000,
                    language = language,
                    onProgress = { windowProgress -> trySend((done + windowProgress) / windows.size) },
                ).forEach { engineSegment ->
                    transcriptionRepository.appendSegment(
                        job.id,
                        Segment(
                            id = "",
                            recordingId = recordingId,
                            windowIndex = window.index,
                            startMs = window.startMs + engineSegment.startMs,
                            endMs = window.startMs + engineSegment.endMs,
                            text = engineSegment.text,
                            editedText = null,
                        ),
                    )
                }
                transcriptionRepository.markWindowDone(job.id, done + 1)
                trySend((done + 1).toFloat() / windows.size)
            }
            transcriptionRepository.markComplete(job.id)
        } catch (t: CancellationException) {
            transcriptionRepository.cancel(job.id)
            throw t
        } catch (t: Throwable) {
            transcriptionRepository.markFailed(job.id, t.message ?: "Unknown error")
            throw t
        }
        // The engine is a shared, app-lifetime instance (see WhisperModule) — it
        // deliberately stays loaded here rather than releasing after every job,
        // since reloading a ~57 MB model costs far more than transcribing a
        // typical recording. loadModel() itself handles switching models.
    }
}
