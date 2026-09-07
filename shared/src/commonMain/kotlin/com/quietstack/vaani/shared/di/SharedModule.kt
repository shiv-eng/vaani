package com.quietstack.vaani.shared.di

import com.quietstack.vaani.shared.data.AppSettings
import com.quietstack.vaani.shared.data.db.DatabaseDriverFactory
import com.quietstack.vaani.shared.data.repository.BookmarkRepositoryImpl
import com.quietstack.vaani.shared.data.repository.RecordingRepositoryImpl
import com.quietstack.vaani.shared.data.repository.TranscriptionRepositoryImpl
import com.quietstack.vaani.shared.db.VaaniDatabase
import com.quietstack.vaani.shared.domain.pipeline.AudioDecoder
import com.quietstack.vaani.shared.domain.pipeline.ModelDownloader
import com.quietstack.vaani.shared.domain.pipeline.createAudioDecoder
import com.quietstack.vaani.shared.domain.pipeline.createModelDownloader
import com.quietstack.vaani.shared.domain.pipeline.TranscriptionProgressTracker
import com.quietstack.vaani.shared.domain.repository.BookmarkRepository
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import com.quietstack.vaani.shared.domain.usecase.BeginRecordingUseCase
import com.quietstack.vaani.shared.domain.usecase.FinalizeRecordingUseCase
import com.quietstack.vaani.shared.domain.usecase.ImportRecordingUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single { VaaniDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<RecordingRepository> { RecordingRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get()) }
    single<BookmarkRepository> { BookmarkRepositoryImpl(get()) }
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
    single { TranscriptionProgressTracker() }

    factory { ImportRecordingUseCase(get()) }
    factory { BeginRecordingUseCase(get()) }
    factory { FinalizeRecordingUseCase(get()) }
    // StartTranscriptionUseCase and ModelRepository are registered per-platform
    // (see platformModule()) since resolving an on-disk path needs a platform Context.
}

/** Supplies the platform-specific [DatabaseDriverFactory] binding. */
expect fun platformModule(): Module
