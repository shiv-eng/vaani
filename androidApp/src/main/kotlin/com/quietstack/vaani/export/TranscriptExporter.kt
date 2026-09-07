package com.quietstack.vaani.export

import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.ui.format.formatMmSs

/**
 * Builds the exportable text for a transcript. Always reads a segment's
 * edited text when present, the model's original otherwise (FR-17/FR-18) —
 * [Segment.displayText] already encodes that rule.
 */
object TranscriptExporter {
    fun plainText(segments: List<Segment>): String =
        segments.joinToString("\n\n") { it.displayText.trim() }

    fun withTimestamps(segments: List<Segment>): String =
        segments.joinToString("\n") { "${formatMmSs(it.startMs)}  ${it.displayText.trim()}" }

    /** SubRip (.srt) — the standard subtitle format, so a transcript can be dropped straight into a video editor. */
    fun srt(segments: List<Segment>): String =
        segments.mapIndexed { index, segment ->
            "${index + 1}\n${srtTimestamp(segment.startMs)} --> ${srtTimestamp(segment.endMs)}\n${segment.displayText.trim()}"
        }.joinToString("\n\n")

    private fun srtTimestamp(ms: Long): String {
        val hours = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val millis = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
