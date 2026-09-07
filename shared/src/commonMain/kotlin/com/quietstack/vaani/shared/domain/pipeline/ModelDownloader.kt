package com.quietstack.vaani.shared.domain.pipeline

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

/**
 * Streams [url] to [targetPath], resuming an interrupted download from
 * where it left off, and verifies [expectedSha256] before the file is
 * considered real (FR-16). Emits progress in [0f, 1f].
 */
interface ModelDownloader {
    fun download(url: String, targetPath: String, expectedSha256: String): Flow<Float>
}

expect fun createModelDownloader(httpClient: HttpClient): ModelDownloader
