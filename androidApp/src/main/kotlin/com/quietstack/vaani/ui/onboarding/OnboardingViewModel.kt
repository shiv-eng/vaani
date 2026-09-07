package com.quietstack.vaani.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietstack.vaani.shared.data.AppSettings
import com.quietstack.vaani.shared.data.ModelCatalogue
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, DOWNLOAD }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val progress: Float = 0f,
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val waitForWifi: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
)

class OnboardingViewModel(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // Guards against re-downloading if onboarding was interrupted after the
        // model landed but before the completion flag was persisted.
        viewModelScope.launch {
            val alreadyDownloaded = modelRepository.observeDownloaded().first().any { it.id == ModelCatalogue.DEFAULT.id }
            if (alreadyDownloaded) {
                appSettings.hasCompletedOnboarding = true
                _state.update { it.copy(isComplete = true) }
            }
        }
    }

    fun onSetUpClicked() {
        _state.update { it.copy(step = OnboardingStep.DOWNLOAD) }
        startDownload()
    }

    fun retry() = startDownload()

    private fun startDownload() {
        downloadJob?.cancel()
        _state.update { it.copy(isDownloading = true, isPaused = false, error = null) }
        downloadJob = viewModelScope.launch {
            try {
                modelRepository.download(ModelCatalogue.DEFAULT.id).collect { p ->
                    _state.update { it.copy(progress = p) }
                }
                appSettings.hasCompletedOnboarding = true
                _state.update { it.copy(isDownloading = false, isComplete = true) }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                _state.update { it.copy(isDownloading = false, error = t.message ?: "Download failed") }
            }
        }
    }

    fun togglePause() {
        if (_state.value.isPaused) {
            startDownload()
        } else {
            downloadJob?.cancel()
            _state.update { it.copy(isDownloading = false, isPaused = true) }
        }
    }

    fun setWaitForWifi(enabled: Boolean) {
        _state.update { it.copy(waitForWifi = enabled) }
    }

    /** Defers the rest of the download to a constrained background worker and lets the user in now. */
    fun skipAhead() {
        downloadJob?.cancel()
        ModelDownloadWorker.enqueue(context, ModelCatalogue.DEFAULT.id, requireUnmetered = _state.value.waitForWifi)
        appSettings.hasCompletedOnboarding = true
        _state.update { it.copy(isDownloading = false, isComplete = true) }
    }
}
