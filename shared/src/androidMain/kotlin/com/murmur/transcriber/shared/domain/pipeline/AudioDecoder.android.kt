package com.murmur.transcriber.shared.domain.pipeline

actual fun createAudioDecoder(): AudioDecoder = AndroidAudioDecoder()

/**
 * TODO: stream-decode via MediaExtractor + MediaCodec to 16 kHz mono float PCM,
 * seeking to [AudioWindow.startMs] and reading through [AudioWindow.endMs]
 * (PRD section 09, "The transcription pipeline"). Left unimplemented until the
 * spike validates the runtime on the reference device (PRD section 14).
 */
internal class AndroidAudioDecoder : AudioDecoder {
    override suspend fun decodeWindow(filePath: String, window: AudioWindow): FloatArray {
        throw NotImplementedError("Audio decode lands with the whisper.cpp spike")
    }
}
