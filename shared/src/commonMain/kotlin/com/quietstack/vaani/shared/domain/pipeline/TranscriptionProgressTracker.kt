package com.quietstack.vaani.shared.domain.pipeline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Live, in-memory fine-grained progress (0f..1f) for whichever transcription
 * job is currently running, keyed by recordingId. Distinct from
 * TranscriptionJob's DB-persisted windowsDone/windowsTotal, which only
 * advances on whole-window completion — for a single-window recording
 * (WindowPlanner defaults to 30s, so most recordings are one window) that
 * reads as "stuck at 0%, then a sudden jump" (a real-device finding). A
 * Koin singleton so both the foreground service (producer) and whichever
 * screen is showing the recording (consumer) share the same instance.
 */
class TranscriptionProgressTracker {
    private val _progress = MutableStateFlow<Pair<String, Float>?>(null)
    val progress: StateFlow<Pair<String, Float>?> = _progress

    fun update(recordingId: String, value: Float) {
        _progress.value = recordingId to value
    }

    fun clear(recordingId: String) {
        if (_progress.value?.first == recordingId) _progress.value = null
    }
}
