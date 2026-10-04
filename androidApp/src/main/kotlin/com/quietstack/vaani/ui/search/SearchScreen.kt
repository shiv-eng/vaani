package com.quietstack.vaani.ui.search

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.ui.components.KindBadge
import com.quietstack.vaani.ui.components.Waveform
import com.quietstack.vaani.ui.components.kind
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.components.rememberAnimateOnce
import com.quietstack.vaani.ui.format.formatBytes
import com.quietstack.vaani.ui.format.formatDurationShort
import com.quietstack.vaani.ui.theme.JetBrainsMonoFamily
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onOpenRecording: (String) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = VaaniTheme.colors
    val animatedRows = remember { mutableSetOf<Any>() }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).mrise()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(colors.surf, RoundedCornerShape(16.dp))
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.ink3, modifier = Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (state.query.isEmpty()) {
                    Text("Search recordings and transcripts", color = colors.ink3, fontSize = 14.sp)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.pri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        when {
            state.query.isBlank() -> Text(
                "Type to find a recording by name or by what was said in it.",
                color = colors.ink3,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            state.isSearching -> {}
            state.results.isEmpty() -> Text(
                "No matches for “${state.query}”.",
                color = colors.ink3,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                itemsIndexed(state.results, key = { _, r -> r.id }) { index, recording ->
                    val animate = rememberAnimateOnce(recording.id, animatedRows)
                    SearchResultCard(
                        recording = recording,
                        onClick = { onOpenRecording(recording.id) },
                        modifier = Modifier.mslide(index * 40, animate = animate).padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(recording: Recording, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(22.dp))
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recording.displayName, color = colors.ink, maxLines = 1)
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
