package com.murmur.transcriber.shared.di

import com.murmur.transcriber.shared.data.db.DatabaseDriverFactory
import com.murmur.transcriber.shared.data.repository.ModelRepositoryImpl
import com.murmur.transcriber.shared.data.repository.RecordingRepositoryImpl
import com.murmur.transcriber.shared.data.repository.TranscriptionRepositoryImpl
import com.murmur.transcriber.shared.db.MurmurDatabase
import com.murmur.transcriber.shared.domain.pipeline.AudioDecoder
import com.murmur.transcriber.shared.domain.pipeline.createAudioDecoder
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.repository.TranscriptionRepository
import com.murmur.transcriber.shared.domain.usecase.ImportRecordingUseCase
import com.murmur.transcriber.shared.domain.usecase.StartTranscriptionUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single { MurmurDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<RecordingRepository> { RecordingRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get()) }
    single<ModelRepository> { ModelRepositoryImpl(get()) }
    single<AudioDecoder> { createAudioDecoder() }

    factory { ImportRecordingUseCase(get()) }
    factory { StartTranscriptionUseCase(get(), get(), get(), get()) }
}

/** Supplies the platform-specific [DatabaseDriverFactory] binding. */
expect fun platformModule(): Module
