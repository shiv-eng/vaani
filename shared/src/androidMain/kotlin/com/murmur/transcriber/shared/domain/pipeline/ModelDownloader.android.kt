package com.murmur.transcriber.shared.domain.pipeline

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

actual fun createModelDownloader(httpClient: HttpClient): ModelDownloader = AndroidModelDownloader(httpClient)

internal class AndroidModelDownloader(private val httpClient: HttpClient) : ModelDownloader {

    override fun download(url: String, targetPath: String, expectedSha256: String): Flow<Float> = flow {
        val targetFile = File(targetPath)
        targetFile.parentFile?.mkdirs()
        val partFile = File("$targetPath.part")
        val resumeFromBytes = if (partFile.exists()) partFile.length() else 0L

        httpClient.prepareGet(url) {
            if (resumeFromBytes > 0) header(HttpHeaders.Range, "bytes=$resumeFromBytes-")
        }.execute { response: HttpResponse ->
            check(response.status.isSuccess()) {
                "Download failed: HTTP ${response.status.value} from $url"
            }

            val remainingBytes = response.contentLength() ?: 0L
            val totalBytes = (resumeFromBytes + remainingBytes).coerceAtLeast(1L)
            val channel = response.bodyAsChannel()
            var bytesWritten = resumeFromBytes

            FileOutputStream(partFile, resumeFromBytes > 0).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = channel.readAvailable(buffer)
                    if (read < 0) break // channel exhausted — the file is fully received
                    if (read == 0) continue // no bytes ready yet; readAvailable will suspend next call
                    output.write(buffer, 0, read)
                    bytesWritten += read
                    emit((bytesWritten.toFloat() / totalBytes).coerceIn(0f, 0.999f))
                }
            }
        }

        val actualSha256 = sha256Of(partFile)
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            partFile.delete()
            error("Checksum mismatch for $url: expected $expectedSha256, got $actualSha256")
        }

        partFile.renameTo(targetFile)
        emit(1f)
    }.flowOn(Dispatchers.IO)

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) {
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
