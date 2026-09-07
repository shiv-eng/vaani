package com.quietstack.vaani.shared.domain.repository

import com.quietstack.vaani.shared.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeForRecording(recordingId: String): Flow<List<Bookmark>>
    suspend fun add(recordingId: String, positionMs: Long): Bookmark
    suspend fun delete(id: String)
}
