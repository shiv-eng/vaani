package com.murmur.transcriber.ui.storage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import com.murmur.transcriber.ui.components.mrise
import com.murmur.transcriber.ui.components.mslide
import com.murmur.transcriber.ui.format.formatBytes
import com.murmur.transcriber.ui.format.formatGbValue
import com.murmur.transcriber.ui.format.formatGbWhole
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.MurmurTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun StorageScreen(viewModel: StorageViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MurmurTheme.colors
    var deleting by remember { mutableStateOf<TranscriptionModel?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 20.dp).mrise(),
        contentPadding = PaddingValues(top = 22.dp, bottom = 40.dp),
    ) {
        item {
            Text("Storage", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Everything sits in app-private storage. Uninstalling removes all of it.",
                color = colors.ink2,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(20.dp))

            if (!state.isLoading) {
                UsageCard(state, modifier = Modifier.mslide(40))
                Spacer(Modifier.height(24.dp))
                CacheCard(cacheBytes = state.cacheBytes, onClear = viewModel::clearCache, modifier = Modifier.mslide(90))
                Spacer(Modifier.height(24.dp))
                Text("Models", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                Spacer(Modifier.height(10.dp))
            }
        }

        if (!state.isLoading) {
            if (state.models.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surf, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                            .mslide(130),
                    ) {
                        Text("No models downloaded yet", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The first-run download flow lands in Milestone 7 — this list fills in once it exists.",
                            color = colors.ink3,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            } else {
                state.models.forEachIndexed { index, model ->
                    item(key = model.id) {
                        ModelRow(
                            model = model,
                            onDelete = { deleting = model },
                            modifier = Modifier.mslide(index * 40).padding(bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }

    deleting?.let { model ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this model?") },
            text = { Text("The speech model will be removed. You can download it again later.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteModel(model.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun UsageCard(state: StorageState, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatGbValue(state.murmurTotalBytes), color = colors.ink, fontFamily = JetBrainsMonoFamily, fontSize = 29.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "GB of ${formatGbWhole(state.deviceTotalBytes)} GB used by Murmur",
                color = colors.ink3,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(12.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val remainder = (1f - state.recordingsFraction - state.modelsFraction - state.cacheFraction).coerceAtLeast(0.001f)
            UsageSegment(colors.pri, Modifier.weight(state.recordingsFraction.coerceAtLeast(0.001f)))
            UsageSegment(colors.acc, Modifier.weight(state.modelsFraction.coerceAtLeast(0.001f)))
            UsageSegment(colors.ok, Modifier.weight(state.cacheFraction.coerceAtLeast(0.001f)))
            UsageSegment(colors.surf3, Modifier.weight(remainder))
        }
        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendRow(colors.pri, "Recordings", formatBytes(state.recordingsBytes))
            LegendRow(colors.acc, "Models", formatBytes(state.modelsBytes))
            LegendRow(colors.ok, "Cache", formatBytes(state.cacheBytes))
        }
    }
}

@Composable
private fun UsageSegment(color: Color, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().background(color, RoundedCornerShape(6.dp)))
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    val colors = MurmurTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text("$label $value", color = colors.ink2, fontSize = 11.5.sp)
    }
}

@Composable
private fun CacheCard(cacheBytes: Long, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text("Cache", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Temporary files made while transcribing. Safe to clear anytime — they're recreated automatically if needed.",
            color = colors.ink3,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Clear ${formatBytes(cacheBytes)}",
            color = colors.pri,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            modifier = Modifier.clickable(onClick = onClear),
        )
    }
}

@Composable
private fun ModelRow(model: TranscriptionModel, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Speech model · Hindi & English", color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text("Delete", color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, modifier = Modifier.clickable(onClick = onDelete))
        }
        Spacer(Modifier.height(4.dp))
        Text(formatBytes(model.sizeBytes), color = colors.ink3, fontFamily = JetBrainsMonoFamily, fontSize = 11.5.sp)
    }
}
