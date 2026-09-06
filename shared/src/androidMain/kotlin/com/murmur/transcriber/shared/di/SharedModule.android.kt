package com.murmur.transcriber.shared.di

import com.murmur.transcriber.shared.data.db.DatabaseDriverFactory
import com.murmur.transcriber.shared.domain.usecase.StartTranscriptionUseCase
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }

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
