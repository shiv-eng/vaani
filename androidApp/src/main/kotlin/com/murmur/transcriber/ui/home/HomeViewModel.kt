package com.murmur.transcriber.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.murmur.transcriber.shared.domain.model.Recording
import com.murmur.transcriber.shared.domain.model.TranscriptionState
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val recordingsCount: Int = 0,
    val totalDurationMs: Long = 0,
    val totalSizeBytes: Long = 0,
    val activeJob: Recording? = null,
    val recent: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val recordingRepository: RecordingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordingRepository.observeAll().collect { all ->
                _state.update {
                    it.copy(
                        recordingsCount = all.size,
                        totalDurationMs = all.sumOf { r -> r.durationMs },
                        totalSizeBytes = all.sumOf { r -> r.sizeBytes },
                        activeJob = all.firstOrNull { r ->
                            r.transcriptionState == TranscriptionState.RUNNING ||
                                r.transcriptionState == TranscriptionState.QUEUED
                        },
                        recent = all.take(3),
                        isLoading = false,
                    )
                }
            }
        }
    }
}
