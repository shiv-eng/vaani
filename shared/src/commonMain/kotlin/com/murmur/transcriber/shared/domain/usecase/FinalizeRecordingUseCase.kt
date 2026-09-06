package com.murmur.transcriber.shared.domain.usecase

import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/**
 * Recordings have no natural name (unlike an imported file's own filename),
 * so give each one a readable label instead of exposing the id-based
 * storage filename in the library UI.
 */
private fun friendlyRecordingName(epochMs: Long): String {
    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when (val h = dt.hour % 12) { 0 -> 12; else -> h }
    val minute = dt.minute.toString().padStart(2, '0')
    val meridiem = if (dt.hour < 12) "AM" else "PM"
    return "Recording — ${dt.dayOfMonth} ${MONTH_NAMES[dt.monthNumber - 1]}, $hour12:$minute $meridiem"
}

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
        displayName = friendlyRecordingName(Clock.System.now().toEpochMilliseconds()),
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        source = RecordingSource.RECORDED,
        mimeType = mimeType,
        sampleRate = sampleRate,
    )
}
