package com.quietstack.vaani.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditSegmentState(val segment: Segment, val draftText: String) {
    val isEdited: Boolean get() = draftText != segment.text
}

data class EditState(
    val segments: List<EditSegmentState> = emptyList(),
    val isLoading: Boolean = true,
    val saved: Boolean = false,
)

class EditViewModel(
    private val recordingId: String,
    private val transcriptionRepository: TranscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val segments: List<Segment> = transcriptionRepository.observeSegments(recordingId).first()
            _state.update {
                it.copy(
                    segments = segments.map { s -> EditSegmentState(s, s.displayText) },
                    isLoading = false,
                )
            }
        }
    }

    fun updateDraft(segmentId: String, text: String) {
        _state.update { st ->
            st.copy(segments = st.segments.map { es -> if (es.segment.id == segmentId) es.copy(draftText = text) else es })
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.value.segments.forEach { es ->
                if (es.isEdited) transcriptionRepository.editSegment(es.segment.id, es.draftText)
            }
            _state.update { it.copy(saved = true) }
        }
    }
}
