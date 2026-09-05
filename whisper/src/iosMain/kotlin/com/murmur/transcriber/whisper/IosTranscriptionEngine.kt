package com.murmur.transcriber.whisper

actual fun createTranscriptionEngine(): TranscriptionEngine = IosTranscriptionEngine()

/**
 * cinterop binding over the same whisper.cpp C API used on Android (PRD section 09).
 * Not started — v1.0 targets Android only; this exists so the module compiles on a
 * macOS CI host from day one, per the PRD's cross-platform evidence goal.
 */
internal class IosTranscriptionEngine : TranscriptionEngine {
    override fun loadModel(modelPath: String): Boolean =
        throw NotImplementedError("iOS cinterop binding is not started — v1.0 is Android only")

    override fun transcribeWindow(pcm: FloatArray, sampleRate: Int, language: String?): List<EngineSegment> =
        throw NotImplementedError("iOS cinterop binding is not started — v1.0 is Android only")

    override fun release() = Unit
}
