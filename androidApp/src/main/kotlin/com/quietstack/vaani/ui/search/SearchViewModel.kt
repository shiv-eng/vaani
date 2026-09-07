package com.quietstack.vaani.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<Recording> = emptyList(),
    val isSearching: Boolean = false,
)

/**
 * Matches on title (instantly, in memory) and transcript content (a debounced
 * DB query — see TranscriptionRepository.searchRecordingIds) so "search" finds
 * what was actually said, not just what a recording is named.
 */
class SearchViewModel(
    private val recordingRepository: RecordingRepository,
    private val transcriptionRepository: TranscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            _state.update { it.copy(isSearching = true) }
            val all = recordingRepository.observeAll().first()
            val contentMatchIds = transcriptionRepository.searchRecordingIds(query).toSet()
            val matches = all.filter {
                it.displayName.contains(query, ignoreCase = true) || it.id in contentMatchIds
            }
            _state.update { it.copy(results = matches, isSearching = false) }
        }
    }
}
