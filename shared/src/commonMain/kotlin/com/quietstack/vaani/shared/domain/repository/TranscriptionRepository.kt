package com.quietstack.vaani.shared.domain.repository

import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.model.TranscriptionJob
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
    fun observeJob(recordingId: String): Flow<TranscriptionJob?>
    fun observeSegments(recordingId: String): Flow<List<Segment>>

    suspend fun createJob(recordingId: String, modelId: String, language: String?, windowsTotal: Int): TranscriptionJob

    /** Persists one completed window immediately, per FR-12. */
    suspend fun appendSegment(jobId: String, segment: Segment)

    suspend fun markWindowDone(jobId: String, windowsDone: Int)
    suspend fun markComplete(jobId: String)
    suspend fun markFailed(jobId: String, error: String)
    suspend fun cancel(jobId: String)

    suspend fun editSegment(segmentId: String, editedText: String)

    /** Recording ids whose transcript text contains [query] (case-sensitivity follows SQLite LIKE). */
    suspend fun searchRecordingIds(query: String): List<String>

    /**
     * A job stuck QUEUED/RUNNING only means the foreground service that owned
     * it died before finishing (force-stop, OS kill) — run once at every app
     * start so those show as failed (offering the manual "Transcribe" button
     * again) instead of a permanent spinner.
     */
    suspend fun failAllRunningJobs()
}
