package com.murmur.transcriber.shared.domain.pipeline

import io.ktor.client.HttpClient

actual fun createModelDownloader(httpClient: HttpClient): ModelDownloader = IosModelDownloader()

/** TODO: NSURLSession-backed download once v1.0 (Android only) ships. Not started. */
internal class IosModelDownloader : ModelDownloader {
    override fun download(url: String, targetPath: String, expectedSha256: String) =
        throw NotImplementedError("iOS model download is not started — v1.0 is Android only")
}
