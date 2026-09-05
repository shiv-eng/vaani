package com.murmur.transcriber.shared.domain.pipeline

actual fun createAudioDecoder(): AudioDecoder = IosAudioDecoder()

/** TODO: decode via AVFoundation once v1.0 (Android only) ships. Not started. */
internal class IosAudioDecoder : AudioDecoder {
    override suspend fun decodeWindow(filePath: String, window: AudioWindow): FloatArray {
        throw NotImplementedError("iOS audio decode is not started — v1.0 is Android only")
    }
}
