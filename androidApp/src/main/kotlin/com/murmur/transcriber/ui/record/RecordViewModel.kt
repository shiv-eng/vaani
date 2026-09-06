package com.murmur.transcriber.ui.record

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.murmur.transcriber.recording.AudioRecorderController
import com.murmur.transcriber.recording.RecorderUiState
import com.murmur.transcriber.shared.domain.usecase.FinalizeRecordingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordViewModel(
    context: Context,
    private val finalizeRecordingUseCase: FinalizeRecordingUseCase,
) : ViewModel() {

    private val controller = AudioRecorderController(context.applicationContext, viewModelScope)
    val recorderState: StateFlow<RecorderUiState> = controller.state

    private val _savedRecordingId = MutableStateFlow<String?>(null)
    val savedRecordingId: StateFlow<String?> = _savedRecordingId.asStateFlow()

    fun start() = controller.start()

    fun togglePause() {
        if (recorderState.value.isPaused) controller.resume() else controller.pause()
    }

    fun stop() {
        val finished = controller.stop()
        viewModelScope.launch {
            val recording = finalizeRecordingUseCase(
                filePath = finished.filePath,
                durationMs = finished.durationMs,
                sizeBytes = finished.sizeBytes,
                mimeType = finished.mimeType,
                sampleRate = finished.sampleRate,
            )
            _savedRecordingId.value = recording.id
        }
    }

    override fun onCleared() {
        // If the ViewModel is torn down mid-recording (e.g. process death candidate),
        // stopping here still lands the file — see the foreground-service follow-up
        // in Milestone 6 for surviving actual process death during a long recording.
        if (recorderState.value.isRecording) stop()
    }
}
