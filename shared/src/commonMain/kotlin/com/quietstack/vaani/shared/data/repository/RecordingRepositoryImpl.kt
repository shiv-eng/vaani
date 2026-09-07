package com.quietstack.vaani.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.quietstack.vaani.shared.db.VaaniDatabase
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.model.TranscriptionState
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecordingRepositoryImpl(
    private val database: VaaniDatabase,
) : RecordingRepository {

    private val queries get() = database.recordingQueries

    override fun observeAll(): Flow<List<Recording>> =
        queries.selectAllWithStatus(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: String): Recording? = withContext(Dispatchers.Default) {
        queries.selectByIdWithStatus(id, ::toDomain).executeAsOneOrNull()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(
        filePath: String,
        displayName: String,
        durationMs: Long,
        sizeBytes: Long,
        source: RecordingSource,
        mimeType: String,
        sampleRate: Int,
        id: String?,
    ): Recording = withContext(Dispatchers.Default) {
        val id = id ?: Uuid.random().toString()
        val fileName = filePath.substringAfterLast('/')
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insert(
            id = id,
            displayName = displayName,
            fileName = fileName,
            createdAt = createdAt,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            source = source.name,
            mimeType = mimeType,
            sampleRate = sampleRate.toLong(),
        )
        requireNotNull(getById(id))
    }

    override suspend fun rename(id: String, newName: String) = withContext(Dispatchers.Default) {
        queries.rename(newName, id)
    }

    override suspend fun updateDuration(id: String, durationMs: Long, sizeBytes: Long) = withContext(Dispatchers.Default) {
        queries.updateDuration(durationMs, sizeBytes, id)
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        // The caller (use case) deletes the underlying audio file; this clears the rows.
        database.transaction {
            database.segmentQueries.deleteForRecording(id)
            database.jobQueries.deleteForRecording(id)
            database.bookmarkQueries.deleteForRecording(id)
            queries.deleteById(id)
        }
    }

    private fun toDomain(
        id: String,
        displayName: String,
        fileName: String,
        createdAt: Long,
        durationMs: Long,
        sizeBytes: Long,
        source: String,
        mimeType: String,
        sampleRate: Long,
        transcriptionState: String,
    ) = Recording(
        id = id,
        displayName = displayName,
        fileName = fileName,
        createdAt = createdAt,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        source = RecordingSource.valueOf(source),
        mimeType = mimeType,
        sampleRate = sampleRate.toInt(),
        transcriptionState = TranscriptionState.valueOf(transcriptionState),
    )
}
