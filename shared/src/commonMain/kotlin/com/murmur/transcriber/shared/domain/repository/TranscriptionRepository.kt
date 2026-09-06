package com.murmur.transcriber.shared.domain.repository

import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.model.TranscriptionJob
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
    fun observeJob(recordingId: String): Flow<TranscriptionJob?>
    fun observeSegments(recordingId: String): Flow<List<Segment>>

    suspend fun createJob(recordingId: String, modelId: String, language: String?, windowsTotal: Int): TranscriptionJob

    /** Jobs left QUEUED or RUNNING — used at app start to find ones no live service still owns. */
    suspend fun getActiveJobs(): List<TranscriptionJob>

    /** Persists one completed window immediately, per FR-12. */
    suspend fun appendSegment(jobId: String, segment: Segment)

    suspend fun markRunning(jobId: String)
    suspend fun markWindowDone(jobId: String, windowsDone: Int)
    suspend fun markComplete(jobId: String)
    suspend fun markFailed(jobId: String, error: String)
    suspend fun cancel(jobId: String)

    suspend fun editSegment(segmentId: String, editedText: String)
}
