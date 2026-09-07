package com.quietstack.vaani.ui.importing

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.importing.ImportController
import com.quietstack.vaani.importing.ImportedFileInfo
import com.quietstack.vaani.shared.domain.usecase.ImportRecordingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportState(
    val isLoading: Boolean = true,
    val info: ImportedFileInfo? = null,
    val error: String? = null,
    val savedRecordingId: String? = null,
)

class ImportViewModel(
    private val context: Context,
    private val importRecordingUseCase: ImportRecordingUseCase,
) : ViewModel() {

    private val controller = ImportController(context.applicationContext)

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun stage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val info = controller.stageImport(uri)
                _state.update { it.copy(isLoading = false, info = info) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Could not read that file") }
            }
        }
    }

    fun commit() {
        val info = _state.value.info ?: return
        viewModelScope.launch {
            val recording = importRecordingUseCase(
                filePath = info.filePath,
                displayName = info.displayFileName,
                durationMs = info.durationMs,
                sizeBytes = info.sizeBytes,
                mimeType = info.mimeType,
                sampleRate = info.sampleRate,
            )
            _state.update { it.copy(savedRecordingId = recording.id) }
        }
    }
}
