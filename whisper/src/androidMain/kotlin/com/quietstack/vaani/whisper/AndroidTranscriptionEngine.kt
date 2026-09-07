package com.quietstack.vaani.whisper

import android.util.Log

actual fun createTranscriptionEngine(): TranscriptionEngine = AndroidTranscriptionEngine()

private const val TAG = "AndroidTranscriptionEngine"

/**
 * JNI binding over whisper.cpp, built via the NDK and externalNativeBuild
 * (PRD section 09, module map).
 *
 * Loading a ~57 MB model into whisper.cpp costs far more wall time than
 * actually transcribing a short recording — real device testing showed a
 * single load dominating the total time for an 18s clip. Registered as a
 * Koin `single` (see WhisperModule) so this instance, and its loaded
 * model, survive across transcription jobs in the same app process:
 * [loadModel] is a no-op when the requested path is already loaded.
 *
 * whisper.cpp's context is not thread-safe, and this engine now has two
 * independent callers that can overlap in time: VaaniApplication's
 * startup preload and a transcription job's own [loadModel] call. Without
 * locking, real-device testing showed those two racing — both calling
 * nativeLoadModel concurrently — and produced corrupted, hallucinated
 * all-caps output instead of a crash, which is worse than a crash because
 * it looks like a plausible transcript. Every native entry point is
 * synchronized on this instance to serialize them.
 */
internal class AndroidTranscriptionEngine : TranscriptionEngine {

    private var nativeHandle: Long = 0L
    private var loadedModelPath: String? = null

    override fun loadModel(modelPath: String): Boolean = synchronized(this) {
        if (nativeHandle != 0L && loadedModelPath == modelPath) {
            Log.i(TAG, "loadModel: already loaded, skipping native reload ($modelPath)")
            return@synchronized true
        }
        Log.i(TAG, "loadModel: calling nativeLoadModel for $modelPath (previous=$loadedModelPath)")
        val start = System.currentTimeMillis()
        releaseLocked()
        nativeHandle = nativeLoadModel(modelPath)
        loadedModelPath = if (nativeHandle != 0L) modelPath else null
        Log.i(TAG, "loadModel: nativeLoadModel finished in ${System.currentTimeMillis() - start}ms, success=${nativeHandle != 0L}")
        nativeHandle != 0L
    }

    override fun isModelLoaded(modelPath: String): Boolean = synchronized(this) {
        nativeHandle != 0L && loadedModelPath == modelPath
    }

    override fun transcribeWindow(
        pcm: FloatArray,
        sampleRate: Int,
        language: String?,
        onProgress: (Float) -> Unit,
    ): List<EngineSegment> = synchronized(this) {
        check(nativeHandle != 0L) { "Model not loaded" }
        val start = System.currentTimeMillis()
        val result = nativeTranscribe(nativeHandle, pcm, sampleRate, language, ProgressCallback(onProgress)).toList()
        Log.i(TAG, "transcribeWindow: ${pcm.size} samples decoded in ${System.currentTimeMillis() - start}ms")
        result
    }

    /**
     * Passed to native code as a plain jobject; whisper_jni.cpp looks up
     * [onProgress] by name via JNI and invokes it from whisper.cpp's
     * progress_callback, which fires on the same thread that called
     * nativeTranscribe (whisper_full's main loop, not a worker thread), so
     * the JNIEnv captured for that call stays valid for the whole callback.
     */
    private class ProgressCallback(private val callback: (Float) -> Unit) {
        @Suppress("unused") // invoked from whisper_jni.cpp via JNI
        fun onProgress(percent: Int) {
            callback(percent / 100f)
        }
    }

    /**
     * Frees the native context. StartTranscriptionUseCase deliberately does not
     * call this after every job anymore (see class doc) — real callers are
     * [loadModel] switching to a different model, and process teardown.
     */
    override fun release(): Unit = synchronized(this) { releaseLocked() }

    private fun releaseLocked() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0L
            loadedModelPath = null
        }
    }

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeTranscribe(
        handle: Long,
        pcm: FloatArray,
        sampleRate: Int,
        language: String?,
        progressCallback: ProgressCallback,
    ): Array<EngineSegment>
    private external fun nativeRelease(handle: Long)

    private companion object {
        init {
            System.loadLibrary("whisper_jni")
        }
    }
}
