package com.murmur.transcriber.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.murmur.transcriber.recording.DeleteRecordingUseCase
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.ui.components.ItemKind
import com.murmur.transcriber.ui.components.kind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    All("All"), Transcribed("Transcribed"), NotYet("Not yet"), Imported("Imported"),
}

// Filter mapping per the design spec (SCREENS.md #06): "Transcribed" also
// covers imported-but-not-yet-transcribed items — imported is itself treated
// as a completed step, distinct from a raw local recording.
private fun Recording.matches(filter: LibraryFilter): Boolean = when (filter) {
    LibraryFilter.All -> true
    LibraryFilter.Transcribed -> kind() == ItemKind.DONE || kind() == ItemKind.IMPORTED_RAW
    LibraryFilter.NotYet -> kind() == ItemKind.RAW || kind() == ItemKind.RUNNING
    LibraryFilter.Imported -> kind() == ItemKind.IMPORTED_RAW
}

data class LibraryState(
    val filter: LibraryFilter = LibraryFilter.All,
    val shown: List<Recording> = emptyList(),
    val shownCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val isLoading: Boolean = true,
)

sealed interface LibraryIntent {
    data class SetFilter(val filter: LibraryFilter) : LibraryIntent
    data class Rename(val id: String, val newName: String) : LibraryIntent
    data class Delete(val id: String) : LibraryIntent
}

class LibraryViewModel(
    private val recordingRepository: RecordingRepository,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
) : ViewModel() {

    private var allRecordings: List<Recording> = emptyList()
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordingRepository.observeAll().collect { recordings ->
                allRecordings = recordings
                applyFilter(_state.value.filter)
            }
        }
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SetFilter -> applyFilter(intent.filter)
            is LibraryIntent.Rename -> viewModelScope.launch {
                recordingRepository.rename(intent.id, intent.newName)
            }
            is LibraryIntent.Delete -> viewModelScope.launch {
                deleteRecordingUseCase(intent.id)
            }
        }
    }

    private fun applyFilter(filter: LibraryFilter) {
        val shown = allRecordings.filter { it.matches(filter) }
        _state.update {
            it.copy(
                filter = filter,
                shown = shown,
                shownCount = shown.size,
                totalSizeBytes = allRecordings.sumOf { r -> r.sizeBytes },
                isLoading = false,
            )
        }
    }
}
