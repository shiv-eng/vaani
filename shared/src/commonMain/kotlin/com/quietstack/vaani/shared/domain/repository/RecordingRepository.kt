package com.quietstack.vaani.shared.domain.repository

import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun observeAll(): Flow<List<Recording>>
    suspend fun getById(id: String): Recording?

    /** Registers a new library item; [filePath] already sits in app-private storage. */
    suspend fun create(
        filePath: String,
        displayName: String,
        durationMs: Long,
        sizeBytes: Long,
        source: RecordingSource,
        mimeType: String,
        sampleRate: Int,
        id: String? = null,
    ): Recording

    /** display_name is metadata only; file_name never changes (FR-07). */
    suspend fun rename(id: String, newName: String)

    /**
     * A live-recorded item is inserted at durationMs=0 the moment recording
     * starts (see BeginRecordingUseCase), so live segments have a real id to
     * attach to as they're transcribed. This fills in the real values once
     * recording actually stops.
     */
    suspend fun updateDuration(id: String, durationMs: Long, sizeBytes: Long)

    /** Removes the audio file, transcript and database rows together (FR-08). */
    suspend fun delete(id: String)
}
