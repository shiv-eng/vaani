package com.murmur.transcriber.shared.di

import com.murmur.transcriber.shared.data.AppSettings
import com.murmur.transcriber.shared.data.db.DatabaseDriverFactory
import com.murmur.transcriber.shared.data.repository.RecordingRepositoryImpl
import com.murmur.transcriber.shared.data.repository.TranscriptionRepositoryImpl
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.pipeline.AudioDecoder
import com.murmur.transcriber.shared.domain.pipeline.ModelDownloader
import com.murmur.transcriber.shared.domain.pipeline.createAudioDecoder
import com.murmur.transcriber.shared.domain.pipeline.createModelDownloader
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import com.murmur.transcriber.shared.domain.usecase.FinalizeRecordingUseCase
import com.murmur.transcriber.shared.domain.usecase.ImportRecordingUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single { MurmurDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<RecordingRepository> { RecordingRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get()) }
    single<AudioDecoder> { createAudioDecoder() }
    single {
        HttpClient {
            // A model download stalling silently (no data, no error — seen on a
            // flaky connection) must fail loudly rather than hang forever with
            // no way for the user to retry.
            install(HttpTimeout) {
                requestTimeoutMillis = 5 * 60_000L
                socketTimeoutMillis = 20_000L
                connectTimeoutMillis = 15_000L
            }
        }
    }
    single<ModelDownloader> { createModelDownloader(get()) }
    single { AppSettings(get()) }

    factory { ImportRecordingUseCase(get()) }
    factory { FinalizeRecordingUseCase(get()) }
    // StartTranscriptionUseCase and ModelRepository are registered per-platform
    // (see platformModule()) since resolving an on-disk path needs a platform Context.
}

/** Supplies the platform-specific [DatabaseDriverFactory] binding. */
expect fun platformModule(): Module
