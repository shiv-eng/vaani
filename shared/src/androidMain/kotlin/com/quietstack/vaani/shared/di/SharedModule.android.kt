package com.quietstack.vaani.shared.di

import android.content.Context
import com.quietstack.vaani.shared.data.db.DatabaseDriverFactory
import com.quietstack.vaani.shared.data.repository.ModelRepositoryImpl
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.shared.domain.usecase.StartTranscriptionUseCase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }

    single<Settings> {
        SharedPreferencesSettings(androidContext().getSharedPreferences("vaani_settings", Context.MODE_PRIVATE))
    }

    single<ModelRepository> {
        ModelRepositoryImpl(
            database = get(),
            modelDownloader = get(),
            resolveModelPath = { fileName -> File(androidContext().filesDir, "models/$fileName").absolutePath },
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
