package com.quietstack.vaani.shared.domain.model

/**
 * One transcription attempt on a recording. Progress is derived from
 * windowsDone / windowsTotal, which is what makes resume trivial after
 * process death or cancellation (FR-12).
 */
data class TranscriptionJob(
    val id: String,
    val recordingId: String,
    val modelId: String,
    val language: String?,
    val status: TranscriptionState,
    val windowsTotal: Int,
    val windowsDone: Int,
    val startedAt: Long,
    val finishedAt: Long?,
    val error: String?,
) {
    val progress: Float
        get() = if (windowsTotal == 0) 0f else windowsDone.toFloat() / windowsTotal
}
