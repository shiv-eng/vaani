package com.murmur.transcriber.shared.domain.repository

import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun observeDownloaded(): Flow<List<TranscriptionModel>>

    /** Emits 0f..1f download progress; the app-private file is checksum-verified on completion (FR-16). */
    fun download(modelId: String): Flow<Float>

    suspend fun delete(modelId: String)
}
