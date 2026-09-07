package com.quietstack.vaani.shared.domain.usecase

import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/**
 * Recordings have no natural name (unlike an imported file's own filename),
 * so give each one a readable label instead of exposing the id-based
 * storage filename in the library UI. internal (not private): also used by
 * BeginRecordingUseCase, which now creates the row at recording start.
 */
internal fun friendlyRecordingName(epochMs: Long): String {
    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when (val h = dt.hour % 12) { 0 -> 12; else -> h }
    val minute = dt.minute.toString().padStart(2, '0')
    val meridiem = if (dt.hour < 12) "AM" else "PM"
    return "Recording — ${dt.dayOfMonth} ${MONTH_NAMES[dt.monthNumber - 1]}, $hour12:$minute $meridiem"
}

/**
 * Fills in the real duration/size on a recording row once it actually stops
 * (FR-01) — the row itself was already created at recording start by
 * [BeginRecordingUseCase] so live transcription had a real id to attach
 * segments to throughout. Previously this use case did the insert too; now
 * it's purely the finishing update.
 */
class FinalizeRecordingUseCase(
    private val recordingRepository: RecordingRepository,
) {
    suspend operator fun invoke(id: String, durationMs: Long, sizeBytes: Long) {
        recordingRepository.updateDuration(id, durationMs, sizeBytes)
    }
}
