package com.quietstack.vaani.ui.format

import kotlin.math.roundToInt

/** "9h 04" — the Home screen's captured-hours stat. */
fun formatHoursCompact(ms: Long): Pair<String, String> {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return hours.toString() to minutes.toString().padStart(2, '0')
}

/** "1h 52m", "48m 12s", "3m 12s" — library/recent row durations. */
fun formatDurationShort(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        minutes > 0 -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
        else -> "${seconds}s"
    }
}

/** "1:52:04", "48:12" — player scrubber position/total. */
fun formatMmSs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/** "84.2 MB", "1.2 GB" — storage figures throughout the design. */
fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    val mb = bytes / 1_048_576.0
    val kb = bytes / 1024.0
    return when {
        gb >= 1.0 -> "${roundTo(gb, 1)} GB"
        mb >= 1.0 -> "${roundTo(mb, 1)} MB"
        kb >= 1.0 -> "${roundTo(kb, 1)} KB"
        else -> "$bytes B"
    }
}

/** "1.2" to "GB" — split for the Home stat card, where the unit renders smaller (SCREENS.md #03). */
fun formatBytesParts(bytes: Long): Pair<String, String> {
    val gb = bytes / 1_073_741_824.0
    val mb = bytes / 1_048_576.0
    val kb = bytes / 1024.0
    return when {
        gb >= 1.0 -> roundTo(gb, 1) to "GB"
        mb >= 1.0 -> roundTo(mb, 1) to "MB"
        kb >= 1.0 -> roundTo(kb, 1) to "KB"
        else -> bytes.toString() to "B"
    }
}

/** "1.36" — the Storage screen's big usage figure, always in GB with two decimals. */
fun formatGbValue(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    return "%.2f".format(gb)
}

/** "64" — device total capacity, rounded to the nearest whole GB. */
fun formatGbWhole(bytes: Long): String = (bytes / 1_073_741_824.0).roundToInt().toString()

private fun roundTo(value: Double, decimals: Int): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    return ((value * factor).roundToInt() / factor).let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    }
}
