package com.quietstack.vaani.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.ui.components.DeleteRecordingDialog
import com.quietstack.vaani.ui.components.KindBadge
import com.quietstack.vaani.ui.components.RecordingActionsSheetContent
import com.quietstack.vaani.ui.components.RenameDialog
import com.quietstack.vaani.ui.components.Waveform
import com.quietstack.vaani.ui.components.kind
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.format.formatDurationShort
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onImportClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onOpenRecording: (String) -> Unit = {},
    onEditRecording: (String) -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = VaaniTheme.colors
    var actionsFor by remember { mutableStateOf<Recording?>(null) }
    var renaming by remember { mutableStateOf<Recording?>(null) }
    var deleting by remember { mutableStateOf<Recording?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).mrise()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surf, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable(onClick = onImportClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Import a file", tint = colors.ink2, modifier = Modifier.size(19.dp))
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surf, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = colors.ink2, modifier = Modifier.size(17.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilter.entries.forEach { filter ->
                FilterChip(
                    label = filter.label,
                    selected = filter == state.filter,
                    onClick = { viewModel.onIntent(LibraryIntent.SetFilter(filter)) },
                )
            }
        }

        Text(
            "${state.shownCount} items · ${formatBytes(state.totalSizeBytes)}",
            color = colors.ink3,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 10.dp),
        )

        when {
            state.isLoading -> {}
            state.shown.isEmpty() -> Text(
                "Nothing here yet. Tap record to start, or import a file.",
                color = colors.ink3,
                modifier = Modifier.padding(20.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                itemsIndexed(state.shown, key = { _, r -> r.id }) { index, recording ->
                    LibraryItemCard(
                        recording = recording,
                        onClick = { onOpenRecording(recording.id) },
                        onLongClick = { actionsFor = recording },
                        modifier = Modifier.mslide(index * 40).padding(bottom = 10.dp),
                    )
                }
            }
        }
    }

    actionsFor?.let { recording ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, sheetState = sheetState) {
            RecordingActionsSheetContent(
                recording = recording,
                onRename = { actionsFor = null; renaming = recording },
                onEditTranscript = { actionsFor = null; onEditRecording(recording.id) },
                onExport = { actionsFor = null; onOpenRecording(recording.id) },
                onDelete = { actionsFor = null; deleting = recording },
                onClose = { actionsFor = null },
            )
        }
    }

    renaming?.let { recording ->
        RenameDialog(
            recording = recording,
            onDismiss = { renaming = null },
            onConfirm = { name -> viewModel.onIntent(LibraryIntent.Rename(recording.id, name)); renaming = null },
        )
    }

    deleting?.let { recording ->
        DeleteRecordingDialog(
            recording = recording,
            onDismiss = { deleting = null },
            onConfirm = { viewModel.onIntent(LibraryIntent.Delete(recording.id)); deleting = null },
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = VaaniTheme.colors
    val bg = if (selected) colors.pri else Color.Transparent
    val fg = if (selected) colors.priOn else colors.ink2
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) bg else colors.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryItemCard(
    recording: Recording,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaaniTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(22.dp))
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recording.displayName, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatDurationShort(recording.durationMs)} · ${formatBytes(recording.sizeBytes)}",
                    color = colors.ink3,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.5.sp,
                )
            }
            KindBadge(recording.kind())
        }
        Spacer(Modifier.height(13.dp))
        Waveform(seed = recording.id.hashCode(), barCount = 46, modifier = Modifier.fillMaxWidth().height(22.dp))
    }
}
