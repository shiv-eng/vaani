package com.quietstack.vaani.shared.data

data class ModelCatalogueEntry(
    val id: String,
    val name: String,
    val variant: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * Q5_1 quantized "base" (56.9 MiB) — the spike's original validated pick.
 * Fast, but real-device testing showed it rendering Hindi speech in Urdu's
 * script even with language forced to "hi" (Hindi and Urdu are the same
 * spoken language with different scripts; small multilingual Whisper models
 * sometimes pick the wrong one). No longer the shipping default — see
 * [ModelCatalogue.DEFAULT].
 */
object ModelCatalogue {
    val BASE_Q5_1 = ModelCatalogueEntry(
        id = "base-q5_1",
        name = "base",
        variant = "multilingual",
        fileName = "ggml-base-q5_1.bin",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
        sizeBytes = 59_707_625L,
        sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
    )

    /**
     * Shipping default: Q5_1 quantized "small" (~181 MiB). "medium" and
     * "large-v3-turbo" were both tried as the default and rejected on real
     * on-device timing — "medium" ran ~4x real time, "large-v3-turbo" ~36x
     * (see [LARGE_V3_TURBO_Q5_0]'s note on why turbo's smaller decoder
     * doesn't help: the encoder, which does most of the work, stays
     * full-size). At that speed people stop using the app before it
     * finishes, which is a worse outcome than "small"'s lower accuracy on
     * hard cases. The specific failure that pushed the default up from
     * "small" in the first place — Hindi rendered in Urdu's script, and
     * Hindi/English code-switching collapsing to English-only — is a
     * multilingual-decoding problem, not something a bigger model reliably
     * fixes (accuracy did improve some with "medium", but not enough to
     * justify a 9-minute wait); it's addressed instead by defaulting
     * [com.quietstack.vaani.shared.data.AppSettings.transcriptionLanguage]
     * to English and telling users plainly that Hindi accuracy is still
     * limited (see WhisperLanguages.kt), rather than by throwing model size
     * at it. VaaniApplication preloads whichever model is downloaded at
     * process start so load time is paid once, before the user finishes
     * recording, not after they tap Transcribe.
     * Hosted at this exact path in whisper.cpp's own official Hugging Face
     * repo; size and sha256 confirmed against the file actually downloaded
     * from it (independently, on a host machine, not just on-device).
     */
    val SMALL_Q5_1 = ModelCatalogueEntry(
        id = "small-q5_1",
        name = "small",
        variant = "multilingual",
        fileName = "ggml-small-q5_1.bin",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
        sizeBytes = 190_085_487L,
        sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
    )

    /**
     * Q5_0 quantized "medium" (~514 MiB). Was the shipping default for a
     * while — meaningfully more accurate than "small" on hard cases like
     * Hindi script rendering and Hindi/English code-switching — but real
     * on-device timing measured ~4x real time (a 14s clip took 54s), which
     * on longer recordings is long enough that testing showed people
     * assuming the app was stuck. Reverted to [SMALL_Q5_1] as the default;
     * kept here for anyone who wants to pick it manually once a model
     * switcher exists. Size and sha256 confirmed against the file actually
     * downloaded from whisper.cpp's official Hugging Face repo
     * (independently, on a host machine, not just on-device).
     */
    val MEDIUM_Q5_0 = ModelCatalogueEntry(
        id = "medium-q5_0",
        name = "medium",
        variant = "multilingual",
        fileName = "ggml-medium-q5_0.bin",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
        sizeBytes = 539_212_467L,
        sha256 = "19fea4b380c3a618ec4723c3eef2eb785ffba0d0538cf43f8f235e7b3b34220f",
    )

    /**
     * Q5_0 quantized "large-v3-turbo" (~547 MiB). Tried as the default after
     * "medium" measured ~4x real time (54s for a 14s clip): told to
     * prioritize accuracy over speed, as long as the wait is communicated.
     * Reverted after actually timing it on the reference device: ~6 minutes
     * for a 10-second clip (~36x real time), roughly 9x slower than
     * "medium" — far past what "communicate the wait" can make tolerable
     * (a 2-minute recording would run 30+ minutes). The core reason: a
     * Whisper encoder always processes a fixed 30-second window internally
     * regardless of how much real speech is in it, so short clips don't get
     * proportionally cheaper — and large-v3-turbo only shrinks the decoder
     * (4 layers vs. large-v3's 32); it keeps large-v3's full-size encoder,
     * which is most of the cost here. "Turbo" is faster than full large-v3,
     * not faster than "medium" — a real, measured distinction, not just a
     * benchmark-marketing one. Kept in the catalogue in case a future
     * reference device profile can afford it; not the default.
     */
    val LARGE_V3_TURBO_Q5_0 = ModelCatalogueEntry(
        id = "large-v3-turbo-q5_0",
        name = "large-v3-turbo",
        variant = "multilingual",
        fileName = "ggml-large-v3-turbo-q5_0.bin",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
        sizeBytes = 574_041_195L,
        sha256 = "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
    )

    /**
     * INT8-quantized ONNX export of AI4Bharat's IndicConformer (Hybrid CTC +
     * RNNT), used only for Hindi — see [RoutingTranscriptionEngine] in the
     * `:whisper` module for the actual language-based dispatch. Whisper's
     * Hindi accuracy was fought over most of this project's life (script
     * confusion with Urdu, code-switching collapsing to English) without a
     * real fix; IndicConformer is a model AI4Bharat trained specifically for
     * Indian languages rather than a multilingual model that merely includes
     * them, and benchmarks meaningfully better on Hindi WER than even
     * Whisper large-v3 (~13.8 vs ~23.8), let alone the "small" model this app
     * actually ships. This conversion covers 8 Indic languages in one file
     * (Assamese, Bengali, Bodo, Gujarati, Hindi, Kannada, Kashmiri, Marathi)
     * — only Hindi is wired up for now since that's the language this app
     * actually had a problem with; routing another of these 8 languages to
     * it later is a small follow-up, not a new integration.
     * Chosen over an official-provenance but differently-packaged Hindi-only
     * export (OpenVoiceOS/ai4bharat-indicconformer-hi-onnx, converted
     * straight from AI4Bharat's own model) specifically because this one's
     * own description confirms it was exported for sherpa-onnx — the runtime
     * this app actually uses — rather than a different library
     * (`onnx-asr`) whose exported graph shape/conventions aren't guaranteed
     * to match sherpa-onnx's expectations. Size and sha256 confirmed against
     * the file actually downloaded from Hugging Face (independently, on a
     * host machine, not just on-device). Original model MIT-licensed by
     * AI4Bharat (IIT Madras); this ONNX conversion also published MIT.
     */
    val INDIC_CONFORMER_HI_MODEL = ModelCatalogueEntry(
        id = "indic-conformer-hi-model",
        name = "indic-conformer",
        variant = "hi",
        fileName = "indic-conformer.int8.onnx",
        url = "https://huggingface.co/meetsync/indic-conformer-onnx-sherpa/resolve/main/model.int8.onnx",
        sizeBytes = 196_977_855L,
        sha256 = "b99a01834cd1a72cd9be682a0b9543df6b152ef7dfceba88d3dbf59fbb77075d",
    )

    /** Token vocabulary for [INDIC_CONFORMER_HI_MODEL] — required alongside it, not optional. */
    val INDIC_CONFORMER_HI_TOKENS = ModelCatalogueEntry(
        id = "indic-conformer-hi-tokens",
        name = "indic-conformer-tokens",
        variant = "hi",
        fileName = "indic-conformer-tokens.txt",
        url = "https://huggingface.co/meetsync/indic-conformer-onnx-sherpa/resolve/main/tokens.txt",
        sizeBytes = 73_238L,
        sha256 = "743aeb755c4489bc734a6705578552b072ffb45055aeeb5db19ae7761a424882",
    )

    val DEFAULT = SMALL_Q5_1

    val all: List<ModelCatalogueEntry> =
        listOf(BASE_Q5_1, SMALL_Q5_1, MEDIUM_Q5_0, LARGE_V3_TURBO_Q5_0, INDIC_CONFORMER_HI_MODEL, INDIC_CONFORMER_HI_TOKENS)

    /**
     * Everything onboarding downloads upfront, not just [DEFAULT] — the user
     * explicitly chose to pay for Hindi support during initial setup rather
     * than deferring it to first Hindi use, so Hindi works immediately with
     * no second wait later.
     */
    val REQUIRED: List<ModelCatalogueEntry> = listOf(DEFAULT, INDIC_CONFORMER_HI_MODEL, INDIC_CONFORMER_HI_TOKENS)

    fun byId(id: String): ModelCatalogueEntry? = all.firstOrNull { it.id == id }
}
