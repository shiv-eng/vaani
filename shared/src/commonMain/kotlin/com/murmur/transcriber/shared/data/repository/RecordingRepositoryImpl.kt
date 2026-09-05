package com.murmur.transcriber.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecordingRepositoryImpl(
    private val database: MurmurDatabase,
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
        durationMs: Long,
        sizeBytes: Long,
        source: RecordingSource,
        mimeType: String,
        sampleRate: Int,
    ): Recording = withContext(Dispatchers.Default) {
        val id = Uuid.random().toString()
        val fileName = filePath.substringAfterLast('/')
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insert(
            id = id,
            displayName = fileName,
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

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        // The caller (use case) deletes the underlying audio file; this clears the rows.
        database.transaction {
            database.segmentQueries.deleteForRecording(id)
            database.jobQueries.deleteForRecording(id)
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
