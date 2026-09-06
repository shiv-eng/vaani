package com.murmur.transcriber.whisper

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Spike-only: proves whisper.cpp actually runs through the JNI boundary on a
 * device/emulator, end to end. Not a permanent test — it bypasses AudioDecoder
 * and recording (still [NotImplementedError] stubs) by reading a bundled 16kHz
 * mono WAV directly, and it expects the base model to already be pushed to
 * /data/local/tmp by the spike script (see PRD section 14). Delete this once
 * the real pipeline exists and there's something more meaningful to test.
 */
@RunWith(AndroidJUnit4::class)
class WhisperEngineSpikeTest {

    @Test
    fun transcribesTheBundledSampleOnTheReferenceModel() {
        val modelFile = File("/data/local/tmp/ggml-base.bin")
        assertTrue("Push the model first: adb push ggml-base.bin /data/local/tmp/", modelFile.exists())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pcm = context.assets.open("jfk.wav").use { readWavAsFloatPcm(it.readBytes()) }

        val engine = createTranscriptionEngine()
        val loaded = engine.loadModel(modelFile.absolutePath)
        assertTrue("Model failed to load from ${modelFile.absolutePath}", loaded)

        try {
            val start = System.currentTimeMillis()
            val segments = engine.transcribeWindow(pcm, sampleRate = 16_000, language = "en")
            val elapsedMs = System.currentTimeMillis() - start

            val audioDurationMs = pcm.size * 1000L / 16_000
            val realTimeFactor = elapsedMs.toDouble() / audioDurationMs

            val fullText = segments.joinToString(" ") { it.text.trim() }
            android.util.Log.i(
                "WhisperSpike",
                "audio=${audioDurationMs}ms wall=${elapsedMs}ms rtf=%.2f segments=%d text=\"%s\"".format(
                    realTimeFactor, segments.size, fullText,
                ),
            )

            assertFalse("Expected at least one segment", segments.isEmpty())
            assertTrue(
                "Expected recognisable speech in the JFK sample, got: \"$fullText\"",
                fullText.contains("country", ignoreCase = true) ||
                    fullText.contains("ask not", ignoreCase = true),
            )
        } finally {
            engine.release()
        }
    }

    /** Minimal 16-bit PCM WAV reader — good enough for a fixed, known-good test fixture. */
    private fun readWavAsFloatPcm(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var dataOffset = -1
        var dataSize = 0
        buffer.position(12) // skip RIFF header
        while (buffer.remaining() >= 8) {
            val chunkStart = buffer.position()
            val chunkId = ByteArray(4).also { buffer.get(it) }
            val chunkSize = buffer.int
            if (chunkId.decodeToString() == "data") {
                dataOffset = buffer.position()
                dataSize = chunkSize
                break
            }
            buffer.position(chunkStart + 8 + chunkSize + (chunkSize % 2))
        }
        check(dataOffset >= 0) { "No data chunk found in WAV" }

        val samples = FloatArray(dataSize / 2)
        buffer.position(dataOffset)
        for (i in samples.indices) {
            samples[i] = buffer.short / 32768f
        }
        return samples
    }
}
