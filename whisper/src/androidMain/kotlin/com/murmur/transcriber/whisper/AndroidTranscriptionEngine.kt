package com.murmur.transcriber.whisper

actual fun createTranscriptionEngine(): TranscriptionEngine = AndroidTranscriptionEngine()

/**
 * JNI binding over whisper.cpp, built via the NDK and externalNativeBuild
 * (PRD section 09, module map). The native library and its CMake wiring are
 * added during the spike (PRD section 14) — until then, [System.loadLibrary]
 * fails fast with UnsatisfiedLinkError rather than pretending to work.
 */
internal class AndroidTranscriptionEngine : TranscriptionEngine {

    private var nativeHandle: Long = 0L

    override fun loadModel(modelPath: String): Boolean {
        nativeHandle = nativeLoadModel(modelPath)
        return nativeHandle != 0L
    }

    override fun transcribeWindow(pcm: FloatArray, sampleRate: Int, language: String?): List<EngineSegment> {
        check(nativeHandle != 0L) { "Model not loaded" }
        return nativeTranscribe(nativeHandle, pcm, sampleRate, language).toList()
    }

    override fun release() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeTranscribe(
        handle: Long,
        pcm: FloatArray,
        sampleRate: Int,
        language: String?,
    ): Array<EngineSegment>
    private external fun nativeRelease(handle: Long)

    private companion object {
        init {
            System.loadLibrary("whisper_jni")
        }
    }
}
