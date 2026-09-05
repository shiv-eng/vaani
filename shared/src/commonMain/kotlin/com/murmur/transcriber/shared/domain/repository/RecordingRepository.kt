package com.murmur.transcriber.shared.domain.repository

import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.RecordingSource
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun observeAll(): Flow<List<Recording>>
    suspend fun getById(id: String): Recording?

    /** Registers a new library item; [filePath] already sits in app-private storage. */
    suspend fun create(
        filePath: String,
        durationMs: Long,
        sizeBytes: Long,
        source: RecordingSource,
        mimeType: String,
        sampleRate: Int,
    ): Recording

    /** display_name is metadata only; file_name never changes (FR-07). */
    suspend fun rename(id: String, newName: String)

    /** Removes the audio file, transcript and database rows together (FR-08). */
    suspend fun delete(id: String)
}
