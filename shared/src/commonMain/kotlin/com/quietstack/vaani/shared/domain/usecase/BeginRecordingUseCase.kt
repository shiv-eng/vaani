package com.quietstack.vaani.shared.domain.usecase

import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import kotlinx.datetime.Clock

/**
 * Registers a recording's library row the moment recording *starts* (not
 * after it stops, as before) — live transcription needs a real recordingId
 * to attach segments to as they're produced. Inserted at durationMs=0;
 * [FinalizeRecordingUseCase] fills in the real duration/size once recording
 * actually stops.
 */
class BeginRecordingUseCase(
    private val recordingRepository: RecordingRepository,
) {
    suspend operator fun invoke(
        id: String,
        filePath: String,
        mimeType: String,
        sampleRate: Int,
    ): Recording = recordingRepository.create(
        filePath = filePath,
        displayName = friendlyRecordingName(Clock.System.now().toEpochMilliseconds()),
        durationMs = 0L,
        sizeBytes = 0L,
        source = RecordingSource.RECORDED,
        mimeType = mimeType,
        sampleRate = sampleRate,
        id = id,
    )
}
