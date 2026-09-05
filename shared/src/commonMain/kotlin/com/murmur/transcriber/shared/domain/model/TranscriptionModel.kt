package com.murmur.transcriber.shared.domain.model

/** A downloaded whisper.cpp model on device. Supports more than one at a time. */
data class TranscriptionModel(
    val id: String,
    val name: String,
    val variant: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadedAt: Long,
)
