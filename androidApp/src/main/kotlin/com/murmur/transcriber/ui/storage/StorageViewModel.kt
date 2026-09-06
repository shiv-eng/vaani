package com.murmur.transcriber.ui.storage

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.murmur.transcriber.shared.domain.model.TranscriptionModel
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageState(
    val recordingsBytes: Long = 0,
    val modelsBytes: Long = 0,
    val cacheBytes: Long = 0,
    val deviceTotalBytes: Long = 1,
    val models: List<TranscriptionModel> = emptyList(),
    val isLoading: Boolean = true,
) {
    val murmurTotalBytes: Long get() = recordingsBytes + modelsBytes + cacheBytes
    val recordingsFraction: Float get() = recordingsBytes.toFloat() / deviceTotalBytes
    val modelsFraction: Float get() = modelsBytes.toFloat() / deviceTotalBytes
    val cacheFraction: Float get() = cacheBytes.toFloat() / deviceTotalBytes
}

class StorageViewModel(
    private val context: Context,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val recordingsDir = File(context.filesDir, "recordings")
    private val modelsDir = File(context.filesDir, "models")

    private val _sizes = MutableStateFlow(Triple(0L, 0L, 0L))
    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    init {
        refreshSizes()
        viewModelScope.launch {
            combine(_sizes, modelRepository.observeDownloaded()) { sizes, models ->
                StorageState(
                    recordingsBytes = sizes.first,
                    modelsBytes = sizes.second,
                    cacheBytes = sizes.third,
                    deviceTotalBytes = StatFs(context.filesDir.path).totalBytes.coerceAtLeast(1),
                    models = models,
                    isLoading = false,
                )
            }.collect { _state.value = it }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            refreshSizes()
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.delete(modelId)
            refreshSizes()
        }
    }

    private fun refreshSizes() {
        viewModelScope.launch {
            val sizes = withContext(Dispatchers.Default) {
                Triple(dirSize(recordingsDir), dirSize(modelsDir), dirSize(context.cacheDir))
            }
            _sizes.update { sizes }
        }
    }

    private fun dirSize(dir: File): Long =
        if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
}
