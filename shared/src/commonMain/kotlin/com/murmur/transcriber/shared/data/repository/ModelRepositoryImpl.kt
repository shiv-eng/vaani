package com.murmur.transcriber.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.murmur.transcriber.shared.data.ModelCatalogue
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import com.murmur.transcriber.shared.domain.pipeline.ModelDownloader
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class ModelRepositoryImpl(
    private val database: MurmurDatabase,
    private val modelDownloader: ModelDownloader,
    /** :shared holds no Context, so the platform layer supplies the models-dir join. */
    private val resolveModelPath: (fileName: String) -> String,
) : ModelRepository {

    private val queries get() = database.modelQueries

    override fun observeDownloaded(): Flow<List<TranscriptionModel>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override fun download(modelId: String): Flow<Float> = flow {
        val entry = requireNotNull(ModelCatalogue.byId(modelId)) { "Unknown model $modelId" }
        val targetPath = resolveModelPath(entry.fileName)

        // The downloader's own progress tops out just under 1f during transfer and
        // emits exactly 1f only once the checksum has passed — reserve that value
        // for after the DB row lands too, so "100%" always means "fully usable".
        modelDownloader.download(entry.url, targetPath, entry.sha256).collect { progress ->
            emit(progress.coerceAtMost(0.999f))
        }

        withContext(Dispatchers.Default) {
            queries.insert(
                id = entry.id,
                name = entry.name,
                variant = entry.variant,
                fileName = entry.fileName,
                sizeBytes = entry.sizeBytes,
                sha256 = entry.sha256,
                downloadedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
        emit(1f)
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
