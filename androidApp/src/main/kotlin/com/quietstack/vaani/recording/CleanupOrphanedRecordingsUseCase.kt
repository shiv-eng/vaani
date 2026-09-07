package com.quietstack.vaani.recording

import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.first

/**
 * Runs once at every app start (see VaaniApplication) to clean up debris
 * from a recording or transcription job that got interrupted by the process
 * dying mid-way — a force-stop, an OS-level kill, a crash. A recording row
 * is created the instant recording starts (BeginRecordingUseCase), before
 * MediaRecorder has produced a real file, specifically so live transcription
 * has a real id to attach segments to; if the process dies before
 * FinalizeRecordingUseCase ever runs, that row is stuck at durationMs=0
 * forever with no usable audio (MediaRecorder never finalized the file's
 * container), and its job is stuck QUEUED/RUNNING showing a permanent
 * spinner. Real-device testing surfaced exactly this — nothing manages it
 * without a real user, and a real user shouldn't have to.
 */
class CleanupOrphanedRecordingsUseCase(
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
    // A plain function rather than the concrete DeleteRecordingUseCase so this
    // class's selection logic (which recordings count as orphaned) is unit
    // testable on the JVM without an Android Context.
    private val deleteRecording: suspend (recordingId: String) -> Unit,
) {
    suspend operator fun invoke() {
        // Captured before doing anything else: a real recording can only be
        // created once the app has a running UI, which is necessarily later
        // than this — VaaniApplication.onCreate (which calls this) hasn't
        // even returned yet, let alone shown a screen the user could tap
        // record on. So anything at durationMs=0 created before this instant
        // is unambiguously left over from a previous, now-dead process, never
        // one this same process is still actively recording.
        val processStartedAt = System.currentTimeMillis()

        transcriptionRepository.failAllRunningJobs()
        val orphaned = recordingRepository.observeAll().first()
            .filter { it.source == RecordingSource.RECORDED && it.durationMs == 0L && it.createdAt < processStartedAt }
        for (recording in orphaned) {
            deleteRecording(recording.id)
        }
    }
}
