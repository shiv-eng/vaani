package com.murmur.transcriber.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryState(
    val recordings: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface LibraryIntent {
    data class Rename(val id: String, val newName: String) : LibraryIntent
    data class Delete(val id: String) : LibraryIntent
}

class LibraryViewModel(
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordingRepository.observeAll().collect { recordings ->
                _state.update { it.copy(recordings = recordings, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.Rename -> viewModelScope.launch {
                recordingRepository.rename(intent.id, intent.newName)
            }
            is LibraryIntent.Delete -> viewModelScope.launch {
                recordingRepository.delete(intent.id)
            }
        }
    }
}
