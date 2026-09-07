package com.quietstack.vaani.shared.domain.usecase

import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.repository.RecordingRepository

/**
 * Registers a file already copied into app-private storage (FR-04) as a library item.
 * The platform layer owns the copy-from-content-URI step, since that API is Android/iOS
 * specific; this use case only ever sees a path that already belongs to the app.
 */
class ImportRecordingUseCase(
    private val recordingRepository: RecordingRepository,
) {
    suspend operator fun invoke(
        filePath: String,
        displayName: String,
        durationMs: Long,
        sizeBytes: Long,
        mimeType: String,
        sampleRate: Int,
    ): Recording = recordingRepository.create(
        filePath = filePath,
        displayName = displayName,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        source = RecordingSource.IMPORTED,
        mimeType = mimeType,
        sampleRate = sampleRate,
    )
}
