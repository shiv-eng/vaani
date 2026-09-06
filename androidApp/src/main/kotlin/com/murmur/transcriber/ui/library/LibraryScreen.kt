package com.murmur.transcriber.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murmur.transcriber.shared.domain.model.Recording
import org.koin.androidx.compose.koinViewModel

// TODO(Milestone 3): rebuild to match the design's Library screen (filter
// chips, waveform previews, state badges) — this is still the scaffold-era
// placeholder, just with a real import entry point wired in.
@Composable
fun LibraryScreen(onImportClick: () -> Unit, viewModel: LibraryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Import a file", modifier = Modifier.clickable(onClick = onImportClick))
        }
        when {
            state.isLoading -> Text("Loading library…", modifier = Modifier.padding(16.dp))
            state.recordings.isEmpty() -> Text(
                "Nothing recorded yet. Tap record to start, or import a file.",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.recordings, key = { it.id }) { recording ->
                    RecordingRow(recording)
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(recording: Recording) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(recording.displayName)
        Text("${recording.durationMs / 1000}s · ${recording.transcriptionState}")
    }
}
