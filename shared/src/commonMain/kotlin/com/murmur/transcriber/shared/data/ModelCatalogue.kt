package com.murmur.transcriber.shared.data

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
 * Ships one model: Q5_1 quantized "base", the spike's validated shipping
 * default (56.9 MiB, ~2x the float model's speed, no observed accuracy
 * loss — spike/RESULTS.md). Hosted at this exact path in whisper.cpp's own
 * official Hugging Face repo; size and sha256 confirmed against the file
 * actually downloaded from it.
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

    val all: List<ModelCatalogueEntry> = listOf(BASE_Q5_1)

    fun byId(id: String): ModelCatalogueEntry? = all.firstOrNull { it.id == id }
}
