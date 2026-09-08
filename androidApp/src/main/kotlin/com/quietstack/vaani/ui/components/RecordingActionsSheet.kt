package com.quietstack.vaani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.theme.VaaniTheme

/**
 * Content for the shared "Item actions" sheet (SCREENS.md #13) — raised by a
 * library row long-press or the detail screen's overflow, so both entry
 * points must render identically rather than drifting into two bespoke menus.
 */
@Composable
fun RecordingActionsSheetContent(
    recording: Recording,
    onRename: () -> Unit,
    onEditTranscript: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = VaaniTheme.colors
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Recording", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Text(recording.displayName, color = colors.ink3, fontSize = 12.5.sp, maxLines = 1)
        Spacer(Modifier.height(10.dp))

        ActionRow("Rename", "Inline, display name only", onClick = onRename)
        ActionRow("Edit transcript", "Original kept alongside", onClick = onEditTranscript)
        ActionRow("Export or share", "Text, clipboard, share sheet", onClick = onExport)
        ActionRow(
            "Delete",
            "Audio, transcript and rows together",
            danger = true,
            tag = "FREES ${formatBytes(recording.sizeBytes)}",
            onClick = onDelete,
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close", color = colors.ink2) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionRow(label: String, meta: String, danger: Boolean = false, tag: String? = null, onClick: () -> Unit) {
    val colors = VaaniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (danger) colors.acc else colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(meta, color = colors.ink3, fontSize = 11.5.sp)
        }
        if (tag != null) {
            Box(modifier = Modifier.background(colors.surf3, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                Text(tag, color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
fun RenameDialog(recording: Recording, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(recording.id) { mutableStateOf(recording.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Display name only. On disk the file keeps its id, so renaming can never break a reference.",
                    fontSize = 11.5.sp,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save name") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DeleteRecordingDialog(recording: Recording, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this recording?") },
        text = { Text("The audio file, the transcript and every database row about it go together, for real. This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
