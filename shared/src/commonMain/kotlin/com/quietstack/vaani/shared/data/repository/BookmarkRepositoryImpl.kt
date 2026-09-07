package com.quietstack.vaani.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.quietstack.vaani.shared.db.VaaniDatabase
import com.quietstack.vaani.shared.domain.model.Bookmark
import com.quietstack.vaani.shared.domain.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BookmarkRepositoryImpl(
    private val database: VaaniDatabase,
) : BookmarkRepository {

    private val queries get() = database.bookmarkQueries

    override fun observeForRecording(recordingId: String): Flow<List<Bookmark>> =
        queries.selectForRecording(recordingId, ::toDomain).asFlow().mapToList(Dispatchers.Default)

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun add(recordingId: String, positionMs: Long): Bookmark = withContext(Dispatchers.Default) {
        val id = Uuid.random().toString()
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insert(id = id, recordingId = recordingId, positionMs = positionMs, createdAt = createdAt)
        Bookmark(id = id, recordingId = recordingId, positionMs = positionMs, createdAt = createdAt)
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    private fun toDomain(id: String, recordingId: String, positionMs: Long, createdAt: Long) =
        Bookmark(id = id, recordingId = recordingId, positionMs = positionMs, createdAt = createdAt)
}
