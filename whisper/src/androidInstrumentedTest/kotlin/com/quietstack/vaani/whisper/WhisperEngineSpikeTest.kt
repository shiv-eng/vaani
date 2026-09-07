package com.quietstack.vaani.whisper

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
 * Exercises whisper.cpp through the real JNI boundary on a device/emulator —
 * the layer where a real production crash was found and fixed this session
 * (env->NewStringUTF aborting the process on a segment whose UTF-8 didn't
 * satisfy Java's stricter "Modified UTF-8", fixed by decoding to UTF-16 and
 * using env->NewString instead — see whisper_jni.cpp). Reads a bundled 16kHz
 * mono WAV directly rather than going through the app's recording/import
 * pipeline, and expects the base model to already be pushed to
 * /data/local/tmp (see PRD section 14) — instrumented tests can't download
 * one themselves.
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
