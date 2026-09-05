package com.murmur.transcriber.shared.domain.model

/**
 * One transcribed span of audio, written as its window completes.
 * [editedText] is null until the user changes something, so the model's
 * original output is never lost (PRD section 08).
 */
data class Segment(
    val id: String,
    val recordingId: String,
    val windowIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val editedText: String?,
) {
    val displayText: String get() = editedText ?: text
}
