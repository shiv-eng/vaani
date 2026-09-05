package com.murmur.transcriber.whisper

/**
 * The only surface shared code sees of the native runtime: load a model,
 * transcribe a float array, release. Nothing about ggml, JNI or whisper.cpp's
 * file layout leaks past this interface (PRD section 09, "Boundary discipline"),
 * so swapping the runtime later stays an experiment rather than a rewrite.
 */
interface TranscriptionEngine {
    /** Loads weights from an app-private file path. Returns false if the file is missing or corrupt. */
    fun loadModel(modelPath: String): Boolean

    /** [pcm] is one window of 16 kHz mono float PCM. Timestamps are relative to the window, not the file. */
    fun transcribeWindow(pcm: FloatArray, sampleRate: Int, language: String?): List<EngineSegment>

    /** Frees the native model context. Safe to call even if no model was loaded. */
    fun release()
}

data class EngineSegment(val startMs: Long, val endMs: Long, val text: String)

expect fun createTranscriptionEngine(): TranscriptionEngine
