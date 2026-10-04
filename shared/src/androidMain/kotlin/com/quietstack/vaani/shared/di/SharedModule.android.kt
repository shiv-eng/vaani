package com.quietstack.vaani.shared.di

import android.content.Context
import com.quietstack.vaani.shared.data.ModelCatalogue
import com.quietstack.vaani.shared.data.db.DatabaseDriverFactory
import com.quietstack.vaani.shared.data.repository.ModelRepositoryImpl
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.shared.domain.usecase.StartTranscriptionUseCase
import com.quietstack.vaani.whisper.TranscriptionEngine
import com.quietstack.vaani.whisper.createAndroidRoutingTranscriptionEngine
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private fun Context.modelPath(fileName: String): String = File(filesDir, "models/$fileName").absolutePath

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }

    single<Settings> {
        SharedPreferencesSettings(androidContext().getSharedPreferences("vaani_settings", Context.MODE_PRIVATE))
    }

    single<ModelRepository> {
        ModelRepositoryImpl(
            database = get(),
            modelDownloader = get(),
            resolveModelPath = { fileName -> androidContext().modelPath(fileName) },
        )
    }

    // Overrides whisperModule's plain createTranscriptionEngine() binding —
    // this Koin version resolves same-type duplicates by last-module-wins,
    // not an explicit override flag, so platformModule() must be listed
    // AFTER whisperModule in VaaniApplication's modules(...) call for this
    // to actually win. Hindi gets routed to a dedicated model here (see
    // RoutingTranscriptionEngine); every other platform still gets plain
    // Whisper via whisperModule, since only Android has sherpa-onnx wired up.
    single<TranscriptionEngine> {
        createAndroidRoutingTranscriptionEngine(
            indicModelPath = androidContext().modelPath(ModelCatalogue.INDIC_CONFORMER_HI_MODEL.fileName),
            indicTokensPath = androidContext().modelPath(ModelCatalogue.INDIC_CONFORMER_HI_TOKENS.fileName),
        )
    }

    factory {
        StartTranscriptionUseCase(
            recordingRepository = get(),
            transcriptionRepository = get(),
            audioDecoder = get(),
            transcriptionEngine = get(),
            resolveAudioPath = { fileName -> File(androidContext().filesDir, "recordings/$fileName").absolutePath },
        )
    }
}
