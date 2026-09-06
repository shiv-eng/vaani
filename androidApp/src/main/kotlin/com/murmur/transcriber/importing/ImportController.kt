package com.murmur.transcriber.importing

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportedFileInfo(
    val filePath: String,
    val displayFileName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val sampleRate: Int,
    val extension: String,
)

/**
 * Copies a `content://` Uri into app-private storage (FR-04 — so a revoked
 * folder permission can't orphan it later) and reads real duration/size/format
 * before anything is committed to the library (FR-03).
 */
class ImportController(private val context: Context) {

    suspend fun stageImport(uri: Uri): ImportedFileInfo = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "audio/mp4"
        val originalName = queryDisplayName(uri) ?: "import"
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = extensionFor(mimeType))

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$extension")

        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $uri")

        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
        val sampleRate = try {
            probeSampleRate(file.absolutePath)
        } catch (t: Throwable) {
            44_100
        }

        ImportedFileInfo(
            filePath = file.absolutePath,
            displayFileName = originalName,
            durationMs = durationMs,
            sizeBytes = file.length(),
            mimeType = mimeType,
            sampleRate = sampleRate,
            extension = extension,
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun probeSampleRate(path: String): Int {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(path)
            val format = extractor.getTrackFormat(0)
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } finally {
            extractor.release()
        }
    }

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/ogg" -> "ogg"
        "audio/3gpp" -> "3gp"
        else -> "m4a"
    }
}
