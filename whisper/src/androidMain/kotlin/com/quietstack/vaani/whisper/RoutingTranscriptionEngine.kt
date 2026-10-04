package com.quietstack.vaani.whisper

/**
 * Sends Hindi to [IndicConformerEngine] and everything else to the wrapped
 * Whisper engine — the only language-aware decision in the whole
 * transcription pipeline, kept here so nothing upstream (StartTranscriptionUseCase,
 * TranscriptionForegroundService) needs to know a second engine exists at all.
 *
 * [indicModelPath]/[indicTokensPath] are resolved once at construction (see
 * platformModule's Android actual) from the same app-private "models/"
 * directory ModelRepository already downloads into — this class doesn't
 * know or care how they got there, only that IndicConformerEngine.loadModel
 * will fail cleanly if they're missing (e.g. onboarding was skipped or
 * interrupted before the Hindi files landed), in which case Hindi silently
 * falls back to Whisper rather than crashing a transcription job.
 */
internal class RoutingTranscriptionEngine(
    private val whisper: TranscriptionEngine,
    private val indicConformer: IndicConformerEngine,
    private val indicModelPath: String,
    private val indicTokensPath: String,
) : TranscriptionEngine {

    override fun loadModel(modelPath: String): Boolean = whisper.loadModel(modelPath)

    override fun isModelLoaded(modelPath: String): Boolean = whisper.isModelLoaded(modelPath)

    override fun transcribeWindow(
        pcm: FloatArray,
        sampleRate: Int,
        language: String?,
        onProgress: (Float) -> Unit,
    ): List<EngineSegment> {
        if (language == "hi" && ensureIndicLoaded()) {
            onProgress(0f)
            val result = indicConformer.transcribeWindow(pcm, sampleRate)
            onProgress(1f)
            return result
        }
        return whisper.transcribeWindow(pcm, sampleRate, language, onProgress)
    }

    private fun ensureIndicLoaded(): Boolean =
        indicConformer.isLoaded(indicModelPath) || indicConformer.loadModel(indicModelPath, indicTokensPath)

    override fun release() {
        whisper.release()
        indicConformer.release()
    }
}
