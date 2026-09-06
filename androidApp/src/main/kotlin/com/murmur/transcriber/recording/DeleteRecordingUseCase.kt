package com.murmur.transcriber.recording

import android.content.Context
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import java.io.File

/**
 * Deletes a recording's audio file alongside its database rows. Mirrors
 * FinalizeRecordingUseCase/ImportController's Context-aware path resolution
 * (:shared holds no filesDir) — RecordingRepository.delete alone only clears
 * rows, which would otherwise leak the audio file on every deletion.
 */
class DeleteRecordingUseCase(
    private val context: Context,
    private val recordingRepository: RecordingRepository,
) {
    suspend operator fun invoke(recordingId: String) {
        val fileName = recordingRepository.getById(recordingId)?.fileName
        recordingRepository.delete(recordingId)
        if (fileName != null) {
            File(context.filesDir, "recordings/$fileName").delete()
        }
    }
}
