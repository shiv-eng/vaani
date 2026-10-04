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

enum class OnboardingStep { WELCOME, DOWNLOAD, WALKTHROUGH }

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
        // models landed but before the completion flag was persisted. Still
        // routes through the walkthrough rather than straight to Home — every
        // first-time user sees it once, even one whose download happened to
        // finish right as the app was killed.
        viewModelScope.launch {
            val downloadedIds = modelRepository.observeDownloaded().first().map { it.id }.toSet()
            val allRequiredPresent = ModelCatalogue.REQUIRED.all { it.id in downloadedIds }
            if (allRequiredPresent) {
                _state.update { it.copy(step = OnboardingStep.WALKTHROUGH, isDownloading = false) }
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
                val alreadyDownloaded = modelRepository.observeDownloaded().first().map { it.id }.toSet()
                val pending = ModelCatalogue.REQUIRED.filter { it.id !in alreadyDownloaded }
                val totalBytes = ModelCatalogue.REQUIRED.sumOf { it.sizeBytes }
                val doneBytes = ModelCatalogue.REQUIRED.filter { it.id in alreadyDownloaded }.sumOf { it.sizeBytes }

                var bytesBeforeCurrent = doneBytes
                for (entry in pending) {
                    modelRepository.download(entry.id).collect { p ->
                        val overall = (bytesBeforeCurrent + entry.sizeBytes * p) / totalBytes.toFloat()
                        _state.update { it.copy(progress = overall) }
                    }
                    bytesBeforeCurrent += entry.sizeBytes
                }
                _state.update { it.copy(isDownloading = false, step = OnboardingStep.WALKTHROUGH, progress = 1f) }
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
        ModelDownloadWorker.enqueue(context, ModelCatalogue.REQUIRED.map { it.id }, requireUnmetered = _state.value.waitForWifi)
        _state.update { it.copy(isDownloading = false, step = OnboardingStep.WALKTHROUGH) }
    }

    /** The walkthrough is the real end of onboarding — this is the only place hasCompletedOnboarding gets set. */
    fun onWalkthroughFinished() {
        appSettings.hasCompletedOnboarding = true
        _state.update { it.copy(isComplete = true) }
    }
}
