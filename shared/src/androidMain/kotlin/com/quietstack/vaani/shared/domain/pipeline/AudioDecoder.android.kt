package com.quietstack.vaani.shared.domain.pipeline

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createAudioDecoder(): AudioDecoder = AndroidAudioDecoder()

private const val TARGET_SAMPLE_RATE = 16_000
private const val BUFFER_TIMEOUT_US = 10_000L

/**
 * MediaExtractor seeks to the window's start and selects the audio track;
 * MediaCodec decodes to 16-bit PCM through the window's end. Multi-channel
 * output is downmixed to mono by averaging channels, then linearly
 * resampled to whisper's expected 16 kHz — adequate for speech and avoids
 * a resampling library dependency (PRD section 09).
 */
internal class AndroidAudioDecoder : AudioDecoder {

    private class DecodeResult(val pcm: ByteArray, val firstPresentationUs: Long)

    override suspend fun decodeWindow(filePath: String, window: AudioWindow): FloatArray =
        withContext(Dispatchers.Default) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(filePath)
                val trackIndex = selectAudioTrack(extractor)
                check(trackIndex >= 0) { "No audio track in $filePath" }
                extractor.selectTrack(trackIndex)
                val format = extractor.getTrackFormat(trackIndex)

                val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME)) { "Missing MIME for $filePath" }

                extractor.seekTo(window.startMs * 1_000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val result = decodeToRawPcm(extractor, format, mime, window, sourceSampleRate, channelCount)
                val mono = toMonoFloat(result.pcm, channelCount)
                val trimmed = trimToWindow(mono, sourceSampleRate, window, result.firstPresentationUs)
                resampleLinear(trimmed, sourceSampleRate, TARGET_SAMPLE_RATE)
            } finally {
                extractor.release()
            }
        }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun decodeToRawPcm(
        extractor: MediaExtractor,
        format: MediaFormat,
        mime: String,
        window: AudioWindow,
        sourceSampleRate: Int,
        channelCount: Int,
    ): DecodeResult {
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val pcmOut = ByteArrayOutputStream()
        var sawInputEOS = false
        var sawOutputEOS = false
        var firstPresentationUs = -1L
        var targetTotalSourceSamples = Long.MAX_VALUE
        var totalSourceSamplesSoFar = 0L

        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(BUFFER_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, BUFFER_TIMEOUT_US)
                if (outputIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        if (firstPresentationUs < 0) {
                            firstPresentationUs = bufferInfo.presentationTimeUs
                            val frontTrimSec = (window.startMs * 1_000L - firstPresentationUs) / 1_000_000.0
                            val frontTrimSamples = (frontTrimSec * sourceSampleRate).roundToInt().coerceAtLeast(0)
                            val windowSamples = (window.durationMs / 1000.0 * sourceSampleRate).roundToInt()
                            targetTotalSourceSamples = (frontTrimSamples + windowSamples + sourceSampleRate / 10).toLong()
                        }
                        val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(chunk)
                        pcmOut.write(chunk)
                        totalSourceSamplesSoFar += bufferInfo.size / 2 / channelCount
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 ||
                        totalSourceSamplesSoFar >= targetTotalSourceSamples
                    ) {
                        sawOutputEOS = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }

        return DecodeResult(pcmOut.toByteArray(), firstPresentationUs)
    }

    /** 16-bit signed little-endian PCM, interleaved by [channelCount], averaged down to mono in [-1, 1]. */
    private fun toMonoFloat(raw: ByteArray, channelCount: Int): FloatArray {
        val frameCount = raw.size / 2 / channelCount
        val mono = FloatArray(frameCount)
        var byteIndex = 0
        for (frame in 0 until frameCount) {
            var sum = 0
            repeat(channelCount) {
                val lo = raw[byteIndex].toInt() and 0xFF
                val hi = raw[byteIndex + 1].toInt()
                sum += (hi shl 8) or lo
                byteIndex += 2
            }
            mono[frame] = (sum / channelCount) / 32768f
        }
        return mono
    }

    private fun trimToWindow(mono: FloatArray, sourceSampleRate: Int, window: AudioWindow, firstPresentationUs: Long): FloatArray {
        if (firstPresentationUs < 0) return mono
        val frontTrimSec = (window.startMs * 1_000L - firstPresentationUs) / 1_000_000.0
        val frontTrimSamples = (frontTrimSec * sourceSampleRate).roundToInt().coerceIn(0, mono.size)
        val windowSamples = (window.durationMs / 1000.0 * sourceSampleRate).roundToInt()
        val end = (frontTrimSamples + windowSamples).coerceIn(frontTrimSamples, mono.size)
        return mono.copyOfRange(frontTrimSamples, end)
    }

    private fun resampleLinear(input: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
        if (sourceRate == targetRate || input.isEmpty()) return input
        val ratio = targetRate.toDouble() / sourceRate
        val outputLength = (input.size * ratio).roundToInt()
        val output = FloatArray(outputLength)
        for (i in output.indices) {
            val srcPos = i / ratio
            val srcIndex = srcPos.toInt()
            val frac = (srcPos - srcIndex).toFloat()
            val s0 = input[srcIndex.coerceAtMost(input.size - 1)]
            val s1 = input[(srcIndex + 1).coerceAtMost(input.size - 1)]
            output[i] = s0 + (s1 - s0) * frac
        }
        return output
    }
}
