package com.quietstack.vaani.shared.domain.pipeline

/**
 * Splits [totalDurationMs] into fixed windows with a short overlap, so the encoder
 * never sees more than [windowLengthMs] at once and words are not cut at a boundary.
 * This is not an optimisation — whisper.cpp's encoder works on fixed-length windows,
 * so windowing is how the model works (PRD section 10).
 *
 * D-04 leaves windowLengthMs and overlapMs as tuning knobs settled by measurement
 * during the spike; the defaults here are the starting assumption only.
 */
class WindowPlanner(
    private val windowLengthMs: Long = 30_000,
    private val overlapMs: Long = 3_000,
) {
    init {
        require(windowLengthMs > 0) { "windowLengthMs must be positive" }
        require(overlapMs in 0 until windowLengthMs) { "overlapMs must be smaller than windowLengthMs" }
    }

    fun plan(totalDurationMs: Long): List<AudioWindow> {
        if (totalDurationMs <= 0) return emptyList()

        val stride = windowLengthMs - overlapMs
        val windows = mutableListOf<AudioWindow>()
        var start = 0L
        var index = 0
        while (start < totalDurationMs) {
            val end = minOf(start + windowLengthMs, totalDurationMs)
            windows += AudioWindow(index = index, startMs = start, endMs = end)
            if (end == totalDurationMs) break
            start += stride
            index++
        }
        return windows
    }
}

data class AudioWindow(val index: Int, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}
