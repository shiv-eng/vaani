package com.murmur.transcriber.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ModelRepositoryImpl(
    private val database: MurmurDatabase,
) : ModelRepository {

    private val queries get() = database.modelQueries

    override fun observeDownloaded(): Flow<List<TranscriptionModel>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override fun download(modelId: String): Flow<Float> = flow {
        // TODO: resumable Ktor range-request download into files/models/, checksum
        // verified against the catalogue sha256 on completion (FR-16). Lands with
        // the first-run flow, once a model catalogue and CDN URL exist.
        throw NotImplementedError("Model download is not wired yet — see PRD section 06, First run")
    }

    override suspend fun delete(modelId: String) = withContext(Dispatchers.Default) {
        queries.deleteById(modelId)
    }

    private fun toDomain(
        id: String,
        name: String,
        variant: String,
        fileName: String,
        sizeBytes: Long,
        sha256: String,
        downloadedAt: Long,
    ) = TranscriptionModel(
        id = id,
        name = name,
        variant = variant,
        fileName = fileName,
        sizeBytes = sizeBytes,
        sha256 = sha256,
        downloadedAt = downloadedAt,
    )
}
