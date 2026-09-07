package com.quietstack.vaani.shared.domain.model

/** A marked moment in a recording, for jumping straight back to it during playback. */
data class Bookmark(
    val id: String,
    val recordingId: String,
    val positionMs: Long,
    val createdAt: Long,
)
