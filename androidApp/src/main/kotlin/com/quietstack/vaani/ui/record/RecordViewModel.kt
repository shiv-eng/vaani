package com.quietstack.vaani.ui.record

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.recording.RecorderUiState
import com.quietstack.vaani.recording.RecordingForegroundService
import com.quietstack.vaani.shared.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordViewModel(
    private val context: Context,
    private val appSettings: AppSettings,
) : ViewModel() {

    private var service: RecordingForegroundService? = null
    private var isBound = false

    private val _recorderState = MutableStateFlow(RecorderUiState())
    val recorderState: StateFlow<RecorderUiState> = _recorderState.asStateFlow()

    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

    private val _language = MutableStateFlow(appSettings.transcriptionLanguage)
    val language: StateFlow<String?> = _language.asStateFlow()

    fun setLanguage(code: String?) {
        appSettings.transcriptionLanguage = code
        _language.value = code
    }

    private val _savedRecordingId = MutableStateFlow<String?>(null)
    val savedRecordingId: StateFlow<String?> = _savedRecordingId.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bound = (binder as RecordingForegroundService.LocalBinder).getService()
            service = bound
            isBound = true
            viewModelScope.launch { bound.state.collect { _recorderState.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    fun start() {
        val intent = Intent(context, RecordingForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun togglePause() {
        if (recorderState.value.isPaused) service?.resume() else service?.pause()
    }

    fun stop() {
        val current = service ?: return
        _isStopping.value = true
        viewModelScope.launch {
            // Also kicks off transcription automatically once the file is
            // finalized (see RecordingForegroundService) — Detail shows its
            // own progress card the moment this navigates there.
            val recordingId = current.stopRecording()
            unbind()
            _savedRecordingId.value = recordingId
        }
    }

    private fun unbind() {
        if (isBound) {
            runCatching { context.unbindService(connection) }
            isBound = false
        }
        service = null
    }

    override fun onCleared() {
        // Leaving the record screen (back navigation) stops and saves the
        // recording, same as before. Backgrounding the app without leaving
        // this screen does not clear the ViewModel, so the common case —
        // screen lock, a brief app switch — never reaches this path; the
        // foreground service keeps the recording alive either way.
        if (recorderState.value.isRecording) stop() else unbind()
    }
}
