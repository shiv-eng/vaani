package com.quietstack.vaani.shared.domain.pipeline

/** Decodes one window of a source file to 16 kHz mono float PCM, whisper's expected input. */
interface AudioDecoder {
    suspend fun decodeWindow(filePath: String, window: AudioWindow): FloatArray
}

expect fun createAudioDecoder(): AudioDecoder
