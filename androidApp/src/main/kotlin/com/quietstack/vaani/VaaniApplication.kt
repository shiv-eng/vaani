package com.quietstack.vaani

import android.app.Application
import com.quietstack.vaani.di.appModule
import com.quietstack.vaani.recording.CleanupOrphanedRecordingsUseCase
import com.quietstack.vaani.shared.di.platformModule
import com.quietstack.vaani.shared.di.sharedModule
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.whisper.TranscriptionEngine
import com.quietstack.vaani.whisper.di.whisperModule
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VaaniApplication : Application() {

    private val modelRepository: ModelRepository by inject()
    private val transcriptionEngine: TranscriptionEngine by inject()
    private val cleanupOrphanedRecordingsUseCase: CleanupOrphanedRecordingsUseCase by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@VaaniApplication)
            modules(appModule, sharedModule, platformModule(), whisperModule)
        }

        // A recording row (and its job) is created the instant recording
        // starts, before there's a finished file, so live transcription has
        // a real id to attach segments to (see BeginRecordingUseCase). If the
        // process dies mid-recording (force-stop, OS kill, crash) that row
        // and job are left stuck — clean them up once per process start,
        // before anything else touches the library.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            cleanupOrphanedRecordingsUseCase()
        }

        // Model load is the dominant cost in a transcription job (see
        // AndroidTranscriptionEngine doc) — more so for a larger model. Warm
        // the engine here, at process start (the earliest point available,
        // before any UI exists), so it's usually already loaded by the time
        // the user finishes recording and taps Transcribe, instead of paying
        // that cost after they've asked for a result.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val model = modelRepository.observeDownloaded().first().firstOrNull() ?: return@launch
            val modelPath = File(filesDir, "models/${model.fileName}").absolutePath
            transcriptionEngine.loadModel(modelPath)
        }
    }
}
