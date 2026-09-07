package com.quietstack.vaani.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.format.formatMmSs
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditScreen(
    recordingId: String,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditViewModel = koinViewModel(parameters = { parametersOf(recordingId) }),
) {
    val state by viewModel.state.collectAsState()
    val colors = VaaniTheme.colors

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).mrise()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Cancel",
                color = colors.ink2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onCancel),
            )
            Text("Editing transcript", style = MaterialTheme.typography.titleSmall, color = colors.ink, fontSize = 14.5.sp)
            Text(
                "Save",
                color = colors.pri,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = viewModel::save),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))

        if (state.isLoading) return@Column

        LazyColumn(contentPadding = PaddingValues(20.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surf2, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        "Your changes are saved alongside what it originally said, not over it — you can always see the original too.",
                        color = colors.ink2,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            itemsIndexed(state.segments, key = { _, es -> es.segment.id }) { index, es ->
                EditSegmentRow(
                    state = es,
                    onTextChange = { text -> viewModel.updateDraft(es.segment.id, text) },
                    modifier = Modifier.mslide(index * 40).padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun EditSegmentRow(state: EditSegmentState, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMmSs(state.segment.startMs), color = colors.pri, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            Text(
                if (state.isEdited) "EDITED" else "ORIGINAL",
                color = if (state.isEdited) colors.pri else colors.ink3,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (state.isEdited) colors.surf2 else colors.surf, RoundedCornerShape(16.dp))
                .border(1.dp, if (state.isEdited) colors.pri else colors.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = state.draftText,
                onValueChange = onTextChange,
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 22.4.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.pri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isEdited) {
            Spacer(Modifier.height(4.dp))
            Text(
                state.segment.text.trim(),
                color = colors.ink3,
                fontSize = 11.5.sp,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
