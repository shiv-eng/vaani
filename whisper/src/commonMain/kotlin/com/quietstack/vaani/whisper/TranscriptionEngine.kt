package com.quietstack.vaani.whisper

/**
 * The only surface shared code sees of the native runtime: load a model,
 * transcribe a float array, release. Nothing about ggml, JNI or whisper.cpp's
 * file layout leaks past this interface (PRD section 09, "Boundary discipline"),
 * so swapping the runtime later stays an experiment rather than a rewrite.
 */
interface TranscriptionEngine {
    /** Loads weights from an app-private file path. Returns false if the file is missing or corrupt. */
    fun loadModel(modelPath: String): Boolean

    /**
     * True if [modelPath] is already the loaded model — i.e. a call to
     * [loadModel] with this path would be a no-op rather than a real,
     * multi-second native load. Lets a caller report an honest "loading" vs
     * "transcribing" state up front instead of a caller inferring it from
     * decode progress sitting at 0%, which is also what real 0% progress
     * looks like once decode has actually started.
     */
    fun isModelLoaded(modelPath: String): Boolean

    /**
     * [pcm] is one window of 16 kHz mono float PCM. Timestamps are relative to the window, not the file.
     * [onProgress] reports 0f..1f progress through decoding *this window* — a single window can be the
     * whole recording (see WindowPlanner), so without this callers have no progress signal until the
     * entire window is done, which reads as "stuck at 0%, then a sudden jump" for anything under ~30s.
     */
    fun transcribeWindow(
        pcm: FloatArray,
        sampleRate: Int,
        language: String?,
        onProgress: (Float) -> Unit = {},
    ): List<EngineSegment>

    /** Frees the native model context. Safe to call even if no model was loaded. */
    fun release()
}

data class EngineSegment(val startMs: Long, val endMs: Long, val text: String)

expect fun createTranscriptionEngine(): TranscriptionEngine
