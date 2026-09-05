package com.murmur.transcriber.shared.domain.model

/** One library item: a recorded or imported audio file. See PRD section 08. */
data class Recording(
    val id: String,
    val displayName: String,
    val fileName: String,
    val createdAt: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val source: RecordingSource,
    val mimeType: String,
    val sampleRate: Int,
    val transcriptionState: TranscriptionState,
)

enum class RecordingSource { RECORDED, IMPORTED }

enum class TranscriptionState { NONE, QUEUED, RUNNING, PAUSED, COMPLETE, FAILED, CANCELLED }
