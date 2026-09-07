package com.quietstack.vaani.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.TranscriptionState
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val recordingsCount: Int = 0,
    val totalDurationMs: Long = 0,
    val totalSizeBytes: Long = 0,
    val activeJob: Recording? = null,
    val activeJobProgress: Float = 0f,
    val recent: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordingRepository.observeAll()
                .flatMapLatest { all ->
                    val activeRecording = all.firstOrNull { r ->
                        r.transcriptionState == TranscriptionState.RUNNING || r.transcriptionState == TranscriptionState.QUEUED
                    }
                    val jobProgress = activeRecording?.let { transcriptionRepository.observeJob(it.id) }
                        ?.map { job -> job?.progress ?: 0f }
                        ?: flowOf(0f)
                    jobProgress.map { progress -> Triple(all, activeRecording, progress) }
                }
                .collect { (all, activeRecording, progress) ->
                    _state.update {
                        it.copy(
                            recordingsCount = all.size,
                            totalDurationMs = all.sumOf { r -> r.durationMs },
                            totalSizeBytes = all.sumOf { r -> r.sizeBytes },
                            activeJob = activeRecording,
                            activeJobProgress = progress,
                            recent = all.take(3),
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
