package com.quietstack.vaani.whisper

import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig

private const val TAG = "IndicConformerEngine"

/**
 * Hindi-only ASR via AI4Bharat's IndicConformer (Hybrid CTC + RNNT, ONNX
 * export for sherpa-onnx — see ModelCatalogue.INDIC_CONFORMER_HI_MODEL for
 * why this model and this specific conversion). Whisper's own Hindi
 * accuracy was fought over most of this project's life without a real fix
 * (script confusion with Urdu, Hindi/English code-switching collapsing to
 * English); this exists specifically so Hindi has its own model trained for
 * it rather than inheriting a general multilingual model's weaknesses —
 * see RoutingTranscriptionEngine for where "hi" gets sent here instead of
 * to Whisper.
 *
 * Deliberately NOT a [TranscriptionEngine] itself: that interface's
 * `loadModel(modelPath: String)` takes one path, but this model needs two
 * files (the ONNX graph and its token vocabulary) — RoutingTranscriptionEngine
 * owns resolving both paths and adapts this class's two-argument load to
 * the shared interface's single-argument one.
 *
 * sherpa-onnx's offline (non-streaming) decode returns one flat transcript
 * per call, not per-sentence segments the way whisper.cpp's decoder does —
 * so unlike Whisper, a whole window comes back as a single EngineSegment
 * spanning it, not several. Getting correct Hindi text was the actual
 * problem; splitting it into finer segments is a reasonable follow-up, not
 * a blocker for this to be useful.
 */
internal class IndicConformerEngine {

    private var recognizer: OfflineRecognizer? = null
    private var loadedModelPath: String? = null

    @Synchronized
    fun isLoaded(modelPath: String): Boolean = recognizer != null && loadedModelPath == modelPath

    @Synchronized
    fun loadModel(modelPath: String, tokensPath: String): Boolean {
        if (isLoaded(modelPath)) {
            Log.i(TAG, "loadModel: already loaded, skipping ($modelPath)")
            return true
        }
        Log.i(TAG, "loadModel: loading $modelPath (tokens=$tokensPath)")
        val start = System.currentTimeMillis()
        releaseLocked()
        return try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80, dither = 0f),
                modelConfig = OfflineModelConfig(
                    nemo = OfflineNemoEncDecCtcModelConfig(model = modelPath),
                    tokens = tokensPath,
                    numThreads = 2,
                    provider = "cpu",
                ),
                decodingMethod = "greedy_search",
            )
            recognizer = OfflineRecognizer(config = config)
            loadedModelPath = modelPath
            Log.i(TAG, "loadModel: finished in ${System.currentTimeMillis() - start}ms")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "loadModel: failed", t)
            recognizer = null
            loadedModelPath = null
            false
        }
    }

    @Synchronized
    fun transcribeWindow(pcm: FloatArray, sampleRate: Int): List<EngineSegment> {
        val engine = checkNotNull(recognizer) { "IndicConformer model not loaded" }
        val start = System.currentTimeMillis()
        val stream = engine.createStream()
        val text = try {
            stream.acceptWaveform(pcm, sampleRate)
            engine.decode(stream)
            engine.getResult(stream).text
        } finally {
            stream.release()
        }
        Log.i(TAG, "transcribeWindow: ${pcm.size} samples decoded in ${System.currentTimeMillis() - start}ms")
        val durationMs = (pcm.size.toLong() * 1000L) / sampleRate
        return if (text.isBlank()) emptyList() else listOf(EngineSegment(startMs = 0L, endMs = durationMs, text = text.trim()))
    }

    @Synchronized
    fun release() = releaseLocked()

    private fun releaseLocked() {
        recognizer?.release()
        recognizer = null
        loadedModelPath = null
    }

    private companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
