package com.murmur.transcriber.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.model.Segment
import com.murmur.transcriber.shared.domain.model.TranscriptionJob
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TranscriptionRepositoryImpl(
    private val database: MurmurDatabase,
) : TranscriptionRepository {

    private val jobQueries get() = database.jobQueries
    private val segmentQueries get() = database.segmentQueries

    override fun observeJob(recordingId: String): Flow<TranscriptionJob?> =
        jobQueries.selectLatestForRecording(recordingId, ::toJob).asFlow().mapToOneOrNull(Dispatchers.Default)

    override fun observeSegments(recordingId: String): Flow<List<Segment>> =
        segmentQueries.selectForRecording(recordingId, ::toSegment).asFlow().mapToList(Dispatchers.Default)

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createJob(
        recordingId: String,
        modelId: String,
        language: String?,
        windowsTotal: Int,
    ): TranscriptionJob = withContext(Dispatchers.Default) {
        val id = Uuid.random().toString()
        jobQueries.insert(
            id = id,
            recordingId = recordingId,
            modelId = modelId,
            language = language,
            status = TranscriptionState.QUEUED.name,
            windowsTotal = windowsTotal.toLong(),
            windowsDone = 0L,
            startedAt = Clock.System.now().toEpochMilliseconds(),
            finishedAt = null,
            error = null,
        )
        requireNotNull(jobQueries.selectById(id, ::toJob).executeAsOneOrNull())
    }

    override suspend fun getActiveJobs(): List<TranscriptionJob> = withContext(Dispatchers.Default) {
        jobQueries.selectActive(::toJob).executeAsList()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun appendSegment(jobId: String, segment: Segment) = withContext(Dispatchers.Default) {
        segmentQueries.insert(
            id = segment.id.ifBlank { Uuid.random().toString() },
            recordingId = segment.recordingId,
            windowIndex = segment.windowIndex.toLong(),
            startMs = segment.startMs,
            endMs = segment.endMs,
            text = segment.text,
            editedText = segment.editedText,
        )
    }

    override suspend fun markRunning(jobId: String) = withContext(Dispatchers.Default) {
        jobQueries.markRunning(jobId)
    }

    override suspend fun markWindowDone(jobId: String, windowsDone: Int) = withContext(Dispatchers.Default) {
        jobQueries.updateProgress(windowsDone.toLong(), jobId)
    }

    override suspend fun markComplete(jobId: String) = withContext(Dispatchers.Default) {
        jobQueries.updateStatus(TranscriptionState.COMPLETE.name, Clock.System.now().toEpochMilliseconds(), null, jobId)
    }

    override suspend fun markFailed(jobId: String, error: String) = withContext(Dispatchers.Default) {
        jobQueries.updateStatus(TranscriptionState.FAILED.name, Clock.System.now().toEpochMilliseconds(), error, jobId)
    }

    override suspend fun cancel(jobId: String) = withContext(Dispatchers.Default) {
        jobQueries.updateStatus(TranscriptionState.CANCELLED.name, Clock.System.now().toEpochMilliseconds(), null, jobId)
    }

    override suspend fun editSegment(segmentId: String, editedText: String) = withContext(Dispatchers.Default) {
        segmentQueries.editText(editedText, segmentId)
    }

    private fun toJob(
        id: String,
        recordingId: String,
        modelId: String,
        language: String?,
        status: String,
        windowsTotal: Long,
        windowsDone: Long,
        startedAt: Long,
        finishedAt: Long?,
        error: String?,
    ) = TranscriptionJob(
        id = id,
        recordingId = recordingId,
        modelId = modelId,
        language = language,
        status = TranscriptionState.valueOf(status),
        windowsTotal = windowsTotal.toInt(),
        windowsDone = windowsDone.toInt(),
        startedAt = startedAt,
        finishedAt = finishedAt,
        error = error,
    )

    private fun toSegment(
        id: String,
        recordingId: String,
        windowIndex: Long,
        startMs: Long,
        endMs: Long,
        text: String,
        editedText: String?,
    ) = Segment(
        id = id,
        recordingId = recordingId,
        windowIndex = windowIndex.toInt(),
        startMs = startMs,
        endMs = endMs,
        text = text,
        editedText = editedText,
    )
}
