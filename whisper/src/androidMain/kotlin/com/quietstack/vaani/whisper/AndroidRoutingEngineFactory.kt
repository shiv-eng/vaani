package com.quietstack.vaani.whisper

/**
 * The one public seam between this module's Hindi/English routing (see
 * RoutingTranscriptionEngine) and the shared module's Koin wiring, which
 * needs to construct this with real, resolved file paths (see
 * platformModule's Android actual in :shared) but has no business
 * constructing RoutingTranscriptionEngine or IndicConformerEngine directly —
 * both stay internal to this module.
 */
fun createAndroidRoutingTranscriptionEngine(indicModelPath: String, indicTokensPath: String): TranscriptionEngine =
    RoutingTranscriptionEngine(
        whisper = createTranscriptionEngine(),
        indicConformer = IndicConformerEngine(),
        indicModelPath = indicModelPath,
        indicTokensPath = indicTokensPath,
    )
