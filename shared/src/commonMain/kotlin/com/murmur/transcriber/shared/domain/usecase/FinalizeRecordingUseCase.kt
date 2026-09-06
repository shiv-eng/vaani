package com.murmur.transcriber.shared.domain.usecase

import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import com.murmur.transcriber.shared.domain.repository.RecordingRepository

/**
 * Registers a just-stopped in-app recording (FR-01) as a library item. Mirrors
 * [ImportRecordingUseCase] but with source=RECORDED; the platform layer owns
 * MediaRecorder and renaming the temp file to its id-based final name before
 * calling this.
 */
class FinalizeRecordingUseCase(
    private val recordingRepository: RecordingRepository,
) {
    suspend operator fun invoke(
        filePath: String,
        durationMs: Long,
        sizeBytes: Long,
        mimeType: String,
        sampleRate: Int,
    ): Recording = recordingRepository.create(
        filePath = filePath,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        source = RecordingSource.RECORDED,
        mimeType = mimeType,
        sampleRate = sampleRate,
    )
}
