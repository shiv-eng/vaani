package com.murmur.transcriber.shared.domain.usecase

import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.pipeline.AudioDecoder
import com.murmur.transcriber.shared.domain.pipeline.WindowPlanner
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import com.murmur.transcriber.whisper.TranscriptionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    /** Emits progress in [0f, 1f] as each window completes. */
    operator fun invoke(
        recordingId: String,
        modelPath: String,
        modelId: String,
        language: String?,
    ): Flow<Float> = flow {
        val recording = requireNotNull(recordingRepository.getById(recordingId)) {
            "Unknown recording $recordingId"
        }
        val windows = windowPlanner.plan(recording.durationMs)
        val job = transcriptionRepository.createJob(recordingId, modelId, language, windows.size)

        check(transcriptionEngine.loadModel(modelPath)) { "Failed to load model $modelPath" }
        try {
            transcriptionRepository.markRunning(job.id)
            windows.forEachIndexed { done, window ->
                val pcm = audioDecoder.decodeWindow(resolveAudioPath(recording.fileName), window)
                transcriptionEngine.transcribeWindow(pcm, sampleRate = 16_000, language = language)
                    .forEach { engineSegment ->
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
                emit((done + 1).toFloat() / windows.size)
            }
            transcriptionRepository.markComplete(job.id)
        } catch (t: CancellationException) {
            transcriptionRepository.cancel(job.id)
            throw t
        } catch (t: Throwable) {
            transcriptionRepository.markFailed(job.id, t.message ?: "Unknown error")
            throw t
        } finally {
            transcriptionEngine.release()
        }
    }
}
