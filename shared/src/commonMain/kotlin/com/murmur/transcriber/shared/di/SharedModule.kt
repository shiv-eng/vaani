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
import com.murmur.transcriber.shared.domain.usecase.FinalizeRecordingUseCase
import com.murmur.transcriber.shared.domain.usecase.ImportRecordingUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single { MurmurDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<RecordingRepository> { RecordingRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get()) }
    single<ModelRepository> { ModelRepositoryImpl(get()) }
    single<AudioDecoder> { createAudioDecoder() }

    factory { ImportRecordingUseCase(get()) }
    factory { FinalizeRecordingUseCase(get()) }
    // StartTranscriptionUseCase is registered per-platform (see platformModule()) since
    // resolving a recording's on-disk path needs a platform Context.
}

/** Supplies the platform-specific [DatabaseDriverFactory] binding. */
expect fun platformModule(): Module
