package com.murmur.transcriber.transcription

import android.app.ActivityManager
import android.content.Context
import com.murmur.transcriber.service.TranscriptionForegroundService
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository

/**
 * TranscriptionForegroundService runs in-process (no android:process override), so an OEM
 * battery killer or crash that ends the service takes the whole app process with it. That
 * means any job still QUEUED/RUNNING at the next process start was orphaned by the previous
 * one — there is no live service left to finish it. Marking it FAILED with [STUCK_JOB_ERROR]
 * lets the UI (DetailViewModel) tell this case apart from a real transcription failure and
 * offer to resume instead of just reporting an error.
 */
class StuckJobRecovery(
    private val context: Context,
    private val transcriptionRepository: TranscriptionRepository,
) {
    suspend fun recoverIfNeeded() {
        if (isServiceRunning()) return
        transcriptionRepository.getActiveJobs().forEach { job ->
            transcriptionRepository.markFailed(job.id, STUCK_JOB_ERROR)
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == TranscriptionForegroundService::class.java.name }
    }

    companion object {
        const val STUCK_JOB_ERROR = "SERVICE_KILLED"
    }
}
